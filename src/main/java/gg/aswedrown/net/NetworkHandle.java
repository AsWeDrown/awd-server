package gg.aswedrown.net;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import gg.aswedrown.server.udp.UdpServer;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@Getter @Setter
@RequiredArgsConstructor
public class NetworkHandle {

    /* = 32+1 - макс. число "недавних" пакетов, данные/сведения о
         которых мы храним (последний пакет плюс 32 пакета до него)  */
    private static final int PACKETS_QUEUE_SIZE = 33;

    private final Object lock = new Object();

    @NonNull
    private final UdpServer udpServer;

    @NonNull
    private final InetAddress addr;

    /**
     * 33 самых "новых" пакета, которые мы отправили другой стороне
     * (хранится в том числе и содержимое этих пакетов для возможности
     * повторной отправки, например, при потере).
     */
    private final Deque<PacketContainer> sentQueue = new ArrayDeque<>();

    /**
     * Числа sequence number вплоть до 33 пакетов, подтверждение об успешной
     * доставке которых мы "недавно" получили от другой стороны. Используется
     * для возможности делать предположения о потерянных пакетах.
     */
    private final Deque<Integer> deliveredQueue = new ArrayDeque<>();

    /**
     * Числа sequence number вплоть до 33 пакетов, которые мы недавно получили.
     * Используется для вычисления и отправке другой стороне ack bitfield.
     */
    private final Deque<Integer> receivedQueue = new ArrayDeque<>();

    /**
     * Статусы доставки вплоть до 33 пакетов, которые мы недавно отправили.
     * При получении подтверждения о доставке, в эту очередь добавляется
     * единица. При подозрении на потерю пакета, в неё добавляется ноль.
     * Используется для подсчёта packet loss %.
     */
    private final Deque<Integer> deliveryStats = new ArrayDeque<>();

    @Getter
    private float packetLossPercent;

    /**
     * "Протоколообразующие" поля.
     */
    private int localSequenceNumber, // новер самого "нового" отправленного пакета
                remoteSequenceNumber; // номер самого "нового" полученного пакета

    /* Этот метод НЕ гарантирует потокобезопасность. Его вызов должен быть обёрнут в блокирующий блок. */
    private int calculateAckBitfield() {
        int ackBitfield = 0;

        // Идём от 1, т.к. иначе мы включим в ack bitfield ещё и пакет с номером remote sequence
        // number - а это нам не нужно - о получении пакета с этим номером мы сообщим отправителю
        // напрямую, когда сами будем отправлять ему свой следующий пакет.
        for (int bitNum = 1; bitNum < PACKETS_QUEUE_SIZE; bitNum++)
            if (receivedQueue.contains(SequenceNumberMath
                    .subtract(remoteSequenceNumber, bitNum))) // в порядке уменьшения номера
                // "Оповещение" о том, что мы успешно получили пакет с этим
                // sequence number (устанавливаем бит с соотв. номером на единицу).
                ackBitfield |= 1 << (bitNum - 1); // т.к. идём с единицы, не забываем отнимать эту единицу здесь

        System.out.println("** TEMP DEBUG ** Calculated ack bitfield: "
                + Integer.toString(ackBitfield, 2));

        return ackBitfield;
    }

    /* Этот метод гарантирует потокобезопасность. Его вызов должен НЕ быть обёрнут в блокирующий блок. */
    void packetReceived(@NonNull UnwrappedPacketData data) {
        synchronized (lock) {
            // Обновляем удалённый sequence number и добавляем sequence number этого пакета
            // в список недавно полученных (для дальнейшего "оповещения" о получении).
            int sequence = data.getSequence();

            if (SequenceNumberMath.isMoreRecent(sequence, remoteSequenceNumber))
                // Полученный только что пакет оказался "новее" пакета,
                // sequence number которого у нас сейчас сохранён. Обновляем.
                remoteSequenceNumber = sequence;

            // Смотрим, подтверждение получения каких пакетов другая сторона указала.
            int ack = data.getAck(); // sequence number последнего пакета, который другая сторона от нас получила
            int ackBitfield = data.getAckBitfield(); // сведения о получении 32 пакетов до пакета с номером ack
            int bitNum = 1; // начинаем с 1, т.к. 0 - это этот с номером ack; нас интересуют те, что были до него

            System.out.println("** TEMP DEBUG ** Packet received: #" + sequence
                    + " (current remote seq: #" + remoteSequenceNumber + "), ack: " + ack
                    + ", ack bitfield: " + Integer.toString(ackBitfield, 2));

            while (ackBitfield > 0) {
                boolean bitSet = (ackBitfield & 1) == 1;

                if (bitSet) {
                    // Другая сторона подтвердила получение от нас пакета с номером (ack - bitNum).
                    int ackedPacketSeqNum = SequenceNumberMath.subtract(ack, bitNum);
                    packetDelivered(ackedPacketSeqNum);
                }

                // Переходим к следующему биту.
                ackBitfield >>= 1;
                bitNum++;
            }

            // Проверяем подтверждение другой стороной получения от нас отправленных ранее пакетов.
            // Если sequence = ack = 0, то, скорее всего, это первый пакет, который мы получили, и
            // без этой проверки (if) можно сделать ложный вывод о том, что другая сторона подтвердила
            // получение от нас пакета с sequence number #0. При этом в данном случае не страшно "потерять"
            // это подтверждение, т.к. мы всё равно получим его в последующих пакетах в ack bitfield.
            if (sequence != 0 || ack != 0)
                // Другая сторона подтвердила получение от нас пакета с номером ack.
                packetDelivered(ack);

            if (receivedQueue.size() == PACKETS_QUEUE_SIZE)
                // Удаляем самый "старый" пакет из очереди полученных.
                receivedQueue.pop();

            // Запоминаем sequence number только что полученного пакета.
            receivedQueue.add(sequence);
        }
    }

    /* Этот метод НЕ гарантирует потокобезопасность. Его вызов должен быть обёрнут в блокирующий блок. */
    private void packetSent(PacketContainer pContainer) {
        // Обновляем локальный sequence number только после успешной отправки этого пакета.
        localSequenceNumber = SequenceNumberMath.add(localSequenceNumber, 1); // поддержка wrap-around
        System.out.println("** TEMP DEBUG ** Packet sent, new local seq: #" + localSequenceNumber);

        // Запоминаем этот пакет (но только в случае успешной отправки).
        // В случае потери пакета это поможет отправить его повторно, но
        // уже с другими sequence number, ack и ack bitfield.
        if (sentQueue.size() == PACKETS_QUEUE_SIZE) {
            // Удаляем самый "старый" пакет из очереди отправленных.
            PacketContainer oldestPacketSent = sentQueue.pop();

            // Проверяем, не приходило ли нам "оповещение" об успешном получении этого пакета.
            if (!deliveredQueue.contains(oldestPacketSent.getOriginalSequence()))
                // Судя по всему, "оповещения" о получении этого пакета нам
                // пока не приходило. Скорее всего, пакет не дошёл до цели.
                packetPossiblyLost(oldestPacketSent);
        }

        // Запоминаем sequence number только что отправленного пакета.
        sentQueue.add(pContainer);
    }

    /* Этот метод НЕ гарантирует потокобезопасность. Его вызов должен быть обёрнут в блокирующий блок. */
    private void packetDelivered(int sequence) {
        // Пакет был успешно доставлен. Это совершенно точно.
        // Добавляем его sequence number в очередь доставленных.
        if (deliveredQueue.size() == PACKETS_QUEUE_SIZE)
            deliveredQueue.pop(); // удаляем самый "старый" элемент из очереди доставленных

        // Запоминаем sequence number пакета, об успешной
        // доставке которого нам только что стало известно.
        deliveredQueue.add(sequence);

        // Учитываем доставку пакета в статистике.
        updateDeliveryStat(true);

        System.out.println("** TEMP DEBUG ** Packet delivered: #" + sequence
                + " | new packet loss: " + packetLossPercent + "%");
    }

    /* Этот метод НЕ гарантирует потокобезопасность. Его вызов должен быть обёрнут в блокирующий блок. */
    private void packetPossiblyLost(PacketContainer pContainer) {
        // С момента, как мы отправили этот пакет, мы уже успели отправить 32 новых.
        // Однако подтверждения о доставке этого пакета мы так и не получили. Скорее
        // всего, пакет был потерян где-то "по дороге" (хотя и не обязательно - могло
        // случиться и такое, что это просто до нас не дошла информация о его получении).
        if (pContainer.shouldEnsureDelivered())
            // Этот пакет обязательно нужно доставить. Пробуем снова.
            // При этом конструируем новый пакет - лишь оставляя оригинальное содержимое (сообщение).
            sendPacket(true, pContainer.getOriginalPacket());

        // Учитываем потерю пакета в статистике.
        updateDeliveryStat(false);

        System.out.println("** TEMP DEBUG ** Packet possibly lost: #" + pContainer.getOriginalSequence()
                + " | new packet loss: " + packetLossPercent + "%");
    }

    /* Этот метод НЕ гарантирует потокобезопасность. Его вызов должен быть обёрнут в блокирующий блок. */
    private void updateDeliveryStat(boolean deliveredSuccessfully) {
        if (deliveryStats.size() == PACKETS_QUEUE_SIZE)
            deliveryStats.pop(); // удаляем запись о самом "старом" отправленном пакете

        deliveryStats.add(deliveredSuccessfully ? 1 : 0);

        int packetsSent = deliveryStats.size();
        int packetsDelivered = deliveryStats.stream().mapToInt(Integer::intValue).sum();
        packetLossPercent = 100.0f * (packetsSent - packetsDelivered) / packetsSent;
    }

    public UnwrappedPacketData receivePacket(@NonNull byte[] packetData) {
        UnwrappedPacketData unwrappedPacketData;

        try {
            unwrappedPacketData = PacketTransformer.unwrap(packetData);

            if (unwrappedPacketData != null) {
                packetReceived(unwrappedPacketData);
                return unwrappedPacketData; // получили пакет успешно
            } else
                // Protobuf смог десериализовать полученный пакет, но для него в listeners
                // не зарегистрировано (в конструкторе этого класса) подходящих PacketListener'ов.
                log.error("Ignoring unknown packet from {} ({} bytes)- no implemented transformer.",
                        addr.getHostAddress(), packetData.length);
        } catch (InvalidProtocolBufferException ex) {
            log.error("Ignoring invalid packet from {} ({} bytes).",
                    addr.getHostAddress(), packetData.length, ex);
        }

        return null; // ошибка получения
    }

    public boolean sendPacket(boolean ensureDelivered, @NonNull Message packet) {
        synchronized (lock) {
            try {
                // Конструируем пакет.
                byte[] data = PacketTransformer.wrap(packet,
                        localSequenceNumber,
                        remoteSequenceNumber,
                        calculateAckBitfield()
                );

                System.out.println("** TEMP DEBUG ** Sending packet #" + localSequenceNumber
                        + ", acking #" + remoteSequenceNumber);

                // Отправляем пакет по UDP.
                udpServer.sendRaw(addr, data);

                // "Протоколообразующие" манипуляции.
                packetSent(new PacketContainer(
                        ensureDelivered, localSequenceNumber, packet)); // "протоколообразующие" манипуляции

                return true; // пакет отправлен успешно
            } catch (IOException ex) {
                log.error("Failed to send a {} packet to {}.",
                        packet.getClass().getName(), addr.getHostAddress(), ex);

                return false; // пакет отправить не удалось
            }
        }
    }

}

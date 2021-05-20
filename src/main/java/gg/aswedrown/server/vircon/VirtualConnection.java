package gg.aswedrown.server.vircon;

import com.google.protobuf.Message;
import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.net.NetworkHandle;
import gg.aswedrown.net.NetworkService;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.Constraints;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter @Setter
public class VirtualConnection {

    private static final int   MAX_SEND_QUEUE_SIZE                = 20  ;
    private static final int   MAX_PENDING_PING_TESTS             = 20  ;
    private static final int   GOOD_RTT_THRESHOLD                 = 80  ;
    private static final float GOOD_PACKET_LOSS_PERCENT_THRESHOLD = 2.5f;

    private final Object lock = new Object();

    private final AwdServer srv;

    private final InetAddress addr;

    private final NetworkHandle handle;

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final Deque<PingTest> pendingPingTests = new ArrayDeque<>();

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final Deque<UnwrappedPacketData> receiveQueue = new ArrayDeque<>();

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final Deque<Message> sendQueue = new ArrayDeque<>();

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final Deque<Boolean> ensureDeliveredStatuses = new ArrayDeque<>();

    private volatile boolean authorized;

    private volatile int currentlyHostedLobbyId,
                         currentlyJoinedLobbyId,
                         currentLocalPlayerId,
                         currentCharacter,
                         lastRtt;

    private volatile long lastPongDateTime = System.currentTimeMillis();

    private ActiveGameLobby gameLobby;

    VirtualConnection(@NonNull AwdServer srv, @NonNull InetAddress addr) {
        this.srv = srv;
        this.addr = addr;
        this.handle = new NetworkHandle(srv.getUdpServer(), addr);
    }

    /**
     * @return true, если это соединение помечено как неактивное (клиент не отвечает на Ping-пакеты),
     *         и его нужно закрыть, false - в противном случае, т.е. если это соединение ещё активно.
     */
    public boolean ping() {
        long idleMillis = System.currentTimeMillis() - lastPongDateTime;

        if (idleMillis > srv.getConfig().getCleanerVirConsMaxIdleMillis())
            return true; // соединение уже неактивно - его нужно закрыть

        int testId = ThreadLocalRandom.current().nextInt(
                Constraints.MIN_INT32_ID, Constraints.MAX_INT32_ID);

        synchronized (lock) {
            if (pendingPingTests.size() == MAX_PENDING_PING_TESTS)
                pendingPingTests.pop(); // удаляем самый старый Ping - мы уже вряд ли получим на него ответ (Pong)

            pendingPingTests.add(new PingTest(testId, System.currentTimeMillis()));
        }

        NetworkService.ping(this, testId, lastRtt);

        return false; // соединение ещё активно - его не нужно закрывать
    }

    public void pongReceived(int testId) {
        long currTime = System.currentTimeMillis();

        synchronized (lock) {
            PingTest pongedTest = pendingPingTests.stream()
                    .filter(pingTest -> pingTest.getTestId() == testId)
                    .findAny()
                    .orElse(null);

            if (pongedTest != null) { // принимаем только "актуальные" и "не повреждённые" Pong'и
                lastPongDateTime = currTime;
                pendingPingTests.remove(pongedTest);
                lastRtt = (int) (currTime - pongedTest.getSentTime());
            }
        }
    }

    public void enqueueReceive(@NonNull UnwrappedPacketData packetData) {
        synchronized (lock) {
            receiveQueue.add(packetData);
        }
    }

    public void flushReceiveQueue() {
        synchronized (lock) {
            if (!receiveQueue.isEmpty()) {
                for (UnwrappedPacketData packet : receiveQueue)
                    srv.getPacketManager().processReceivedPacket(this, packet);

                receiveQueue.clear();
            }
        }
    }

    private void enqueueSend(boolean ensureDelivered, @NonNull Message packet) {
        synchronized (lock) {
            if (sendQueue.size() == MAX_SEND_QUEUE_SIZE) {
                // Отменяем отправку самого "старого" пакета (их накопилось уж слишком много).
                sendQueue.pop();
                ensureDeliveredStatuses.pop();
            }

            sendQueue.add(packet);
            ensureDeliveredStatuses.add(ensureDelivered);
        }
    }

    public void enqueueSend(@NonNull Message packet) {
        enqueueSend(false, packet);
    }

    public void enqueueSendImportant(@NonNull Message packet) {
        enqueueSend(true, packet);
    }

    public void flushSendQueue() {
        synchronized (lock) {
            if (!sendQueue.isEmpty()) {
                Iterator<Message> packetsIt  = sendQueue.iterator();
                Iterator<Boolean> statusesIt = ensureDeliveredStatuses.iterator();

                while (packetsIt.hasNext()) {
                    Message packet          = packetsIt .next();
                    boolean ensureDelivered = statusesIt.next();

                    if (ensureDelivered)
                        sendImportantPacket(packet);
                    else
                        sendPacket(packet);
                }

                sendQueue.clear();
                ensureDeliveredStatuses.clear();
            }
        }
    }

    public UnwrappedPacketData receivePacket(@NonNull byte[] packetData) {
        return handle.receivePacket(packetData);
    }

    /**
     * Если пакет не дойдёт до цели, попыток отправить его повторно совершено НЕ будет.
     *
     * Используется для "real-time" пакетов (таких, которые отправляются часто; таких,
     * данные которых быстро устаревают и обновляются с новыми пакетами).
     */
    public boolean sendPacket(@NonNull Message packet) {
        return handle.sendPacket(false, packet);
    }

    /**
     * @see #sendPacket(Message) - то же самое, но эта перегрузка позволяет ещё и подменить
     *                             номер последнего пакета, полученного сервером от клиента.
     */
    public boolean sendPacket(@NonNull Message packet, int ack) {
        return handle.sendPacket(false, packet, ack);
    }

    /**
     * Если пакет не дойдёт до цели, будем пытаться отправить его повторно.
     *
     * Используется для важных единоразовых пакетов, потеря которых недопустима
     * (например, пакеты, связанные с аутентификацией или глобальным изменением
     * состояния).
     */
    public boolean sendImportantPacket(@NonNull Message packet) {
        return handle.sendPacket(true, packet);
    }

    /**
     * @see #sendImportantPacket(Message) - то же самое, но эта перегрузка позволяет ещё и подменить
     *                                      номер последнего пакета, полученного сервером от клиента.
     */
    public boolean sendImportantPacket(@NonNull Message packet, int ack) {
        return handle.sendPacket(true, packet, ack);
    }

    public float getPacketLossPercent() {
        return handle.getPacketLossPercent();
    }

    public boolean isConnectionBad() {
        return lastRtt                       > GOOD_RTT_THRESHOLD
            || handle.getPacketLossPercent() > GOOD_PACKET_LOSS_PERCENT_THRESHOLD;
    }

    public void connectionClosed() {
        log.info("Virtual connection closed: {}.", addr.getHostAddress());

        if (currentlyJoinedLobbyId != 0)
            srv.getLobbyManager().leaveFromLobby(this);
    }

}

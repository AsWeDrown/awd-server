package gg.aswedrown.server.udp;

import gg.aswedrown.net.PacketManager;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.vircon.VirtualConnection;
import gg.aswedrown.server.vircon.VirtualConnectionManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
public class AwdUdpServer implements UdpServer {

    private final int port, bufferSize;

    @NonNull
    private final ExecutorService packetRecvExecService;

    @NonNull
    private final VirtualConnectionManager virConManager;

    @NonNull
    private final PacketManager packetManager;

    private volatile boolean running;

    private DatagramSocket udpSocket;

    @Override
    public void start() throws SocketException {
        log.info("Starting UDP server on port {}. Buffer size: {}.", port, bufferSize);
        log.info("Server protocol version: {}.", PacketManager.PROTOCOL_VERSION);

        running = true;
        udpSocket = new DatagramSocket(port);
        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running) {
            try {
                // Ждём записи очередного UDP пакета в буффер.
                udpSocket.receive(packet);

                // Получаем IP-адрес отправителя пакета.
                InetAddress senderAddr = packet.getAddress();

                // packet.getData() возвращает массив длиной buffer.length, который
                // до нужной длины (длины буффера) дополнен хвостом из нулей. Они
                // нам не нужны. packet.getLength() возвращает точное число полученных
                // байтов - "обрезаем" буффер до этого числа и получаем "реальный" пакет.
                byte[] rawPacketData = Arrays.copyOf(buffer, packet.getLength());

                if (rawPacketData.length > 0) {
                    VirtualConnection virCon = virConManager.getVirtualConnection(senderAddr);

                    if (virCon == null)
                        // Виртуальное соединение не было открыто (например, потому, что сервер переполнен).
                        // В таком случае просто отклоняем полученный пакет.
                        return;

                    packetRecvExecService.execute(() -> {
                        UnwrappedPacketData packetData = virCon.receivePacket(rawPacketData);

                        if (packetData != null) {
                            if (packetData.getPacketType() == PacketWrapper.PacketCase.PONG)
                                // Для пакетов Ping/Pong используем мгновенные отправку/получение.
                                packetManager.processReceivedPacket(virCon, packetData);
                            else
                                virCon.enqueueReceive(packetData);
                        }
                    });
                } else
                    log.warn("Ignoring empty packet from {}.", senderAddr.getHostAddress());
            } catch (IOException ex) {
                if (running) // чтобы не выводить "ошибку" при отключении сервера
                    log.error("Failed to receive a UDP packet.", ex);
            }
        }

        udpSocket.close();
    }

    @Override
    public void sendRaw(@NonNull InetAddress targetAddr, @NonNull byte[] data) throws IOException {
        udpSocket.send(new DatagramPacket(data, data.length, targetAddr, port));
    }

    @Override
    public void stop() {
        running = false;
        udpSocket.close();
    }

}

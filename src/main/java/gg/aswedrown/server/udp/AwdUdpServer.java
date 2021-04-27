package gg.aswedrown.server.udp;

import gg.aswedrown.server.listener.PacketManager;
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
    private final PacketManager packetManager;

    private volatile boolean running;

    private DatagramSocket udpSocket;

    @Override
    public void start() throws SocketException {
        log.info("Starting UDP server on port {}. Buffer size: {}.", port, bufferSize);
        log.info("Protocol version: {}.", PacketManager.PROTOCOL_VERSION);

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
                byte[] packetData = Arrays.copyOf(buffer, packet.getLength());

                System.out.println();
                System.out.println("RECV " + packetData.length
                        + " bytes TO " + senderAddr.getHostAddress() + ":");
                System.out.println(Arrays.toString(packetData));
                System.out.println();

                if (packetData.length > 0)
                    packetRecvExecService.execute(()
                            -> packetManager.receivePacket(senderAddr, packetData));
                else
                    log.warn("Ignoring empty packet from {}.", senderAddr.getHostAddress());
            } catch (IOException ex) {
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

package gg.aswedrown.server.listener;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.util.PacketTransformer;
import gg.aswedrown.server.util.UnwrappedPacketData;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PacketManager {

    private static final String LISTENERS_FORMAT_ERR_MSG =
            "'listeners' must be a non-null array of even length: PacketWrapper.PacketCase enums " +
            "(packet types to bind to) at even indexes, and PacketListener<? extends Message> objects " +
            "(processors for corresponding packets) at odd indexes";

    private final Map<PacketWrapper.PacketCase, PacketListener<? extends Message>>
            listeners = new HashMap<>();

    private final AwdServer srv;

    public PacketManager(AwdServer srv, Object... listeners) {
        this.srv = srv;

        if (listeners == null || listeners.length == 0 || listeners.length % 2 != 0)
            throw new IllegalArgumentException(LISTENERS_FORMAT_ERR_MSG);

        for (int i = 0; i < listeners.length; i += 2) {
            Object key = listeners[i];
            Object val = listeners[i + 1];

            if (key instanceof PacketWrapper.PacketCase && val instanceof PacketListener) {
                try {
                    this.listeners.put((PacketWrapper.PacketCase) key, (PacketListener<? extends Message>) val);
                    log.info("Packets of type {} will be processed by {}.",
                            ((PacketWrapper.PacketCase) key).name(), val.getClass().getName());
                } catch (Exception ex) {
                    log.error("Invalid packet listener binding. Ignoring this entry.", ex);
                    log.error("Hint: {}.", LISTENERS_FORMAT_ERR_MSG);
                }
            } else
                throw new IllegalArgumentException(LISTENERS_FORMAT_ERR_MSG);
        }
    }

    public void receivePacket(InetAddress senderAddr, byte[] packetData) {
        UnwrappedPacketData unwrappedPacketData;

        try {
            unwrappedPacketData = PacketTransformer.unwrap(packetData);

            if (unwrappedPacketData != null) {
                // Всё в порядке. Отправляем пакет на обработку соответствующему PacketListener'у.
                try {
                    listeners.get(unwrappedPacketData.getPacketType())
                            .packetReceived(senderAddr, unwrappedPacketData.getPacket());
                } catch (Exception ex) {
                    log.error("Failed to process a {} packet from {} ({} bytes) " +
                                    "due to an internal error in the corresponding packet listener.",
                            unwrappedPacketData.getPacketType(), senderAddr.getHostAddress(), packetData.length);
                    log.error("Details:", ex);
                }
            } else
                // Protobuf смог десериализовать полученный пакет, но для него в listeners
                // не зарегистрировано (в конструкторе этого класса) подходящих PacketListener'ов.
                log.error("Ignoring unknown packet from {} ({} bytes)" +
                                " - no corresponding packet listener found.",
                        senderAddr.getHostAddress(), packetData.length);
        } catch (InvalidProtocolBufferException ex) {
            log.error("Ignoring invalid packet from {} ({} bytes).",
                    senderAddr.getHostAddress(), packetData.length);
            log.error("Details:", ex);
        }
    }

    public boolean sendPacket(InetAddress targetAddr, Message packet) {
        try {
            byte[] data = PacketTransformer.wrap(packet);
            srv.getUdpServer().send(new DatagramPacket(
                    data, data.length, targetAddr, srv.getConfig().getUdpServerPort()));

            return true; // пакет отправлен успешно
        } catch (IOException ex) {
            log.error("Failed to send a {} packet to {}.",
                    packet.getClass().getName(), targetAddr.getHostAddress());
            log.error("Details:", ex);

            return false; // пакет отправить не удалось
        }
    }

}

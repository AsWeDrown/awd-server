package gg.aswedrown.server.packetlistener;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.util.PacketTransformer;
import gg.aswedrown.server.util.UnwrappedPacketData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PacketManager {

    // TODO - МЕНЯТЬ ЗДЕСЬ:
    public static final int PROTOCOL_VERSION = 1;

    private final Map<PacketWrapper.PacketCase,
                      PacketListener<? extends Message>> listeners = new HashMap<>();

    @NonNull
    private final AwdServer srv;

    public PacketManager(AwdServer srv) {
        this.srv = srv;
        wirePacketListeners();
    }

    /**
     * Автоматически регистрирует все обработчики пакетов из пакета/папки
     *
     *     gg.aswedrown.server.packetlistener
     *
     * (при условии, что они "помечены" аннотацией @RegisterPacketListener).
     */
    private void wirePacketListeners() {
        Reflections reflections = new Reflections("gg.aswedrown.server.packetlistener");

        for (Class<?> clazz : reflections.getTypesAnnotatedWith(RegisterPacketListener.class)) {
            try {
                // Валидация.
                if (!PacketListener.class.isAssignableFrom(clazz))
                    throw new ClassCastException(
                            "class " + clazz.getName() + " is annotated with @RegisterPacketListener, " +
                                    "but does not inherit from " + PacketListener.class.getName());

                // Инициализация.
                RegisterPacketListener anno = clazz.getAnnotation(RegisterPacketListener.class);
                PacketWrapper.PacketCase packetType = anno.value();
                Constructor<PacketListener<? extends Message>> constructor
                        = (Constructor<PacketListener<? extends Message>>) clazz.getDeclaredConstructor(AwdServer.class);

                PacketListener<? extends Message> listener = constructor.newInstance(srv);

                // Регистрация.
                PacketListener<? extends Message> otherListener = listeners.get(packetType);

                if (otherListener != null)
                    throw new IllegalStateException(
                            "multiple classes attempt to listen for packets of type " + packetType
                                    + ": " + otherListener.getClass().getName() + " and " + clazz.getName());

                log.info("Wired packets of type {} to {}.", packetType, clazz.getSimpleName());
            } catch (Exception ex) {
                log.error("Ignoring misconfigured packet listener: {}.", ex.toString());
            }
        }
    }

    public void receivePacket(@NonNull InetAddress senderAddr, @NonNull byte[] packetData) {
        UnwrappedPacketData unwrappedPacketData;

        try {
            unwrappedPacketData = PacketTransformer.unwrap(packetData);

            if (unwrappedPacketData != null) {
                try {
                    // Кажется, всё в порядке. Отправляем пакет на обработку соответствующему PacketListener'у.
                    listeners.get(unwrappedPacketData.getPacketType())
                            .packetReceived(senderAddr, unwrappedPacketData.getPacket());
                } catch (Exception ex) {
                    log.error("Failed to process a {} packet from {} ({} bytes) " +
                                    "due to an internal error in the corresponding packet listener.",
                            unwrappedPacketData.getPacketType(), senderAddr.getHostAddress(), packetData.length, ex);
                }
            } else
                // Protobuf смог десериализовать полученный пакет, но для него в listeners
                // не зарегистрировано (в конструкторе этого класса) подходящих PacketListener'ов.
                log.error("Ignoring unknown packet from {} ({} bytes)" +
                                " - no corresponding packet listener found.",
                        senderAddr.getHostAddress(), packetData.length);
        } catch (InvalidProtocolBufferException ex) {
            log.error("Ignoring invalid packet from {} ({} bytes).",
                    senderAddr.getHostAddress(), packetData.length, ex);
        }
    }

    public boolean sendPacket(@NonNull InetAddress targetAddr, @NonNull Message packet) {
        try {
            byte[] data = PacketTransformer.wrap(packet);
            srv.getUdpServer().sendRaw(targetAddr, data);
            return true; // пакет отправлен успешно
        } catch (IOException ex) {
            log.error("Failed to send a {} packet to {}.",
                    packet.getClass().getName(), targetAddr.getHostAddress(), ex);

            return false; // пакет отправить не удалось
        }
    }

}

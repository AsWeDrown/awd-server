package gg.aswedrown.net;

import com.google.protobuf.Message;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.util.UnwrappedPacketData;
import gg.aswedrown.vircon.VirtualConnection;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
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
                listeners.put(packetType, listener);
            } catch (Exception ex) {
                log.error("Ignoring misconfigured packet listener: {}.", ex.toString());
            }
        }
    }

    public void receivePacket(@NonNull VirtualConnection virCon, @NonNull byte[] packetData) {
        UnwrappedPacketData unwrappedPacketData = virCon.getHandle().receivePacket(packetData);

        if (unwrappedPacketData != null) { // null - внутренняя ошибка получения
            try {
                // Кажется, всё в порядке. Отправляем пакет на обработку соответствующему PacketListener'у.
                listeners.get(unwrappedPacketData.getPacketType())
                        .packetReceived(virCon, unwrappedPacketData.getPacket());
            } catch (Exception ex) {
                log.error("Failed to process a {} packet from {} ({} bytes) " +
                                "due to an internal error in the corresponding packet listener.",
                        unwrappedPacketData.getPacketType(), virCon.getAddr().getHostAddress(),
                        packetData.length, ex);
            }
        }
    }

}
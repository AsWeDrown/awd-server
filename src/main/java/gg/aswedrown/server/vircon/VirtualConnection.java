package gg.aswedrown.server.vircon;

import com.google.protobuf.Message;
import gg.aswedrown.net.NetworkHandle;
import gg.aswedrown.server.udp.UdpServer;
import gg.aswedrown.net.UnwrappedPacketData;
import lombok.*;

import java.net.InetAddress;

@Getter @Setter
public class VirtualConnection {

    private static final long  GOOD_LATENCY_MILLIS_THRESHOLD      = 100;
    private static final float GOOD_PACKET_LOSS_PERCENT_THRESHOLD = 5.0f;

    private final UdpServer udpServer;

    private final InetAddress addr;

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final NetworkHandle handle;

    private volatile boolean authorized;

    private volatile int currentlyHostedLobbyId,
                         currentlyJoinedLobbyId,
                         currentLocalPlayerId;

    private volatile long lastPongDateTime = System.currentTimeMillis(),
                          pongLatency;

    VirtualConnection(@NonNull UdpServer udpServer, @NonNull InetAddress addr) {
        this.udpServer = udpServer;
        this.addr = addr;
        this.handle = new NetworkHandle(udpServer, addr);
    }

    long getMillisSinceLastPong() {
        return System.currentTimeMillis() - lastPongDateTime;
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
     * Если пакет не дойдёт до цели, будем пытаться отправить его повторно.
     *
     * Используется для важных единоразовых пакетов, потеря которых недопустима
     * (например, пакеты, связанные с аутентификацией или глобальным изменением
     * состояния).
     */
    public boolean sendImportantPacket(@NonNull Message packet) {
        return handle.sendPacket(true, packet);
    }

    public float getPacketLossPercent() {
        return handle.getPacketLossPercent();
    }

    public boolean isConnectionBad() {
        return pongLatency                   > GOOD_LATENCY_MILLIS_THRESHOLD
            || handle.getPacketLossPercent() > GOOD_PACKET_LOSS_PERCENT_THRESHOLD;
    }

}

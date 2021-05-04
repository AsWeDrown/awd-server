package gg.aswedrown.vircon;

import com.google.protobuf.Message;
import gg.aswedrown.net.NetworkHandle;
import gg.aswedrown.server.udp.UdpServer;
import lombok.*;

import java.net.InetAddress;

@Getter @Setter
@RequiredArgsConstructor
public class VirtualConnection {

    @NonNull
    private final UdpServer udpServer;

    @NonNull
    private final InetAddress addr;

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final NetworkHandle handle = new NetworkHandle(udpServer, addr);

    private volatile boolean authorized;

    private volatile int currentlyHostedLobbyId,
                         currentlyJoinedLobbyId,
                         currentLocalPlayerId;

    private volatile long lastPongDateTime = System.currentTimeMillis(),
                          pongLatency;

    long getMillisSinceLastPong() {
        return System.currentTimeMillis() - lastPongDateTime;
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

}

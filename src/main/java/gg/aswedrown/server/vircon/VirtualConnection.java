package gg.aswedrown.server.vircon;

import com.google.protobuf.Message;
import gg.aswedrown.net.NetworkHandle;
import gg.aswedrown.net.NetworkService;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

@Getter @Setter
public class VirtualConnection {

    private static final int   MAX_PENDING_PING_TESTS             = 30;
    private static final long  GOOD_LATENCY_MILLIS_THRESHOLD      = 90;
    private static final float GOOD_PACKET_LOSS_PERCENT_THRESHOLD = 5.0f;

    private final Object lock = new Object();

    private final AwdServer srv;

    private final InetAddress addr;

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final NetworkHandle handle;

    @Getter (AccessLevel.NONE) /* закрываем сторонний доступ к этому полю */
    private final Deque<PingTest> pendingPingTests = new ArrayDeque<>();

    private volatile boolean authorized;

    private volatile int currentlyHostedLobbyId,
                         currentlyJoinedLobbyId,
                         currentLocalPlayerId,
                         pongLatency;

    private volatile long lastPongDateTime = System.currentTimeMillis();

    VirtualConnection(@NonNull AwdServer srv, @NonNull InetAddress addr) {
        this.srv = srv;
        this.addr = addr;
        this.handle = new NetworkHandle(srv.getUdpServer(), addr);
    }

    long getMillisSinceLastPong() {
        return System.currentTimeMillis() - lastPongDateTime;
    }

    public void ping() {
        int testId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        long currTime;

        synchronized (lock) {
            if (pendingPingTests.size() == MAX_PENDING_PING_TESTS)
                pendingPingTests.pop(); // удаляем самый старый Ping - мы уже вряд ли получим на него ответ (Pong)

            currTime = System.currentTimeMillis();
            pendingPingTests.add(new PingTest(testId, currTime));
            System.out.println("** TEMP DEBUG ** [1/3] Registered pending ping to "
                    + addr.getHostAddress() + " : testId = " + testId + ", currTime = " + currTime);
        }

        System.out.println("** TEMP DEBUG ** [2/3] Sending ping to " + addr.getHostAddress() + ", testId = "
                + testId + ", millis delay: " + (System.currentTimeMillis() - currTime));
        NetworkService.ping(this, testId, pongLatency);
        System.out.println("** TEMP DEBUG ** [3/3] Sent ping to " + addr.getHostAddress() + ", testId = "
                + testId + ", millis delay: " + (System.currentTimeMillis() - currTime));
    }

    public void pongReceived(int testId) {
        long currTime = System.currentTimeMillis();
        System.out.println("** TEMP DEBUG ** (1/2) RECEIVED ping FROM " + addr.getHostAddress() + ", testId = "
                + testId + ", currTime = " + currTime);

        synchronized (lock) {
            PingTest pongedTest = pendingPingTests.stream()
                    .filter(pingTest -> pingTest.getTestId() == testId)
                    .findAny()
                    .orElse(null);

            if (pongedTest != null) { // принимаем только "актуальные" и "не повреждённые" Pong'и
                lastPongDateTime = currTime;
                pendingPingTests.remove(pongedTest);
                pongLatency = (int) (currTime - pongedTest.getSentTime()) / 2; // "/ 2" для ОДНОсторонней задержки

                System.out.println("** TEMP DEBUG ** (2/2) ACCEPTED ping FROM " + addr.getHostAddress() + ", testId = "
                        + testId + ", millis delay = " + (System.currentTimeMillis() - currTime)
                        + " --> pongLatency := " + pongLatency + " (" + pendingPingTests.size()
                        + " tests still pending)");
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

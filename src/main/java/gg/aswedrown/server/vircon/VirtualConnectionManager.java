package gg.aswedrown.server.vircon;

import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class VirtualConnectionManager {

    private final AwdServer srv;

    private final Map<InetAddress, VirtualConnection> virConMap = new ConcurrentHashMap<>();

    public VirtualConnectionManager(@NonNull AwdServer srv) {
        this.srv = srv;

        // Запускаем периодическую чистку старых, ненужных данных.
        long period = srv.getConfig().getCleanerVirConsCleanupPeriodMillis();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                closeIdleVirtualConnections();
            }
        }, period, period);
    }

    public int getActiveVirtualConnections() {
        return virConMap.size();
    }

    public int getAuthorizedVirtualConnections() {
        return (int) virConMap.values().stream()
                .filter(VirtualConnection::isAuthorized)
                .count();
    }

    public VirtualConnection resolveVirtualConnection(int lobbyId, int playerId) {
        return virConMap.values().stream()
                .filter(virCon -> virCon.getCurrentlyJoinedLobbyId() == lobbyId
                        && virCon.getCurrentLocalPlayerId() == playerId)
                .findAny()
                .orElse(null);
    }

    public VirtualConnection strictGetVirtualConnection(@NonNull InetAddress addr) {
        return virConMap.get(addr);
    }

    public VirtualConnection getVirtualConnection(@NonNull InetAddress addr) {
        VirtualConnection virCon = virConMap.get(addr);

        if (virCon == null) {
            // Виртуального соединения, связанного с этим IP-адресом, ещё нет. Пытаемся открыть его.
            if (virConMap.size() < srv.getConfig().getUdpMaxVirtualConnections()) {
                // Свободные места ещё есть - открываем.
                log.info("Virtual connection established: {}.", addr.getHostAddress());
                virCon = new VirtualConnection(srv, addr);
                virConMap.put(addr, virCon);
            } else
                // Свободных мест больше нет - возвращаем null (отказываем в соединении).
                log.warn("Virtual connection denied: {} (the server is full).", addr.getHostAddress());
        }

        return virCon;
    }

    public void closeVirtualConnection(@NonNull InetAddress addr) {
        VirtualConnection virCon = virConMap.get(addr);

        if (virCon != null) {
            virConMap.remove(addr);

            try {
                virCon.connectionClosed();
            } catch (Exception ex) {
                log.error("Error in connectionClosed() ({}):", addr.getHostAddress(), ex);
            }
        }
    }

    public long getAverageRtt() {
        return (long) virConMap.values().stream()
                .mapToLong(VirtualConnection::getLastRtt)
                .average()
                .orElse(0.0);
    }

    public void flushAllReceiveQueues() {
        virConMap.values().forEach(VirtualConnection::flushReceiveQueue);
    }

    public void flushAllSendQueues() {
        virConMap.values().forEach(VirtualConnection::flushSendQueue);
    }

    /**
     * Пингует клиентов из списка подключённых, удовлетворяющих указанному условию.
     */
    public void pingThose(@NonNull Predicate<? super VirtualConnection> pred) {
        virConMap.values().stream().filter(pred).forEach(VirtualConnection::ping);
    }

    private void closeIdleVirtualConnections() {
        Set<InetAddress> idle = virConMap.keySet().stream()
                .filter(addr -> virConMap.get(addr).getMillisSinceLastPong()
                        >= srv.getConfig().getCleanerVirConsMaxIdleMillis())
                .collect(Collectors.toSet());

        if (!idle.isEmpty()) {
            log.info("Closing {} idle virtual connections (timed out).", idle.size());
            idle.forEach(this::closeVirtualConnection);
        }
    }

}

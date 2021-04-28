package gg.aswedrown.server.data.virtualconnection;

import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class VirtualConnectionManager {

    private final Map<InetAddress, Long> lastLatency = new ConcurrentHashMap<>();

    @NonNull
    private final AwdServer srv;

    @NonNull
    private final VirtualConnectionRepository repo;

    public VirtualConnectionManager(@NonNull AwdServer srv, @NonNull VirtualConnectionRepository repo) {
        this.srv = srv;
        this.repo = repo;

        // Запускаем периодическую чистку старых, ненужных данных.
        new Timer().schedule(new VirtualConnectionCleaner(srv, srv.getDb()),
                // Первым числом ставим 0 (задержка), чтобы чистка
                // выполнялась в том числе сразу при запуске сервера.
                0, srv.getConfig().getDbCleanerVirtualConnectionsCleanupPeriodMillis()
        );
    }

    public Set<InetAddress> getOpenVirtualConnections() {
        return lastLatency.keySet();
    }

    public void openVirtualConnection(@NonNull InetAddress addr) {
        String addrStr = addr.getHostAddress();

        if (isVirtuallyConnected(addrStr)) {
            updatePongLatency(addr, 0L); // сбрасываем пинг И ВРЕМЯ ПОЛУЧЕНИЯ ПОСЛЕДНЕГО ПАКЕТА PONG
            log.info("Virtual connection re-established: {}.", addrStr);
        } else {
            repo.createVirtualConnection(addrStr);
            lastLatency.put(addr, 0L); // ПРОСТО сбрасываем пинг
            log.info("Virtual connection established: {}.", addrStr);
        }
    }

    public void closeVirtualConnection(@NonNull String addrStr) {
        try {
            lastLatency.remove(InetAddress.getByName(addrStr));
        } catch (UnknownHostException ex) {
            log.error("Failed to close virtual connection properly: {} (unknown host).", addrStr);
        }

        repo.deleteVirtualConnection(addrStr);
        log.info("Virtual connection closed: {}. " +
                "There are now {} addresses in memory.", addrStr, lastLatency.size());
    }

    public boolean isVirtuallyConnected(@NonNull String addrStr) {
        return repo.virtualConnectionExists(addrStr);
    }

    public long getAverageLatency() {
        return (long) lastLatency.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    public void updatePongLatency(@NonNull InetAddress addr, long latency) {
        lastLatency.put(addr, latency);
        repo.setLastPongDateTime(addr.getHostAddress(), System.currentTimeMillis());
    }

    public int getCurrentlyHostedLobbyId(@NonNull String addrStr) {
        return repo.getCurrentlyHostedLobbyId(addrStr);
    }

    public int getCurrentlyJoinedLobbyId(@NonNull String addrStr) {
        return repo.getCurrentlyJoinedLobbyId(addrStr);
    }

    public int getCurrentLocalPlayerId(@NonNull String addrStr) {
        return repo.getCurrentLocalPlayerId(addrStr);
    }

    public void bulkSet(@NonNull String addrStr,
                        int currentlyHostedLobbyId,
                        int currentlyJoinedLobbyId,
                        int currentLocalPlayerId) {
        repo.bulkSet(addrStr, currentlyHostedLobbyId, currentlyJoinedLobbyId, currentLocalPlayerId);
    }

    public String resolveVirtualConnection(int lobbyId, int playerId) {
        return repo.resolveVirtualConnection(lobbyId, playerId);
    }

}

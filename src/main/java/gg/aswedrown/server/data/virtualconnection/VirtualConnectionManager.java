package gg.aswedrown.server.data.virtualconnection;

import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;

@Slf4j
public class VirtualConnectionManager {

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

    public void openVirtualConnection(@NonNull String addrStr) {
        if (isVirtuallyConnected(addrStr)) {
            resetLastPacketReceivedDateTime(addrStr);
            log.info("Virtual connection re-established: {}.", addrStr);
        } else {
            repo.createVirtualConnection(addrStr);
            log.info("Virtual connection established: {}.", addrStr);
        }
    }

    public boolean isVirtuallyConnected(@NonNull String addrStr) {
        return repo.virtualConnectionExists(addrStr);
    }

    public void resetLastPacketReceivedDateTime(@NonNull String addrStr) {
        repo.setLastPacketReceivedDateTime(addrStr, System.currentTimeMillis());
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

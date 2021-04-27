package gg.aswedrown.server.data.lobby;

import gg.aswedrown.game.GameState;
import gg.aswedrown.net.KickedFromLobby;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.Constraints;
import gg.aswedrown.server.data.DatabaseCleaner;
import gg.aswedrown.server.data.DbInfo;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class LobbyManager {

    @NonNull
    private final AwdServer srv;

    @NonNull
    private final LobbyRepository repo;

    public LobbyManager(@NonNull AwdServer srv, @NonNull LobbyRepository repo) {
        this.srv = srv;
        this.repo = repo;

        // Запускаем периодическую чистку старых, ненужных данных.
        new Timer().schedule(new DatabaseCleaner(
                        srv.getDb(),
                        DbInfo.Lobbies.COLLECTION_NAME,
                        DbInfo.Lobbies.CREATION_DATE_TIME,
                        srv.getConfig().getDbCleanerLobbiesMaxObjectLifespanMillis(),
                        // Чтобы удалялись только комнаты, в которых ещё идёт ожидание игры:
                        criteria -> criteria.append(DbInfo.Lobbies.GAME_STATE, GameState.LOBBY_STATE)
                ),
                // Первым числом ставим 0 (задержка), чтобы чистка
                // выполнялась в том числе сразу при запуске сервера.
                0, srv.getConfig().getDbCleanerLobbiesCleanupPeriodMillis()
        );
    }

    public CreationResult createNewLobby(@NonNull String creatorAddrStr,
                                         @NonNull String creatorPlayerName) {
        try {
            if (!srv.getVirConManager().isVirtuallyConnected(creatorAddrStr))
                return CreationResult.UNAUTHORIZED;

            if (!creatorPlayerName.matches(Constraints.PLAYER_NAME_PATTERN))
                return CreationResult.BAD_PLAYER_NAME;

            if (srv.getVirConManager().getCurrentlyJoinedLobbyId(creatorAddrStr) != 0)
                // Этот игрок уже состоит в другой комнате, в которой он не является хостом.
                return CreationResult.ALREADY_JOINED_ANOTHER_LOBBY;

            int curHostedLobbyId = srv.getVirConManager().getCurrentlyHostedLobbyId(creatorAddrStr);

            if (curHostedLobbyId == 0) {
                // У этого игрока ещё нет созданных комнат. Создаём.
                int newLobbyId = generateNewLobbyId();
                int localPlayerId = generateNewLocalPlayerId(newLobbyId, true); // ID игрока-хоста

                repo.createLobby(newLobbyId, localPlayerId, creatorPlayerName);
                srv.getVirConManager().bulkSet(creatorAddrStr, newLobbyId, newLobbyId, localPlayerId);
                log.info("Created lobby {} (host: {}#{}).", newLobbyId, creatorPlayerName, localPlayerId);

                return new CreationResult(newLobbyId, localPlayerId);
            } else
                // Этот игрок уже является создателем некоторой комнаты. Возвращаем её данные.
                return new CreationResult(curHostedLobbyId,
                        srv.getVirConManager().getCurrentLocalPlayerId(creatorAddrStr));
        } catch (Exception ex) {
            log.error("Unhandled exception in createNewLobby:", ex);
            return CreationResult.INTERNAL_ERROR;
        }
    }

    private int generateNewLobbyId() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int id;

        do {
            id = rng.nextInt(Constraints.MIN_INT32_ID, Constraints.MAX_INT32_ID);
        } while (repo.lobbyExists(id)); // "лучше перебдеть, чем недобдеть" (с)

        return id;
    }

    private int generateNewLocalPlayerId(int lobbyId, boolean firstMember) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int id = rng.nextInt(Constraints.MIN_INT32_ID, Constraints.MAX_INT32_ID);

        if (!firstMember) // для первого участника доп. проверка не нужна - очевидно, что все ID изначально свободны
            while (repo.isMemberOf(lobbyId, id)) // "лучше перебдеть, чем недобдеть" (с)
                id = rng.nextInt(Constraints.MIN_INT32_ID, Constraints.MAX_INT32_ID);

        return id;
    }

    public void deleteLobby(int lobbyId) {
        Map<String, String> members = repo.getMembers(lobbyId);

        for (String playerIdStr : members.keySet()) {
            int playerId = Integer.parseInt(playerIdStr);
            String addrStr = srv.getVirConManager()
                    .resolveVirtualConnection(lobbyId, playerId);

            if (addrStr != null)
                // Здесь результат кика (true/false) не важен.
                // А вот при ручном кике (кик хоста) - важен.
                kickFromLobby(lobbyId, playerId, addrStr, KickReason.LOBBY_DELETED);
        }

        repo.deleteLobby(lobbyId);
        log.info("Deleted lobby {}.", lobbyId);
    }

    public boolean kickFromLobby(int lobbyId, int targetPlayerId) {
        // TODO: 26.04.2021 дать хостам возможность кикать других участников
        return false;
    }

    private boolean kickFromLobby(int lobbyId, int targetPlayerId,
                                  @NonNull String targetPlayerAddrStr, int reason) {
        boolean actuallyKicked = repo.removeMember(lobbyId, targetPlayerId);

        if (actuallyKicked)
            log.info("Kicked player {} from lobby {} (reason: {}).",
                    targetPlayerId, lobbyId, reason);

        srv.getVirConManager().bulkSet(targetPlayerAddrStr, 0, 0, 0);

        // Оповещаем игрока об исключении из комнаты.
        // TODO: 26.04.2021 возможно, стоит вытеснить оповещения из ЭТОГО класса
        if (actuallyKicked) {
            try {
                srv.getPacketManager().sendPacket(InetAddress.getByName(targetPlayerAddrStr),
                        KickedFromLobby.newBuilder()
                                .setReason(reason)
                                .build()
                );
            } catch (UnknownHostException ex) {
                log.error("Failed to notify {} (address string: {}) about kick from lobby {} " +
                        "(unknown host).", targetPlayerId, targetPlayerAddrStr, lobbyId);
            }
        }

        return actuallyKicked;
    }

    @RequiredArgsConstructor @Getter
    public static final class CreationResult {
        private static final CreationResult BAD_PLAYER_NAME
                = new CreationResult(-1, 0);

        private static final CreationResult ALREADY_JOINED_ANOTHER_LOBBY
                = new CreationResult(-2, 0);

        private static final CreationResult UNAUTHORIZED
                = new CreationResult(-401, 0);

        private static final CreationResult INTERNAL_ERROR
                = new CreationResult(-999, 0);

        private final int lobbyId, playerId;
    }

    public static final class KickReason {
        private KickReason() {}

        private static final int LOBBY_DELETED = 1;
    }

}

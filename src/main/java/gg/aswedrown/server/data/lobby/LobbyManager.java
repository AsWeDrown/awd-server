package gg.aswedrown.server.data.lobby;

import gg.aswedrown.net.KickedFromLobby;
import gg.aswedrown.net.UpdatedMembersList;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.Constraints;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
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
        new Timer().schedule(new LobbyCleaner(srv, srv.getDb()),
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

    public JoinResult joinToLobby(@NonNull String addrStr, int lobbyId, @NonNull String playerName) {
        try {
            if (!srv.getVirConManager().isVirtuallyConnected(addrStr))
                return JoinResult.UNAUTHORIZED;

            if (!playerName.matches(Constraints.PLAYER_NAME_PATTERN))
                return JoinResult.BAD_PLAYER_NAME;

            int curJoinedLobby = srv.getVirConManager().getCurrentlyJoinedLobbyId(addrStr);

            if (curJoinedLobby != 0)
                // Этот игрок уже состоит в какой-то комнате.
                return curJoinedLobby == lobbyId
                        ? JoinResult.ALREADY_JOINED_THIS_LOBBY     // состоит в этой комнате
                        : JoinResult.ALREADY_JOINED_ANOTHER_LOBBY; // состоит в другой комнате

            if (!repo.lobbyExists(lobbyId))
                return JoinResult.LOBBY_DOES_NOT_EXIST;

            Map<String, String> members = repo.getMembers(lobbyId);

            if (members.size() == srv.getConfig().getMaxLobbySize())
                return JoinResult.LOBBY_IS_FULL;

            // Если дошли до этого момента, то всё ок. Добавляем игрока в комнату.
            int playerId = generateNewLocalPlayerId(lobbyId, false);
            boolean added = addMember(lobbyId, playerId, playerName, addrStr);

            if (added) {
                notifyMembersListUpdated(lobbyId, playerId);
                log.info("Player {}#{} joined lobby {}.", playerName, playerId, lobbyId);

                return new JoinResult(playerId, convertMembersMap(members));
            } else
                return JoinResult.ALREADY_JOINED_THIS_LOBBY;
        } catch (Exception ex) {
            log.error("Unhandled exception in joinToLobby:", ex);
            return JoinResult.INTERNAL_ERROR;
        }
    }

    public int leaveFromLobby(@NonNull String addrStr, int lobbyId, int playerId) {
        if (!srv.getVirConManager().isVirtuallyConnected(addrStr))
            return LeaveResult.UNAUTHORIZED;

        int curJoinedLobby = srv.getVirConManager().getCurrentlyJoinedLobbyId(addrStr);
        int curLocalPlayerId = srv.getVirConManager().getCurrentLocalPlayerId(addrStr);

        if (curJoinedLobby != lobbyId || curLocalPlayerId != playerId)
            return LeaveResult.DENIED;

        // Исключаем.
        boolean removed = removeMember(lobbyId, playerId, addrStr);

        if (removed) {
            notifyMembersListUpdated(lobbyId);
            log.info("Player {} left lobby {}.", playerId, lobbyId);

            return LeaveResult.SUCCESS;
        } else
            return LeaveResult.DENIED;
    }

    private Map<Integer, String> convertMembersMap(@NonNull Map<String, String> strToStrMap) {
        Map<Integer, String> intToStrMap = new HashMap<>();
        strToStrMap.forEach((k, v) -> intToStrMap.put(Integer.parseInt(k), v));
        return intToStrMap;
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
                kickFromLobby(lobbyId, playerId, addrStr, KickReason.LOBBY_DELETED);
        }

        repo.deleteLobby(lobbyId);
        log.info("Deleted lobby {}.", lobbyId);
    }

    public void notifyMembersListUpdated(int lobbyId) {
        notifyMembersListUpdated(lobbyId, 0);
    }

    public void notifyMembersListUpdated(int lobbyId, int exceptPlayerId) {
        Map<String, String> members = repo.getMembers(lobbyId);
        Map<Integer, String> convertedMembers = convertMembersMap(members);

        String exceptedPlayerAppdx = exceptPlayerId == 0 ? "" : " (except for player {})";
        log.info("Notifying {} players of lobby {} about its members list update {}.",
                members.size(), lobbyId, exceptedPlayerAppdx);

        for (String playerIdStr : members.keySet()) {
            int playerId = Integer.parseInt(playerIdStr);

            if (playerId != exceptPlayerId) { // чтобы не оповещать самих зашедших игроков об их же входе
                String addrStr = srv.getVirConManager()
                        .resolveVirtualConnection(lobbyId, playerId);

                if (addrStr != null) {
                    // Оповещаем.
                    try {
                        srv.getPacketManager().sendPacket(InetAddress.getByName(addrStr),
                                UpdatedMembersList.newBuilder()
                                        .putAllMembers(convertedMembers)
                                        .build()
                        );
                    } catch (UnknownHostException ex) {
                        log.error("Failed to notify {} (address string: {}) about members list update in lobby {} " +
                                "(unknown host).", playerId, addrStr, lobbyId);
                    }
                }
            }
        }
    }

    private boolean addMember(int lobbyId, int newPlayerId,
                             @NonNull String newPlayerName, @NonNull String targetPlayerAddrStr) {
        boolean actuallyAdded = repo.addMember(lobbyId, newPlayerId, newPlayerName);
        srv.getVirConManager().bulkSet(targetPlayerAddrStr, 0, lobbyId, newPlayerId);
        return actuallyAdded;
    }

    private boolean removeMember(int lobbyId, int targetPlayerId, @NonNull String targetPlayerAddrStr) {
        boolean actuallyRemoved = repo.removeMember(lobbyId, targetPlayerId);
        srv.getVirConManager().bulkSet(targetPlayerAddrStr, 0, 0, 0);
        return actuallyRemoved;
    }

    public boolean kickFromLobby(int lobbyId, int targetPlayerId) {
        // TODO: 26.04.2021 дать хостам возможность кикать других участников
        return false;
    }

    private boolean kickFromLobby(int lobbyId, int targetPlayerId,
                                  @NonNull String targetPlayerAddrStr, int reason) {
        boolean actuallyKicked = removeMember(lobbyId, targetPlayerId, targetPlayerAddrStr);

        if (actuallyKicked)
            log.info("Kicked player {} from lobby {} (reason: {}).",
                    targetPlayerId, lobbyId, reason);

        // Оповещаем игрока об исключении из комнаты.
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

    @RequiredArgsConstructor @Getter
    public static final class JoinResult {
        private static final JoinResult BAD_PLAYER_NAME
                = new JoinResult(-1, null);

        private static final JoinResult LOBBY_IS_FULL
                = new JoinResult(-2, null);

        private static final JoinResult ALREADY_JOINED_THIS_LOBBY
                = new JoinResult(-3, null);

        private static final JoinResult ALREADY_JOINED_ANOTHER_LOBBY
                = new JoinResult(-4, null);

        private static final JoinResult PLAYER_NAME_TAKEN
                = new JoinResult(-5, null);

        private static final JoinResult LOBBY_DOES_NOT_EXIST
                = new JoinResult(-6, null);

        private static final JoinResult UNAUTHORIZED
                = new JoinResult(-401, null);

        private static final JoinResult INTERNAL_ERROR
                = new JoinResult(-999, null);

        private final int playerId;
        private final Map<Integer, String> members;
    }

    public static final class KickReason {
        private KickReason() {}

        private static final int LOBBY_DELETED = 1;
    }

    public static final class LeaveResult {
        private LeaveResult() {}

        private static final int SUCCESS = 1;

        private static final int DENIED         =   -1;
        private static final int UNAUTHORIZED   = -401;
        private static final int INTERNAL_ERROR = -999;
    }

}

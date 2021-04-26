package gg.aswedrown.server.data.lobby;

import gg.aswedrown.net.KickedFromLobby;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.Constraints;
import gg.aswedrown.server.data.player.ConnectionData;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
public class LobbyManager {

    private final AwdServer srv;

    private final LobbyRepository repo;

    public CreationResult createNewLobby(@NonNull InetAddress creatorAddr,
                                         @NonNull String creatorPlayerName) {
        try {
            ConnectionData connData = srv.getConnData(creatorAddr);

            if (connData == null)
                return CreationResult.UNAUTHORIZED;

            if (!creatorPlayerName.matches(Constraints.PLAYER_NAME_PATTERN))
                return CreationResult.BAD_PLAYER_NAME;

            if (connData.getCurrentlyJoinedLobbyId() != 0)
                // Этот игрок уже состоит в другой комнате, в которой он не является хостом.
                return CreationResult.ALREADY_JOINED_ANOTHER_LOBBY;

            int curLobbyId = connData.getCurrentlyHostedLobbyId();

            if (curLobbyId != 0)
                // Этот игрок уже является хостом другой комнаты.
                // Расформировываем его текущую (старую) комнату.
                deleteLobby(curLobbyId);

            int newLobbyId = generateNewLobbyId();
            int localPlayerId = generateNewLocalPlayerId(newLobbyId); // этот ID будет присвоен создателю - хосту

            repo.createLobby(newLobbyId, localPlayerId, creatorPlayerName);
            log.info("Created lobby {} (host: {}#{}).", newLobbyId, creatorPlayerName, localPlayerId);

            connData.setCurrentlyHostedLobbyId(newLobbyId);
            connData.setCurrentlyJoinedLobbyId(newLobbyId);
            connData.setCurrentLocalPlayerId(localPlayerId);

            return new CreationResult(newLobbyId, localPlayerId);
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

    private int generateNewLocalPlayerId(int lobbyId) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int id;

        do {
            id = rng.nextInt(Constraints.MIN_INT32_ID, Constraints.MAX_INT32_ID);
        } while (repo.isMemberOf(lobbyId, id)); // "лучше перебдеть, чем недобдеть" (с)

        return id;
    }

    public void deleteLobby(int lobbyId) {
        Map<Integer, String> members = repo.getMembers(lobbyId);

        for (int playerId : members.keySet()) {
            ConnectionData connData = resolveConnData(lobbyId, playerId);

            if (connData != null)
                // Здесь результат кика (true/false) не важен.
                // А вот при ручном кике (кик хоста) - важен.
                kickFromLobby(connData, KickReason.LOBBY_DELETED);
        }

        repo.deleteLobby(lobbyId);
        log.info("Deleted lobby {}.", lobbyId);
    }

    public boolean kickFromLobby(int lobbyId, int targetPlayerId) {
        // TODO: 26.04.2021 дать хостам возможность кикать других участников
        return false;
    }

    public boolean kickFromLobby(@NonNull ConnectionData targetPlayerConnData, int reason) {
        int curLobbyId = targetPlayerConnData.getCurrentlyJoinedLobbyId();
        boolean actuallyKicked = false;

        if (curLobbyId != 0) {
            int localTargetPlayerId = targetPlayerConnData.getCurrentLocalPlayerId();
            actuallyKicked = repo.removeMember(curLobbyId, localTargetPlayerId);

            if (actuallyKicked)
                log.debug("Kicked player {} from lobby {} (reason: {}).",
                        localTargetPlayerId, curLobbyId, reason);

            targetPlayerConnData.setCurrentlyHostedLobbyId(0);
            targetPlayerConnData.setCurrentlyJoinedLobbyId(0);
            targetPlayerConnData.setCurrentLocalPlayerId(0);

            // Оповещаем игрока об исключении из комнаты.
            // TODO: 26.04.2021 возможно, стоит вытеснить оповещения из ЭТОГО класса
            if (actuallyKicked)
                srv.getPacketManager().sendPacket(targetPlayerConnData.getAddr(),
                        KickedFromLobby.newBuilder()
                                .setReason(reason)
                                .build()
                );
        }

        return actuallyKicked;
    }

    public ConnectionData resolveConnData(int lobbyId, int playerId) {
        if (lobbyId == 0 || playerId == 0)
            // Не состоит ни в какой комнате - это может быть кто угодно.
            return null;

        return srv.getConnDataMap().values().stream()
                .filter(connData -> connData.getCurrentlyJoinedLobbyId() == lobbyId
                        && connData.getCurrentLocalPlayerId() == playerId)
                .findAny()
                .orElse(null); // игрок с указанным локальным ID не состоит в комнате с указанным номером
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

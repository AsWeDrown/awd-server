package gg.aswedrown.server.data.lobby;

import gg.aswedrown.net.NetworkService;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.Constraints;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                0, srv.getConfig().getCleanerLobbiesCleanupPeriodMillis()
        );
    }

    public CreationResult createNewLobby(@NonNull VirtualConnection virCon,
                                         @NonNull String creatorPlayerName) {
        if (!virCon.isAuthorized())
            return CreationResult.UNAUTHORIZED;

        try {
            if (!creatorPlayerName.matches(Constraints.PLAYER_NAME_PATTERN))
                return CreationResult.BAD_PLAYER_NAME;

            int curJoinedLobbyId = virCon.getCurrentlyJoinedLobbyId();
            int curHostedLobbyId = virCon.getCurrentlyHostedLobbyId();

            if (curJoinedLobbyId != 0 && curJoinedLobbyId != curHostedLobbyId)
                // Этот игрок уже состоит в другой комнате, в которой он не является хостом.
                return CreationResult.ALREADY_JOINED_ANOTHER_LOBBY;
            else if (curHostedLobbyId == 0) {
                // У этого игрока ещё нет созданных комнат. Создаём.
                int newLobbyId = generateNewLobbyId();
                int localPlayerId = generateNewLocalPlayerId(newLobbyId, true); // локальный ID игрока-хоста
                int character = ThreadLocalRandom.current().nextInt(
                        Constraints.MIN_CHARACTER_ID, 1 + Constraints.MAX_CHARACTER_ID);

                repo.createLobby(newLobbyId, localPlayerId, creatorPlayerName, character);

                virCon.setCurrentlyHostedLobbyId(newLobbyId);
                virCon.setCurrentlyJoinedLobbyId(newLobbyId);
                virCon.setCurrentLocalPlayerId(localPlayerId);
                virCon.setCurrentCharacter(character);

                log.info("Created lobby {} (host: {}#{}).", newLobbyId, creatorPlayerName, localPlayerId);

                return new CreationResult(newLobbyId, localPlayerId, character);
            } else {
                // Этот игрок уже является создателем некоторой комнаты. Возвращаем её данные.
                int registeredHostId = virCon.getCurrentLocalPlayerId();
                int curCharacter = virCon.getCurrentCharacter();
                String originalName = repo.getMembersNames(curHostedLobbyId)
                        .get(Integer.toString(registeredHostId));

                if (!creatorPlayerName.equals(originalName))
                    // Игрок "переприсоединился" к комнате с другим именем. Устанавливаем новое имя в БД.
                    if (!repo.updateMemberName(curHostedLobbyId, registeredHostId, creatorPlayerName))
                        // Обновить имя не получилось. Что-то здесь не так...
                        return CreationResult.FORBIDDEN;

                return new CreationResult(curHostedLobbyId, registeredHostId, curCharacter);
            }
        } catch (Exception ex) {
            log.error("Unhandled exception in createNewLobby:", ex);
            return CreationResult.INTERNAL_ERROR;
        }
    }

    public JoinResult joinToLobby(@NonNull VirtualConnection virCon, int lobbyId, @NonNull String playerName) {
        if (!virCon.isAuthorized())
            return JoinResult.UNAUTHORIZED;

        try {
            if (!playerName.matches(Constraints.PLAYER_NAME_PATTERN))
                return JoinResult.BAD_PLAYER_NAME;

            int curJoinedLobby = virCon.getCurrentlyJoinedLobbyId();

            if (curJoinedLobby != 0)
                // Этот игрок уже состоит в какой-то комнате.
                return curJoinedLobby == lobbyId
                        ? JoinResult.ALREADY_JOINED_THIS_LOBBY     // состоит в этой комнате
                        : JoinResult.ALREADY_JOINED_ANOTHER_LOBBY; // состоит в другой комнате

            if (!repo.lobbyExists(lobbyId))
                return JoinResult.LOBBY_DOES_NOT_EXIST;

            Map<String, String> membersNames = repo.getMembersNames(lobbyId);

            if (membersNames.size() == srv.getConfig().getMaxLobbySize())
                return JoinResult.LOBBY_IS_FULL;

            Map<Integer, Integer> membersCharacters
                    = convertMembersCharactersMap(repo.getMembersCharacters(lobbyId));

            // Если дошли до этого момента, то всё ок. Добавляем игрока в комнату.
            int playerId = generateNewLocalPlayerId(lobbyId, false);
            int character = generateNextCharacter(membersCharacters);
            boolean added = addMember(lobbyId, playerId, playerName, character, virCon);

            if (added) {
                notifyMembersListUpdated(lobbyId, playerId);
                log.info("Player {}#{} joined lobby {}.", playerName, playerId, lobbyId);

                return new JoinResult(playerId, repo.getHost(lobbyId), character,
                        convertMembersNamesMap(membersNames), membersCharacters);
            } else
                return JoinResult.ALREADY_JOINED_THIS_LOBBY;
        } catch (Exception ex) {
            log.error("Unhandled exception in joinToLobby:", ex);
            return JoinResult.INTERNAL_ERROR;
        }
    }

    public int leaveFromLobby(@NonNull VirtualConnection virCon, int lobbyId, int playerId) {
        if (!virCon.isAuthorized())
            return LeaveResult.UNAUTHORIZED;

        try {
            int curJoinedLobby = virCon.getCurrentlyJoinedLobbyId();
            int curLocalPlayerId = virCon.getCurrentLocalPlayerId();

            if (curJoinedLobby != lobbyId || curLocalPlayerId != playerId)
                return LeaveResult.DENIED;

            // Исключаем.
            boolean removed = removeMember(lobbyId, playerId, virCon);

            if (removed) {
                notifyMembersListUpdated(lobbyId);
                log.info("Player {} left lobby {}.", playerId, lobbyId);

                return LeaveResult.SUCCESS;
            } else
                return LeaveResult.DENIED;
        } catch (Exception ex) {
            log.error("Unhandled exception in leaveFromLobby:", ex);
            return LeaveResult.INTERNAL_ERROR;
        }
    }

    private Map<Integer, String> convertMembersNamesMap(@NonNull Map<String, String> strToStrMap) {
        Map<Integer, String> intToStrMap = new HashMap<>();
        strToStrMap.forEach((k, v) -> intToStrMap.put(Integer.parseInt(k), v));
        return intToStrMap;
    }

    private Map<Integer, Integer> convertMembersCharactersMap(@NonNull Map<String, String> strToStrMap) {
        Map<Integer, Integer> intToIntMap = new HashMap<>();
        strToStrMap.forEach((k, v) -> intToIntMap.put(Integer.parseInt(k), Integer.parseInt(v)));
        return intToIntMap;
    }

    public void deleteLobby(int lobbyId) {
        Map<String, String> members = repo.getMembersNames(lobbyId);

        for (String playerIdStr : members.keySet()) {
            int playerId = Integer.parseInt(playerIdStr);
            VirtualConnection virCon = srv.getVirConManager()
                    .resolveVirtualConnection(lobbyId, playerId);

            if (virCon != null)
                kickFromLobby(lobbyId, playerId, virCon, KickReason.LOBBY_DELETED);
        }

        repo.deleteLobby(lobbyId);
        log.info("Deleted lobby {}.", lobbyId);
    }

    public void notifyMembersListUpdated(int lobbyId) {
        notifyMembersListUpdated(lobbyId, 0);
    }

    public void notifyMembersListUpdated(int lobbyId, int exceptPlayerId) {
        Map<String, String> members = repo.getMembersNames(lobbyId);
        Map<Integer, String> convertedMembers = convertMembersNamesMap(members);

        String exceptedPlayerAppdx = exceptPlayerId == 0 ? "" : " (except for player {})";
        log.info("Notifying {} players of lobby {} about its members list update {}.",
                members.size(), lobbyId, exceptedPlayerAppdx);

        for (String playerIdStr : members.keySet()) {
            int playerId = Integer.parseInt(playerIdStr);

            if (playerId != exceptPlayerId) { // чтобы не оповещать самих зашедших игроков об их же входе
                VirtualConnection virCon = srv.getVirConManager()
                        .resolveVirtualConnection(lobbyId, playerId);

                if (virCon != null)
                    // Оповещаем.
                    NetworkService.updatedMembersList(virCon, convertedMembers);
            }
        }
    }

    private boolean addMember(int lobbyId, int newPlayerId, @NonNull String newPlayerName,
                              int character, @NonNull VirtualConnection virCon) {
        boolean actuallyAdded = repo.addMember(lobbyId, newPlayerId, newPlayerName, character);

        if (actuallyAdded) {
            virCon.setCurrentlyHostedLobbyId(0);
            virCon.setCurrentlyJoinedLobbyId(lobbyId);
            virCon.setCurrentLocalPlayerId(newPlayerId);
            virCon.setCurrentCharacter(character);
        }

        return actuallyAdded;
    }

    private boolean removeMember(int lobbyId, int targetPlayerId, @NonNull VirtualConnection virCon) {
        boolean actuallyRemoved = repo.removeMember(lobbyId, targetPlayerId);

        if (actuallyRemoved) {
            virCon.setCurrentlyHostedLobbyId(0);
            virCon.setCurrentlyJoinedLobbyId(0);
            virCon.setCurrentLocalPlayerId(0);
            virCon.setCurrentCharacter(0);
        }

        return actuallyRemoved;
    }

    public boolean kickFromLobby(int lobbyId, int targetPlayerId) {
        // TODO: 26.04.2021 дать хостам возможность кикать других участников
        return false;
    }

    private boolean kickFromLobby(int lobbyId, int targetPlayerId,
                                  @NonNull VirtualConnection virCon, int reason) {
        boolean actuallyKicked = removeMember(lobbyId, targetPlayerId, virCon);

        if (actuallyKicked)
            log.info("Kicked player {} from lobby {} (reason: {}).",
                    targetPlayerId, lobbyId, reason);

        if (actuallyKicked)
            // Оповещаем игрока об исключении из комнаты.
            NetworkService.kickedFromLobby(virCon, reason);

        return actuallyKicked;
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

    private int generateNextCharacter(@NonNull Map<Integer, Integer> takenCharacters) {
        if (ThreadLocalRandom.current().nextBoolean()) { // "рандомность" в том, с какого конца идём
            for (int i = 1; i <= Constraints.MAX_CHARACTER_ID; i++)
                if (!takenCharacters.containsValue(i))
                    return i;
        } else {
            for (int i = Constraints.MAX_CHARACTER_ID; i >= 1 ; i--)
                if (!takenCharacters.containsValue(i))
                    return i;
        }

        throw new IllegalStateException("out of characters (lobby size exceeded?)");
    }

    @RequiredArgsConstructor @Getter
    public static final class CreationResult {
        private static final CreationResult BAD_PLAYER_NAME
                = new CreationResult(-1, 0, 0);

        private static final CreationResult ALREADY_JOINED_ANOTHER_LOBBY
                = new CreationResult(-2, 0, 0);

        private static final CreationResult UNAUTHORIZED
                = new CreationResult(-401, 0, 0);

        private static final CreationResult FORBIDDEN
                = new CreationResult(-403, 0, 0);

        private static final CreationResult INTERNAL_ERROR
                = new CreationResult(-999, 0, 0);

        private final int lobbyId, playerId, character;
    }

    @RequiredArgsConstructor @Getter
    public static final class JoinResult {
        private static final JoinResult BAD_PLAYER_NAME
                = new JoinResult(-1, 0, 0, null, null);

        private static final JoinResult LOBBY_IS_FULL
                = new JoinResult(-2, 0, 0, null, null);

        private static final JoinResult ALREADY_JOINED_THIS_LOBBY
                = new JoinResult(-3, 0, 0, null, null);

        private static final JoinResult ALREADY_JOINED_ANOTHER_LOBBY
                = new JoinResult(-4, 0, 0, null, null);

        private static final JoinResult PLAYER_NAME_TAKEN
                = new JoinResult(-5, 0, 0, null, null);

        private static final JoinResult LOBBY_DOES_NOT_EXIST
                = new JoinResult(-6, 0, 0, null, null);

        private static final JoinResult UNAUTHORIZED
                = new JoinResult(-401, 0, 0, null, null);

        private static final JoinResult INTERNAL_ERROR
                = new JoinResult(-999, 0, 0, null, null);

        private final int playerId, character, hostId;
        private final Map<Integer, String> membersNames;
        private final Map<Integer, Integer> membersCharacters;
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

package gg.aswedrown.server.data.lobby;

import gg.aswedrown.game.GameState;
import gg.aswedrown.server.data.DbInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class MongoLobbyRepository implements LobbyRepository {

    @NonNull
    private final MongoManager db;

    @Override
    public void createLobby(int lobbyId, int hostPlayerId, @NonNull String hostPlayerName, int hostCharacter) {
        // Преобразовываем число ID в строку, т.к. в Mongo легче всего сохранять Map<String, String>.
        // (Через обычную вставку Mongo сериализует именно в <String, String>, а вставлять поэлементно менее удобно.)
        String hostPlayerIdStr = Integer.toString(hostPlayerId);

        Map<String, String> membersNames = new HashMap<>();
        membersNames.put(hostPlayerIdStr, hostPlayerName);

        Map<String, String> membersCharacters = new HashMap<>();
        membersCharacters.put(hostPlayerIdStr, Integer.toString(hostCharacter));

        Document doc = new Document(DbInfo.Lobbies.LOBBY_ID, lobbyId)
                .append(DbInfo.Lobbies.CREATION_DATE_TIME, System.currentTimeMillis())
                .append(DbInfo.Lobbies.GAME_STATE, GameState.LOBBY_STATE)
                .append(DbInfo.Lobbies.HOST_PLAYER_ID, hostPlayerId)
                .append(DbInfo.Lobbies.MEMBERS_NAMES, membersNames)
                .append(DbInfo.Lobbies.MEMBERS_CHARACTERS, membersCharacters);

        db.insertOne(DbInfo.Lobbies.COLLECTION_NAME, doc);
    }

    @Override
    public void deleteLobby(int lobbyId) {
        db.deleteOne(DbInfo.Lobbies.COLLECTION_NAME, DbInfo.Lobbies.LOBBY_ID, lobbyId);
    }

    @Override
    public boolean lobbyExists(int lobbyId) {
        return fetchLobbyData(lobbyId) != null;
    }

    @Override
    public boolean updateMemberName(int lobbyId, int playerId, @NonNull String newPlayerName) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        Map<String, String> members = getMembersNames(lobbyId);
        String playerIdStr = Integer.toString(playerId);

        if (members.containsKey(playerIdStr)) {
            members.put(playerIdStr, newPlayerName);

            db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                    DbInfo.Lobbies.LOBBY_ID, lobbyId,
                    new Document(DbInfo.Lobbies.MEMBERS_NAMES, members)
            );

            return true; // имя успешно обновлено
        } else
            return false; // в этой комнате нет такого участника
    }

    @Override
    public boolean addMember(int lobbyId, int playerId, @NonNull String playerName, int character) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        Map<String, String> membersNames = getMembersNames(lobbyId);
        String playerIdStr = Integer.toString(playerId);

        if (membersNames.containsKey(playerIdStr))
            return false; // уже участник
        else {
            Map<String, String> membersCharacters = getMembersCharacters(lobbyId);

            membersNames.put(playerIdStr, playerName);
            membersCharacters.put(playerIdStr, Integer.toString(character));

            db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                    DbInfo.Lobbies.LOBBY_ID, lobbyId,
                    new Document(DbInfo.Lobbies.MEMBERS_NAMES,      membersNames    ).
                          append(DbInfo.Lobbies.MEMBERS_CHARACTERS, membersCharacters)
            );

            return true; // успешно добавлен
        }
    }

    @Override
    public boolean removeMember(int lobbyId, int playerId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        Map<String, String> membersNames = getMembersNames(lobbyId);
        String playerIdStr = Integer.toString(playerId);

        if (membersNames.containsKey(playerIdStr)) {
            Map<String, String> membersCharacters = getMembersCharacters(lobbyId);

            membersNames.remove(playerIdStr);
            membersCharacters.remove(playerIdStr);

            db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                    DbInfo.Lobbies.LOBBY_ID, lobbyId,
                    new Document(DbInfo.Lobbies.MEMBERS_NAMES,      membersNames    ).
                          append(DbInfo.Lobbies.MEMBERS_CHARACTERS, membersCharacters)
            );

            return true; // успешно исключён
        } else
            return false; // не участник
    }

    @Override
    public boolean isMemberOf(int lobbyId, int playerId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        return getMembersNames(lobbyId).containsKey(Integer.toString(playerId));
    }

    @Override
    public int getHost(int lobbyId) {
        return fetchLobbyData(lobbyId).getInteger(DbInfo.Lobbies.HOST_PLAYER_ID);
    }

    @Override
    public Map<String, String> getMembersNames(int lobbyId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        return fetchLobbyData(lobbyId).get(DbInfo.Lobbies.MEMBERS_NAMES, Map.class);
    }

    @Override
    public Map<String, String> getMembersCharacters(int lobbyId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        return fetchLobbyData(lobbyId).get(DbInfo.Lobbies.MEMBERS_CHARACTERS, Map.class);
    }

    @Override
    public int getGameState(int lobbyId) {
        return fetchLobbyData(lobbyId).getInteger(DbInfo.Lobbies.GAME_STATE);
    }

    @Override
    public void setGameState(int lobbyId, int gameState) {
        db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                DbInfo.Lobbies.LOBBY_ID, lobbyId,
                new Document(DbInfo.Lobbies.GAME_STATE, gameState)
        );
    }

    @Override
    public Document fetchLobbyData(int lobbyId) {
        return db.fetchFirst(DbInfo.Lobbies.COLLECTION_NAME, DbInfo.Lobbies.LOBBY_ID, lobbyId);
    }

}

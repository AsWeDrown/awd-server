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
    public void createLobby(int lobbyId, int hostPlayerId, @NonNull String hostPlayerName) {
        // Преобразовываем число ID в строку, т.к. в Mongo легче всего сохранять Map<String, String>.
        // (Через обычную вставку Mongo сериализует именно в <String, String>, а вставлять поэлементно менее удобно.)
        Map<String, String> members = new HashMap<>();
        members.put(Integer.toString(hostPlayerId), hostPlayerName);
        Document doc = new Document(DbInfo.Lobbies.LOBBY_ID, lobbyId)
                .append(DbInfo.Lobbies.CREATION_DATE_TIME, System.currentTimeMillis())
                .append(DbInfo.Lobbies.GAME_STATE, GameState.LOBBY_STATE)
                .append(DbInfo.Lobbies.HOST_PLAYER_ID, hostPlayerId)
                .append(DbInfo.Lobbies.MEMBERS, members);

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
    public boolean addMember(int lobbyId, int playerId, @NonNull String playerName) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        Map<String, String> members = getMembers(lobbyId);
        String playerIdStr = Integer.toString(playerId);

        if (members.containsKey(playerIdStr))
            return false; // уже участник
        else {
            members.put(playerIdStr, playerName);
            db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                    DbInfo.Lobbies.LOBBY_ID, lobbyId,
                    new Document(DbInfo.Lobbies.MEMBERS, members)
            );

            return true; // успешно добавлен
        }
    }

    @Override
    public boolean removeMember(int lobbyId, int playerId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        Map<String, String> members = getMembers(lobbyId);
        String playerIdStr = Integer.toString(playerId);

        if (members.containsKey(playerIdStr)) {
            members.remove(playerIdStr);
            db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                    DbInfo.Lobbies.LOBBY_ID, lobbyId,
                    new Document(DbInfo.Lobbies.MEMBERS, members)
            );

            return true; // успешно исключён
        } else
            return false; // не участник
    }

    @Override
    public boolean isMemberOf(int lobbyId, int playerId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        return getMembers(lobbyId).containsKey(Integer.toString(playerId));
    }

    @Override
    public int getHost(int lobbyId) {
        return fetchLobbyData(lobbyId).getInteger(DbInfo.Lobbies.HOST_PLAYER_ID);
    }

    @Override
    public Map<String, String> getMembers(int lobbyId) {
        // См. коммент в методе createLobby - причина <String, String> вместо <Integer, String>.
        return fetchLobbyData(lobbyId).get(DbInfo.Lobbies.MEMBERS, Map.class);
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

package gg.aswedrown.server.data.lobby;

import gg.aswedrown.server.data.DbInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

// TODO: 26.04.2021 оптимизировать работу с БД (сейчас код удобный и простой, но с производительностью не очень)
@RequiredArgsConstructor
public class MongoLobbyRepository implements LobbyRepository {

    private final MongoManager db;

    @Override
    public void createLobby(int lobbyId, int hostPlayerId, @NonNull String hostPlayerName) {
        Map<Integer, String> members = new HashMap<>();
        members.put(hostPlayerId, hostPlayerName);
        Document doc = new Document(DbInfo.Lobbies.LOBBY_ID, lobbyId)
                .append(DbInfo.Lobbies.CURRENT_STATE, LobbyState.LOBBY_STATE)
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
        Map<Integer, String> members = getMembers(lobbyId);

        if (members.containsKey(playerId))
            return false; // уже участник
        else {
            members.put(playerId, playerName);
            db.updateOne(DbInfo.Lobbies.COLLECTION_NAME,
                    DbInfo.Lobbies.LOBBY_ID, lobbyId,
                    new Document(DbInfo.Lobbies.MEMBERS, members)
            );

            return true; // успешно добавлен
        }
    }

    @Override
    public boolean removeMember(int lobbyId, int playerId) {
        Map<Integer, String> members = getMembers(lobbyId);

        if (members.containsKey(playerId)) {
            members.remove(playerId);
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
        return getMembers(lobbyId).containsKey(playerId);
    }

    @Override
    public int getHost(int lobbyId) {
        return fetchLobbyData(lobbyId).getInteger(DbInfo.Lobbies.HOST_PLAYER_ID);
    }

    @Override
    public Map<Integer, String> getMembers(int lobbyId) {
        return fetchLobbyData(lobbyId).get(DbInfo.Lobbies.MEMBERS, Map.class);
    }

    @Override
    public Document fetchLobbyData(int lobbyId) {
        return db.fetchFirst(DbInfo.Lobbies.COLLECTION_NAME, DbInfo.Lobbies.LOBBY_ID, lobbyId);
    }

}

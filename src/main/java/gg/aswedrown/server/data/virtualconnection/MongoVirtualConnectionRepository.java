package gg.aswedrown.server.data.virtualconnection;

import com.mongodb.client.MongoCollection;
import gg.aswedrown.server.data.DbInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.bson.Document;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
public class MongoVirtualConnectionRepository implements VirtualConnectionRepository {

    @NonNull
    private final MongoManager db;

    @Override
    public void createVirtualConnection(@NonNull String addrStr) {
        Document doc = new Document(DbInfo.VirtualConnections.ADDR_STR, addrStr)
                .append(DbInfo.VirtualConnections.LAST_PACKET_RECEIVED_DATE_TIME, System.currentTimeMillis())
                .append(DbInfo.VirtualConnections.CURRENTLY_HOSTED_LOBBY_ID, 0)
                .append(DbInfo.VirtualConnections.CURRENTLY_JOINED_LOBBY_ID, 0)
                .append(DbInfo.VirtualConnections.CURRENT_LOCAL_PLAYER_ID, 0);

        db.insertOne(DbInfo.VirtualConnections.COLLECTION_NAME, doc);
    }

    @Override
    public boolean virtualConnectionExists(@NonNull String addrStr) {
        return fetchVirtualConnectionData(addrStr) != null;
    }

    @Override
    public void setLastPacketReceivedDateTime(@NonNull String addrStr, long timestamp) {
        db.updateOne(DbInfo.VirtualConnections.COLLECTION_NAME,
                DbInfo.VirtualConnections.ADDR_STR, addrStr,
                new Document(DbInfo.VirtualConnections.LAST_PACKET_RECEIVED_DATE_TIME, timestamp)
        );
    }

    @Override
    public int getCurrentlyHostedLobbyId(@NonNull String addrStr) {
        return fetchVirtualConnectionData(addrStr)
                .getInteger(DbInfo.VirtualConnections.CURRENTLY_HOSTED_LOBBY_ID);
    }

    @Override
    public int getCurrentlyJoinedLobbyId(@NonNull String addrStr) {
        return fetchVirtualConnectionData(addrStr)
                .getInteger(DbInfo.VirtualConnections.CURRENTLY_JOINED_LOBBY_ID);
    }

    @Override
    public int getCurrentLocalPlayerId(@NonNull String addrStr) {
        return fetchVirtualConnectionData(addrStr)
                .getInteger(DbInfo.VirtualConnections.CURRENT_LOCAL_PLAYER_ID);
    }

    @Override
    public void bulkSet(@NonNull String addrStr,
                        int currentlyHostedLobbyId,
                        int currentlyJoinedLobbyId,
                        int currentLocalPlayerId) {
        MongoCollection<Document> col = db.getCollection(DbInfo.VirtualConnections.COLLECTION_NAME);
        Document targetEntry = col.find(new Document(DbInfo.VirtualConnections.ADDR_STR, addrStr)).first();

        if (targetEntry == null)
            throw new NoSuchElementException(
                    "no such virtual connection in database: " + addrStr);

        Document updatedData = new Document()
                .append(DbInfo.VirtualConnections.CURRENTLY_HOSTED_LOBBY_ID, currentlyHostedLobbyId)
                .append(DbInfo.VirtualConnections.CURRENTLY_JOINED_LOBBY_ID, currentlyJoinedLobbyId)
                .append(DbInfo.VirtualConnections.CURRENT_LOCAL_PLAYER_ID, currentLocalPlayerId);

        col.updateOne(targetEntry, new Document("$set", updatedData));
    }

    @Override
    public String resolveVirtualConnection(int lobbyId, int playerId) {
        MongoCollection<Document> col = db.getCollection(DbInfo.VirtualConnections.COLLECTION_NAME);
        Document criteria = new Document()
                .append(DbInfo.VirtualConnections.CURRENTLY_JOINED_LOBBY_ID, lobbyId)
                .append(DbInfo.VirtualConnections.CURRENT_LOCAL_PLAYER_ID, playerId);

        Document resolvedData = col.find(criteria).first();
        return resolvedData == null ? null : resolvedData.getString(DbInfo.VirtualConnections.ADDR_STR);
    }

    @Override
    public Document fetchVirtualConnectionData(@NonNull String addrStr) {
        return db.fetchFirst(DbInfo.VirtualConnections.COLLECTION_NAME,
                             DbInfo.VirtualConnections.ADDR_STR, addrStr);
    }

}

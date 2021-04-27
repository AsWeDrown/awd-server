package gg.aswedrown.server.data.virtualconnection;

import lombok.NonNull;
import org.bson.Document;

// TODO: 26.04.2021 periodic cleanup based on millis since last packet recv
public interface VirtualConnectionRepository {

    void createVirtualConnection(@NonNull String addrStr);

    void deleteVirtualConnection(@NonNull String addrStr);

    boolean virtualConnectionExists(@NonNull String addrStr);

    void setLastPacketReceivedDateTime(@NonNull String addrStr, long timestamp);
    
    int getCurrentlyHostedLobbyId(@NonNull String addrStr);

    int getCurrentlyJoinedLobbyId(@NonNull String addrStr);

    int getCurrentLocalPlayerId(@NonNull String addrStr);

    void bulkSet(@NonNull String addrStr,
                 int currentlyHostedLobbyId,
                 int currentlyJoinedLobbyId,
                 int currentLocalPlayerId);

    String resolveVirtualConnection(int lobbyId, int playerId);

    Document fetchVirtualConnectionData(@NonNull String addrStr);
    
}

package gg.aswedrown.net;

import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.NonNull;

import java.util.Map;

public class NetworkService {

    private NetworkService() {}

    public static void handshakeResponse(@NonNull VirtualConnection virCon) {
        virCon.enqueueSendImportant(HandshakeResponse.newBuilder()
                .setProtocolVersion(PacketManager.PROTOCOL_VERSION)
                .build()
        );
    }

    public static void createLobbyResponse(@NonNull VirtualConnection virCon,
                                           @NonNull LobbyManager.CreationResult result) {
        virCon.enqueueSendImportant(CreateLobbyResponse.newBuilder()
                .setLobbyId(result.getLobbyId())
                .setPlayerId(result.getPlayerId())
                .setCharacter(result.getCharacter())
                .build()
        );
    }

    public static void joinLobbyResponse(@NonNull VirtualConnection virCon,
                                         @NonNull LobbyManager.JoinResult result) {
        virCon.enqueueSendImportant(JoinLobbyResponse.newBuilder()
                .setPlayerId(result.getPlayerId())
                .setCharacter(result.getCharacter())
                .setHostId(result.getHostId())
                .putAllOthersNames(result.getMembersNames())
                .putAllOthersCharacters(result.getMembersCharacters())
                .build()
        );
    }

    public static void leaveLobbyResponse(@NonNull VirtualConnection virCon, int result) {
        virCon.enqueueSendImportant(LeaveLobbyResponse.newBuilder()
                .setStatusCode(result)
                .build()
        );
    }

    public static void ping(@NonNull VirtualConnection virCon, int testId, int rtt) {
        virCon.sendPacket(Ping.newBuilder() // Для пакетов Ping/Pong используем мгновенные отправку/получение.
                .setTestId(testId)
                .setRtt(rtt)
                .build()
        );
    }

    public static void updatedMembersList(@NonNull VirtualConnection virCon,
                                          @NonNull Map<Integer, String> newAllNames,
                                          @NonNull Map<Integer, Integer> newAllCharacters) {
        virCon.enqueueSendImportant(UpdatedMembersList.newBuilder()
                .putAllNewAllNames(newAllNames)
                .putAllNewAllCharacters(newAllCharacters)
                .build()
        );
    }

    public static void kickedFromLobby(@NonNull VirtualConnection virCon, int reason) {
        virCon.enqueueSendImportant(KickedFromLobby.newBuilder()
                .setReason(reason)
                .build()
        );
    }

    public static void beginPlayStateResponse(@NonNull VirtualConnection virCon, int result) {
        virCon.enqueueSendImportant(BeginPlayStateResponse.newBuilder()
                .setStatusCode(result)
                .build()
        );
    }

    public static void updateDimensionCommand(@NonNull VirtualConnection virCon, int dimension) {
        virCon.enqueueSendImportant(UpdateDimensionCommand.newBuilder()
                .setDimension(dimension)
                .build()
        );
    }

    public static void joinWorldCommand(@NonNull VirtualConnection virCon) {
        virCon.enqueueSendImportant(JoinWorldCommand.newBuilder()
                .build()
        );
    }

    public static void spawnEntity(@NonNull VirtualConnection virCon,
                                   int entityType, int entityId, @NonNull Map<String, String> entityData) {
        virCon.enqueueSendImportant(SpawnEntity.newBuilder()
                .setEntityType(entityType)
                .setEntityId(entityId)
                .putAllEntityData(entityData)
                .build()
        );
    }

    public static void despawnEntity(@NonNull VirtualConnection virCon, int entityId) {
        virCon.enqueueSendImportant(DespawnEntity.newBuilder()
                .setEntityId(entityId)
                .build()
        );
    }

}

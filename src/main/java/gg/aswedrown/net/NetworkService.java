package gg.aswedrown.net;

import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.NonNull;

import java.util.Map;

public class NetworkService {

    private NetworkService() {}

    public static void handshakeResponse(@NonNull VirtualConnection virCon) {
        virCon.sendImportantPacket(HandshakeResponse.newBuilder()
                .setProtocolVersion(PacketManager.PROTOCOL_VERSION)
                .build()
        );
    }

    public static void createLobbyResponse(@NonNull VirtualConnection virCon,
                                           @NonNull LobbyManager.CreationResult result) {
        virCon.sendImportantPacket(CreateLobbyResponse.newBuilder()
                .setLobbyId(result.getLobbyId())
                .setPlayerId(result.getPlayerId())
                .build()
        );
    }

    public static void joinLobbyResponse(@NonNull VirtualConnection virCon,
                                         @NonNull LobbyManager.JoinResult result) {
        virCon.sendImportantPacket(JoinLobbyResponse.newBuilder()
                .setPlayerId(result.getPlayerId())
                .putAllOtherPlayers(result.getMembers())
                .build()
        );
    }

    public static void leaveLobbyResponse(@NonNull VirtualConnection virCon, int result) {
        virCon.sendImportantPacket(LeaveLobbyResponse.newBuilder()
                .setStatusCode(result)
                .build()
        );
    }

    public static void ping(@NonNull VirtualConnection virCon, int testId, int lastLatency) {
        // Для каждого клиента устанавливаем "своё" время (currentTimeMillis), т.к.
        // отправка пакета может занять некоторое время, что визуально сделает пинг
        // клиентов зависимым от того, в каком порядке им был отправлен пакет Ping.
        virCon.sendPacket(Ping.newBuilder()
                .setTestId(testId)
                .setLastLatency(lastLatency)
                .build()
        );
    }

    public static void updatedMembersList(@NonNull VirtualConnection virCon,
                                          @NonNull Map<Integer, String> members) {
        virCon.sendImportantPacket(UpdatedMembersList.newBuilder()
                .putAllMembers(members)
                .build()
        );
    }

    public static void kickedFromLobby(@NonNull VirtualConnection virCon, int reason) {
        virCon.sendImportantPacket(KickedFromLobby.newBuilder()
                .setReason(reason)
                .build()
        );
    }

}

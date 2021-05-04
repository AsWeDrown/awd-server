package gg.aswedrown.server;

import gg.aswedrown.net.*;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.vircon.VirtualConnection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class NetworkService {

    public void handshakeResponse(@NonNull VirtualConnection virCon) {
        virCon.sendImportantPacket(HandshakeResponse.newBuilder()
                .setProtocolVersion(PacketManager.PROTOCOL_VERSION)
                .build()
        );
    }

    public void createLobbyResponse(@NonNull VirtualConnection virCon,
                                    @NonNull LobbyManager.CreationResult result) {
        virCon.sendImportantPacket(CreateLobbyResponse.newBuilder()
                .setLobbyId(result.getLobbyId())
                .setPlayerId(result.getPlayerId())
                .build()
        );
    }

    public void joinLobbyResponse(@NonNull VirtualConnection virCon,
                                  @NonNull LobbyManager.JoinResult result) {
        virCon.sendImportantPacket(JoinLobbyResponse.newBuilder()
                .setPlayerId(result.getPlayerId())
                .putAllOtherPlayers(result.getMembers())
                .build()
        );
    }

    public void leaveLobbyResponse(@NonNull VirtualConnection virCon, int result) {
        virCon.sendImportantPacket(LeaveLobbyResponse.newBuilder()
                .setStatusCode(result)
                .build()
        );
    }

    public void ping(@NonNull VirtualConnection virCon) {
        // Для каждого клиента устанавливаем "своё" время (currentTimeMillis), т.к.
        // отправка пакета может занять некоторое время, что визуально сделает пинг
        // клиентов зависимым от того, в каком порядке им был отправлен пакет Ping.
        virCon.sendPacket(Ping.newBuilder()
                .setServerTime(System.currentTimeMillis())
                .build()
        );
    }

    public void updatedMembersList(@NonNull VirtualConnection virCon,
                                   @NonNull Map<Integer, String> members) {
        virCon.sendImportantPacket(UpdatedMembersList.newBuilder()
                .putAllMembers(members)
                .build()
        );
    }

    public void kickedFromLobby(@NonNull VirtualConnection virCon, int reason) {
        virCon.sendImportantPacket(KickedFromLobby.newBuilder()
                .setReason(reason)
                .build()
        );
    }

}

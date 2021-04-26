package gg.aswedrown.server.listener;

import gg.aswedrown.net.CreateLobbyRequest;
import gg.aswedrown.net.CreateLobbyResponse;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.lobby.LobbyManager;

import java.net.InetAddress;

public class CreateLobbyRequestListener extends PacketListener<CreateLobbyRequest> {

    public CreateLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(InetAddress senderAddr, CreateLobbyRequest packet) throws Exception {
        LobbyManager.CreationResult result = srv.getLobbyManager()
                .createNewLobby(senderAddr, packet.getPlayerName());

        sendResponse(senderAddr, result);
    }

    private void sendResponse(InetAddress targetAddr, LobbyManager.CreationResult result) {
        srv.getPacketManager().sendPacket(targetAddr,
                CreateLobbyResponse.newBuilder()
                        .setLobbyId(result.getLobbyId())
                        .setPlayerId(result.getPlayerId())
                        .build()
        );
    }

}

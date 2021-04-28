package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.JoinLobbyRequest;
import gg.aswedrown.net.JoinLobbyResponse;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.lobby.LobbyManager;

import java.net.InetAddress;

@RegisterPacketListener (PacketWrapper.PacketCase.JOINLOBBYREQUEST)
public class JoinLobbyRequestListener extends PacketListener<JoinLobbyRequest> {

    public JoinLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(InetAddress senderAddr, JoinLobbyRequest packet) throws Exception {
        LobbyManager.JoinResult result = srv.getLobbyManager()
                .joinToLobby(senderAddr.getHostAddress(), packet.getLobbyId(), packet.getPlayerName());

        sendResponse(senderAddr, result);
    }

    private void sendResponse(InetAddress targetAddr, LobbyManager.JoinResult result) {
        srv.getPacketManager().sendPacket(targetAddr,
                JoinLobbyResponse.newBuilder()
                        .setPlayerId(result.getPlayerId())
                        .putAllOtherPlayers(result.getMembers())
                        .build()
        );
    }

}

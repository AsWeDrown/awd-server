package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.LeaveLobbyRequest;
import gg.aswedrown.net.LeaveLobbyResponse;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;

import java.net.InetAddress;

@RegisterPacketListener (PacketWrapper.PacketCase.LEAVELOBBYREQUEST)
public class LeaveLobbyRequestListener extends PacketListener<LeaveLobbyRequest> {

    public LeaveLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(InetAddress senderAddr, LeaveLobbyRequest packet) throws Exception {
        int result = srv.getLobbyManager()
                .leaveFromLobby(senderAddr.getHostAddress(), packet.getLobbyId(), packet.getPlayerId());

        sendResponse(senderAddr, result);
    }

    private void sendResponse(InetAddress targetAddr, int result) {
        srv.getPacketManager().sendPacket(targetAddr,
                LeaveLobbyResponse.newBuilder()
                        .setStatusCode(result)
                        .build()
        );
    }

}

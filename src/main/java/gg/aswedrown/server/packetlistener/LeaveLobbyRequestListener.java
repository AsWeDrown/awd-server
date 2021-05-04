package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.LeaveLobbyRequest;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.LEAVELOBBYREQUEST)
public class LeaveLobbyRequestListener extends PacketListener<LeaveLobbyRequest> {

    public LeaveLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, LeaveLobbyRequest packet) throws Exception {
        int result = srv.getLobbyManager()
                .leaveFromLobby(virCon, packet.getLobbyId(), packet.getPlayerId());

        srv.getNetService().leaveLobbyResponse(virCon, result);
    }

}

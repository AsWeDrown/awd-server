package gg.aswedrown.server.packetlistener.lobby;

import gg.aswedrown.net.LeaveLobbyRequest;
import gg.aswedrown.net.NetworkService;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.LEAVE_LOBBY_REQUEST)
public class LeaveLobbyRequestListener extends PacketListener<LeaveLobbyRequest> {

    public LeaveLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, LeaveLobbyRequest packet) throws Exception {
        int result = srv.getLobbyManager().leaveFromLobby(virCon);
        NetworkService.leaveLobbyResponse(virCon, result);
    }

}

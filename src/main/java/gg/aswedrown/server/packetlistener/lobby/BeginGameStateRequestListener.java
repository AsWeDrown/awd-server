package gg.aswedrown.server.packetlistener.lobby;

import gg.aswedrown.net.BeginPlayStateRequest;
import gg.aswedrown.net.NetworkService;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.BEGIN_PLAY_STATE_REQUEST)
public class BeginGameStateRequestListener extends PacketListener<BeginPlayStateRequest> {

    public BeginGameStateRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, BeginPlayStateRequest packet) throws Exception {
        int result = srv.getLobbyManager().beginPlayState(virCon, packet.getSaveId());
        NetworkService.beginPlayStateResponse(virCon, result);
    }

}

package gg.aswedrown.server.packetlistener.play;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.UpdateDimensionComplete;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.UPDATE_DIMENSION_COMPLETE)
public class UpdateDimensionCompleteListener extends PacketListener<UpdateDimensionComplete> {

    public UpdateDimensionCompleteListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UpdateDimensionComplete packet) throws Exception {
        srv.getLobbyManager().updateDimensionComplete(virCon);
    }

}

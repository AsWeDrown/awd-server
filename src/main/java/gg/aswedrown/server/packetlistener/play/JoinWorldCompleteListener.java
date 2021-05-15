package gg.aswedrown.server.packetlistener.play;

import gg.aswedrown.net.JoinWorldComplete;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.JOIN_WORLD_COMPLETE)
public class JoinWorldCompleteListener extends PacketListener<JoinWorldComplete> {

    public JoinWorldCompleteListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, JoinWorldComplete packet) throws Exception {
        srv.getLobbyManager().joinWorldComplete(virCon);
    }

}

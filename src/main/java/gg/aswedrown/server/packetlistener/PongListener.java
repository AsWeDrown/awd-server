package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.Pong;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.vircon.VirtualConnection;

@RegisterPacketListener (value = PacketWrapper.PacketCase.PONG)
public class PongListener extends PacketListener<Pong> {

    public PongListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, Pong packet) throws Exception {
        long latency = System.currentTimeMillis() - packet.getClientTime();
        srv.getVirConManager().pongReceived(virCon, latency);
    }

}

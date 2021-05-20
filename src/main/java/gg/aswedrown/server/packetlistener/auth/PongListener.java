package gg.aswedrown.server.packetlistener.auth;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.Pong;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.PONG)
public class PongListener extends PacketListener<Pong> {

    public PongListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                 Pong packet) throws Exception {
        virCon.pongReceived(packet.getTestId());
    }

}

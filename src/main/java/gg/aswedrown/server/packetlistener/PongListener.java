package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.Pong;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.PONG)
public class PongListener extends PacketListener<Pong> {

    public PongListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, Pong packet) throws Exception {
        virCon.pongReceived(packet.getTestId());

        System.out.println("** TEMP DEBUG ** Pong received: " + virCon.getAddr().getHostAddress());
    }

}

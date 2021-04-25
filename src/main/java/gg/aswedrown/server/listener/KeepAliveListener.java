package gg.aswedrown.server.listener;

import gg.aswedrown.net.KeepAlive;
import gg.aswedrown.server.AwdServer;

import java.net.InetAddress;

public class KeepAliveListener extends PacketListener<KeepAlive> {

    public KeepAliveListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(InetAddress senderAddr, KeepAlive packet) throws Exception {
        System.out.println("Received KeepAlive from " + senderAddr.getHostAddress() + ":");
        System.out.println(packet.toString());
        System.out.println("(playerId = " + packet.getPlayerId() + ", testId = " + packet.getTestId() + ")");
        srv.getPacketManager().sendPacket(senderAddr, packet);
    }

}

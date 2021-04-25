package gg.aswedrown.server.listener;

import gg.aswedrown.net.HandshakeRequest;
import gg.aswedrown.server.AwdServer;

import java.net.InetAddress;

public class HandshakeRequestListener extends PacketListener<HandshakeRequest> {

    public HandshakeRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(InetAddress senderAddr, HandshakeRequest packet) throws Exception {
        System.out.println("Received HandshakeRequest from " + senderAddr.getHostAddress() + ":");
        System.out.println("  protocolVersion = " + packet.getProtocolVersion());
        System.out.println("Responding... sent successfully:");
        System.out.println(srv.getPacketManager().sendPacket(senderAddr,
                HandshakeRequest.newBuilder()
                        .setProtocolVersion(PacketManager.PROTOCOL_VERSION)
                        .build()
                )
        );
    }

}

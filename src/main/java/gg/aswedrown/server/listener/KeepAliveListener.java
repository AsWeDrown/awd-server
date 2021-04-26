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
        // TODO: 26.04.2021
    }

}

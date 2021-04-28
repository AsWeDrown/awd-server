package gg.aswedrown.server.packetlistener;

import com.google.protobuf.Message;
import gg.aswedrown.server.AwdServer;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@RequiredArgsConstructor
public abstract class PacketListener<WiredPacketType extends Message> {

    protected final AwdServer srv;

    final void packetReceived(InetAddress senderAddr, Message packet) throws Exception {
        processPacket(senderAddr, (WiredPacketType) packet);
    }

    protected abstract void processPacket(InetAddress senderAddr, WiredPacketType packet) throws Exception;

}

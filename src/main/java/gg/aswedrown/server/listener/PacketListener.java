package gg.aswedrown.server.listener;

import com.google.protobuf.Message;
import gg.aswedrown.server.AwdServer;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@RequiredArgsConstructor
public abstract class PacketListener<BoundPacketType extends Message> {

    protected final AwdServer srv;

    final void packetReceived(InetAddress senderAddr, Message packet) throws Exception {
        processPacket(senderAddr, (BoundPacketType) packet);
    }

    protected abstract void processPacket(InetAddress senderAddr, BoundPacketType packet) throws Exception;

}

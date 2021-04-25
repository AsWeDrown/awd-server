package gg.aswedrown.server.listener;

import com.google.protobuf.Message;
import gg.aswedrown.server.AwdServer;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@RequiredArgsConstructor
public abstract class PacketListener<T extends Message> {

    protected final AwdServer srv;

    abstract void packetReceived(InetAddress senderAddr, Message packet) throws Exception;

    protected abstract void processPacket(InetAddress senderAddr, T packet) throws Exception;

}

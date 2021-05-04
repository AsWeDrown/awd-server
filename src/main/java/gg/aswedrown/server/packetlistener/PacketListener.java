package gg.aswedrown.server.packetlistener;

import com.google.protobuf.Message;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.vircon.VirtualConnection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PacketListener<WiredPacketType extends Message> {

    protected final AwdServer srv;

    public final void packetReceived(VirtualConnection virCon, Message packet) throws Exception {
        processPacket(virCon, (WiredPacketType) packet);
    }

    protected abstract void processPacket(VirtualConnection virCon, WiredPacketType packet) throws Exception;

}

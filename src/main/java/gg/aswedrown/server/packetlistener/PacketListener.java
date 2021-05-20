package gg.aswedrown.server.packetlistener;

import com.google.protobuf.Message;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PacketListener<WiredPacketType extends Message> {

    protected final AwdServer srv;

    public final void packetReceived(VirtualConnection virCon,
                                     UnwrappedPacketData packetData) throws Exception {
        processPacket(virCon, packetData, (WiredPacketType) packetData.getPacket());
    }

    protected abstract void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                          WiredPacketType packet) throws Exception;

}

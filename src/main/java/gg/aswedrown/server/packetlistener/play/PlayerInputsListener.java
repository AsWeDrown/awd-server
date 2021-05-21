package gg.aswedrown.server.packetlistener.play;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.net.UpdatePlayerInputs;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.UPDATE_PLAYER_INPUTS)
public class PlayerInputsListener extends PacketListener<UpdatePlayerInputs> {

    public PlayerInputsListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                 UpdatePlayerInputs packet) throws Exception {
        srv.getLobbyManager().enqueuePlayerInputs(
                virCon, packetData.getSequence(), packet.getInputsBitfield());
    }

}

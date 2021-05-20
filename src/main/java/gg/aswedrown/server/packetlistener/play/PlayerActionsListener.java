package gg.aswedrown.server.packetlistener.play;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.PlayerActions;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.PLAYER_ACTIONS)
public class PlayerActionsListener extends PacketListener<PlayerActions> {

    public PlayerActionsListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                 PlayerActions packet) throws Exception {
        if (packet.getActionsBitfield() != 0)
            srv.getLobbyManager().enqueuePlayerActions(
                    virCon, packetData.getSequence(), packet.getActionsBitfield());
    }

}

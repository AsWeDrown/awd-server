package gg.aswedrown.server.packetlistener.play;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.PlayerTileInteract;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.PLAYER_TILE_INTERACT)
public class PlayerTileInteractListener extends PacketListener<PlayerTileInteract> {

    public PlayerTileInteractListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                 PlayerTileInteract packet) throws Exception {
        srv.getLobbyManager().handlePlayerTileInteract(
                virCon, packet.getX(), packet.getY(), packet.getCommand());
    }

}

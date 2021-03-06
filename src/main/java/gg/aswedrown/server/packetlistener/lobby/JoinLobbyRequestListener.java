package gg.aswedrown.server.packetlistener.lobby;

import gg.aswedrown.net.JoinLobbyRequest;
import gg.aswedrown.net.NetworkService;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.net.UnwrappedPacketData;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.JOIN_LOBBY_REQUEST)
public class JoinLobbyRequestListener extends PacketListener<JoinLobbyRequest> {

    public JoinLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                 JoinLobbyRequest packet) throws Exception {
        LobbyManager.JoinResult result = srv.getLobbyManager()
                .joinToLobby(virCon, packet.getLobbyId(), packet.getPlayerName());

        NetworkService.joinLobbyResponse(virCon, result);
    }

}

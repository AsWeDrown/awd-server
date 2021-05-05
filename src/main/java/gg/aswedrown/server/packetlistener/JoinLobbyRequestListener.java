package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.JoinLobbyRequest;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.JOIN_LOBBY_REQUEST)
public class JoinLobbyRequestListener extends PacketListener<JoinLobbyRequest> {

    public JoinLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, JoinLobbyRequest packet) throws Exception {
        LobbyManager.JoinResult result = srv.getLobbyManager()
                .joinToLobby(virCon, packet.getLobbyId(), packet.getPlayerName());

        srv.getNetService().joinLobbyResponse(virCon, result);
    }

}

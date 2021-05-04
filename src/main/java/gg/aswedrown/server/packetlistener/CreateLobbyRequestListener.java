package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.CreateLobbyRequest;
import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.vircon.VirtualConnection;

@RegisterPacketListener (PacketWrapper.PacketCase.CREATELOBBYREQUEST)
public class CreateLobbyRequestListener extends PacketListener<CreateLobbyRequest> {

    public CreateLobbyRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, CreateLobbyRequest packet) throws Exception {
        LobbyManager.CreationResult result = srv.getLobbyManager()
                .createNewLobby(virCon, packet.getPlayerName());

        srv.getNetService().createLobbyResponse(virCon, result);
    }

}

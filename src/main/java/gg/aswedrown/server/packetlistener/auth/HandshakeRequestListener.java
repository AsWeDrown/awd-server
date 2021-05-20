package gg.aswedrown.server.packetlistener.auth;

import gg.aswedrown.net.*;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.packetlistener.PacketListener;
import gg.aswedrown.server.packetlistener.RegisterPacketListener;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RegisterPacketListener (PacketWrapper.PacketCase.HANDSHAKE_REQUEST)
public class HandshakeRequestListener extends PacketListener<HandshakeRequest> {

    public HandshakeRequestListener(AwdServer srv) {
        super(srv);
    }

    @Override
    protected void processPacket(VirtualConnection virCon, UnwrappedPacketData packetData,
                                 HandshakeRequest packet) throws Exception {
        log.info("Protocol version of {} is {}.",
                virCon.getAddr().getHostAddress(), packet.getProtocolVersion());

        if (packet.getProtocolVersion() == PacketManager.PROTOCOL_VERSION) {
            // Всё хорошо, регистрируем "соединение".
            log.info("Virtual connection authorized: {}.", virCon.getAddr().getHostAddress());
            virCon.setAuthorized(true);
        } else if (virCon.isAuthorized()) {
            // Клиент "переподключился" с другой версией протокола. Запрещаем дальнейшую коммуникацию.
            log.warn("Virtual connection no longer authorized: {}.", virCon.getAddr().getHostAddress());
            virCon.setAuthorized(false);
        }

        // Возвращаем клиенту версию протокола на сервере.
        NetworkService.handshakeResponse(virCon);
    }

}

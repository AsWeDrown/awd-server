package gg.aswedrown.server.listener;

import gg.aswedrown.net.HandshakeRequest;
import gg.aswedrown.net.HandshakeResponse;
import gg.aswedrown.server.AwdServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

@Slf4j
public class HandshakeRequestListener extends PacketListener<HandshakeRequest> {

    private final HandshakeResponse commonResponse;

    public HandshakeRequestListener(AwdServer srv) {
        super(srv);

        // Один и тот же ответ для всех клиентов.
        commonResponse = HandshakeResponse
                .newBuilder()
                .setProtocolVersion(PacketManager.PROTOCOL_VERSION)
                .build();
    }

    @Override
    protected void processPacket(InetAddress senderAddr, HandshakeRequest packet) throws Exception {
        String addrStr = senderAddr.getHostAddress();

        log.debug("Protocol version of {} is {}.", addrStr, packet.getProtocolVersion());

        if (packet.getProtocolVersion() == PacketManager.PROTOCOL_VERSION)
            // Всё хорошо, регистрируем новое "соединение".
            srv.handleNewConnection(senderAddr, addrStr);

        // Возвращаем клиенту версию протокола на сервере.
        sendResponse(senderAddr);
    }

    protected void sendResponse(InetAddress targetAddr) {
        srv.getPacketManager().sendPacket(targetAddr, commonResponse);
    }

}

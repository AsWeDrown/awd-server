package gg.aswedrown.server;

import gg.aswedrown.net.Ping;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Set;
import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class Pinger extends TimerTask {

    @NonNull
    private final AwdServer srv;

    @Override
    public void run() {
        Set<InetAddress> virConns = srv.getVirConManager().getOpenVirtualConnections();

        if (!virConns.isEmpty()) {
            log.info("Pinging {} virtually connected clients.", virConns.size());

            for (InetAddress addr : virConns) {
                // Для каждого клиента устанавливаем "своё" время (currentTimeMillis), т.к.
                // отправка пакета может занять некоторое время, что визуально сделает пинг
                // клиентов зависимым от того, в каком порядке им был отправлен пакет Ping.
                Ping pingPacket = Ping.newBuilder().setServerTime(System.currentTimeMillis()).build();
                srv.getPacketManager().sendPacket(addr, pingPacket);
            }
        }
    }

}

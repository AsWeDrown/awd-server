package gg.aswedrown.server.command;

import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RegisterConsoleCommand (
        name = "stats",
        usage = "stats [ip-address]",
        desc = "Display various general/connection-specific statistics.",
        minArgsLen = 0
)
public class ConsoleCommandStats extends ConsoleCommand {

    private static final String HEADER = "---------------------------- Statistics ----------------------------";
    private static final String FOOTER = "--------------------------------------------------------------------";

    public ConsoleCommandStats(AwdServer srv, String name, String usage, String desc, int minArgsLen) {
        super(srv, name, usage, desc, minArgsLen);
    }

    @Override
    protected void execute(String[] args) throws Exception {
        if (args.length == 0) {
            // Общая статистика ("средняя по больнице").
            int activeConns = srv.getVirConManager().getActiveVirtualConnections();
            int authdConns  = srv.getVirConManager().getAuthorizedVirtualConnections();

            log.info(HEADER);
            log.info("  Active connections: {}", activeConns);
            log.info("  Authorized connections: {} ({}%)", authdConns,
                    String.format("%.2f", 100.0 * authdConns / activeConns));
            log.info("  Average latency: {} ms", srv.getVirConManager().getAverageLatency());
            log.info(FOOTER);
        } else {
            // Статистика об одном конкретном соединении с указанным IP-адресом.
            String addrStr = args[0];
            InetAddress addr;

            try {
                addr = InetAddress.getByName(addrStr);
            } catch (UnknownHostException ex) {
                log.warn("No active virtual connections with the specified address (unknown host).");
                return;
            }

            VirtualConnection virCon = srv.getVirConManager().strictGetVirtualConnection(addr);

            if (virCon == null) {
                log.warn("No active virtual connections with the specified address (not listed).");
                return;
            }

            log.info(HEADER);
            log.info("  IP address: {}", virCon.getAddr().getHostAddress());
            log.info("  Authorized: {}", virCon.isAuthorized());
            log.info("  Latency: {} ms", virCon.getPongLatency());
            log.info("  Packet loss: {}%", String.format("%.2f", virCon.getPacketLossPercent()));
            log.info("  Connection quality: {}", virCon.isConnectionBad() ? "BAD" : "GOOD");
            log.info(FOOTER);
        }
    }

}

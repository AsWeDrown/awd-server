package gg.aswedrown.server.command;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RegisterConsoleCommand (
        name = "stats",
        usage = "stats [U<ip-address>] | [L<lobby-id>]",
        desc = "Display various general, user-, or lobby-specific statistics.",
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
            int   activeConns   = srv.getVirConManager().getActiveVirtualConnections();
            int   authdConns    = srv.getVirConManager().getAuthorizedVirtualConnections();
            float rootServerTps = srv.getGameServer().getTpsMeter().estimateTps();
            float avgGameLobTps = srv.getGameServer().getAverageGameLobbiesTps();

            log.info(HEADER);
            log.info("  Active connections: {}", activeConns);
            log.info("  Authorized connections: {} ({}%)", authdConns,
                    String.format("%.2f", 100.0 * authdConns / activeConns));
            log.info("  Average connections' RTT: {} ms", srv.getVirConManager().getAverageRtt());
            log.info("  ");
            log.info("  Active game lobbies: {}", srv.getGameServer().getActiveGameLobbies());
            log.info("  Root game server TPS: {}",
                    rootServerTps < 0.0f ? (-rootServerTps + " (approx.)") : rootServerTps);
            log.info("  Average game lobbies' TPS: {}",
                    avgGameLobTps < 0.0f ? (-avgGameLobTps + " (approx.)") : avgGameLobTps);
            log.info("  Total players playing: {}", srv.getGameServer().getTotalPlayersPlaying());
            log.info(FOOTER);
        } else {
            String param = args[0];

            if (param.length() > 1) {
                char targetType = Character.toLowerCase(param.charAt(0));
                String target = param.substring(1);

                switch (targetType) {
                    case 'U':
                        // Статистика об одном конкретном пользователе с указанным IP-адресом.
                        InetAddress addr;

                        try {
                            addr = InetAddress.getByName(target);
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
                        log.info("  RTT: {} ms", virCon.getLastRtt());
                        log.info("  Packet loss: {}%", String.format("%.2f", virCon.getPacketLossPercent()));
                        log.info("  Connection quality: {}", virCon.isConnectionBad() ? "BAD" : "GOOD");
                        log.info(FOOTER);

                        break;

                    case 'L':
                        // Статистика об одной конкретной комнате с указанным идентификатором.
                        int lobbyId;

                        try {
                            lobbyId = Integer.parseInt(target);
                        } catch (NumberFormatException ex) {
                            log.warn("Invalid lobby ID (expected integer).");
                            return;
                        }

                        ActiveGameLobby lobby = srv.getGameServer().getActiveGameLobby(lobbyId);

                        if (lobby == null) {
                            log.warn("No active game lobbies with the specified ID (not listed).");
                            return;
                        }

                        float tps = lobby.getTpsMeter().estimateTps();

                        log.info(HEADER);
                        log.info("  Lobby ID: {}", lobby.getLobbyId());
                        log.info("  Players: {} ({} ready, {} joined world)",
                                lobby.getPlayers(), lobby.getReadyPlayers(), lobby.getJoinedWorldPlayers());
                        log.info("  TPS: {}", tps < 0.0f ? (-tps + " (approx.)") : tps);
                        log.info("  Loaded dimensions: {}", lobby.getDimensions());
                        log.info(FOOTER);

                        break;

                    default:
                        log.warn("Unrecognized target type: '{}' - expected " +
                                "'U' for user- or 'L' for lobby-specific statistics", targetType);

                        printUsage();

                        break;
                }
            } else
                printUsage();
        }
    }

}

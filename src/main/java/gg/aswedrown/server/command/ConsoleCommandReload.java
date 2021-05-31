package gg.aswedrown.server.command;

import gg.aswedrown.config.PhysicsConfig;
import gg.aswedrown.server.AwdServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RegisterConsoleCommand (
        name = "reload",
        usage = "reload <config name>",
        desc = "Reload the specified configuration, if possible.",
        minArgsLen = 1
)
public class ConsoleCommandReload extends ConsoleCommand {

    public ConsoleCommandReload(AwdServer srv, String name, String usage, String desc, int minArgsLen) {
        super(srv, name, usage, desc, minArgsLen);
    }

    @Override
    protected void execute(String[] args) throws Exception {
        String confName = args[0].toLowerCase();

        switch (confName) {
            case "physics":
                AwdServer.getServer().setPhysics(PhysicsConfig.loadOrDefault());
                log.info("Reloading physics configuration successfully.");

                break;

            default:
                log.error("The specified configuration does not " +
                        "exist or is not reloadable. Consider restarting the server.");

                break;
        }
    }

}

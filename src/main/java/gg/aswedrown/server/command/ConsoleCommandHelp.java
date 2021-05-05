package gg.aswedrown.server.command;

import gg.aswedrown.server.AwdServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RegisterConsoleCommand (
        name = "help",
        usage = "help [command]",
        desc = "Print a list of all available commands or a command-specific help.",
        minArgsLen = 0
)
public class ConsoleCommandHelp extends ConsoleCommand {

    public ConsoleCommandHelp(AwdServer srv, String name, String usage, String desc, int minArgsLen) {
        super(srv, name, usage, desc, minArgsLen);
    }

    @Override
    protected void execute(String[] args) throws Exception {
        if (args.length == 0) {
            log.info("Available commands:");

            for (ConsoleCommand cmd : srv.getConsoleCmdsDisp().getCommands().values())
                log.info("    {} - {}", cmd.getName(), cmd.getDesc());
        } else {
            String label = args[0].toLowerCase();
            ConsoleCommand targetCmd = srv.getConsoleCmdsDisp().getCommands().get(label);

            if (targetCmd == null)
                log.warn(ConsoleCommandDispatcher.UNKNOWN_COMMAND_MESSAGE);
            else
                targetCmd.printUsage();
        }
    }

}

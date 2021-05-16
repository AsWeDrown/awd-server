package gg.aswedrown.server.command;

import gg.aswedrown.server.AwdServer;

@RegisterConsoleCommand (
        name = "stop",
        usage = "stop",
        desc = "Shut the server down gracefully.",
        minArgsLen = 0
)
public class ConsoleCommandStop extends ConsoleCommand {

    public ConsoleCommandStop(AwdServer srv, String name, String usage, String desc, int minArgsLen) {
        super(srv, name, usage, desc, minArgsLen);
    }

    @Override
    protected void execute(String[] args) throws Exception {
        srv.shutdown();
    }

}

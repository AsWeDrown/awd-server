package gg.aswedrown.server.command;

import gg.aswedrown.server.AwdServer;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class ConsoleCommand {

    protected final AwdServer srv;

    @Getter
    private final String name, usage, desc;

    private final int minArgsLen;

    public void process(@NonNull String[] args) {
        if (args.length < minArgsLen) {
            printUsage();
            return;
        }

        try {
            execute(args);
        } catch (Exception ex) {
            throw new RuntimeException("unhandled exception " +
                    "occurred while attempting to execute command " + name, ex);
        }
    }

    protected abstract void execute(String[] args) throws Exception;

    public void printUsage() {
        log.warn("Usage: {}", usage);
    }

}
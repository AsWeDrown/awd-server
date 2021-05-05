package gg.aswedrown.server.command;

import gg.aswedrown.server.AwdServer;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ConsoleCommandDispatcher extends Thread {

    public static final String UNKNOWN_COMMAND_MESSAGE = "Unknown command. " +
            "Use \"help\" to print a list of all available commands.";

    private final AwdServer srv;

    @Getter
    private final Map<String, ConsoleCommand> commands = new HashMap<>();

    public ConsoleCommandDispatcher(@NonNull AwdServer srv) {
        super("Console Command Dispatcher Thread");

        this.srv = srv;
        wireConsoleCommands();
    }

    /**
     * Автоматически регистрирует все обработчики команд из пакета/папки
     *
     *     gg.aswedrown.server.command
     *
     * (при условии, что они "помечены" аннотацией @RegisterConsoleCommand).
     */
    private void wireConsoleCommands() {
        Reflections reflections = new Reflections("gg.aswedrown.server.command");

        for (Class<?> clazz : reflections.getTypesAnnotatedWith(RegisterConsoleCommand.class)) {
            try {
                // Валидация.
                if (!ConsoleCommand.class.isAssignableFrom(clazz))
                    throw new ClassCastException(
                            "class " + clazz.getName() + " is annotated with @RegisterConsoleCommand, " +
                                    "but does not inherit from " + ConsoleCommand.class.getName());

                // Инициализация.
                RegisterConsoleCommand anno = clazz.getAnnotation(RegisterConsoleCommand.class);
                String cmdName = anno.name().trim().toLowerCase();
                Constructor<ConsoleCommand> constructor
                        = (Constructor<ConsoleCommand>) clazz.getDeclaredConstructor(
                                AwdServer.class, String.class, String.class, String.class, int.class);
                ConsoleCommand command = constructor.newInstance(
                        srv, cmdName, anno.usage(), anno.desc(), anno.minArgsLen());

                // Регистрация.
                ConsoleCommand otherCommand = commands.get(cmdName);

                if (otherCommand != null)
                    throw new IllegalStateException(
                            "multiple classes attempt to listen for console command with name " + cmdName
                                    + ": " + otherCommand.getClass().getName() + " and " + clazz.getName());

                log.info("Wired console command {} to {}.", cmdName, clazz.getSimpleName());
                commands.put(cmdName, command);
            } catch (Exception ex) {
                log.error("Ignoring misconfigured console command: {}.", ex.toString());
            }
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    String label = line.split(" ")[0].toLowerCase();
                    String[] args = line.replace(label + " ", "").split(" ");

                    if ((args.length == 1) && (args[0].equals(line)))
                        // напр., "flushcaches" --> (len=1)["flushcaches"]
                        args = new String[0];

                    ConsoleCommand targetCmd = commands.get(label);

                    if (targetCmd != null) {
                        try {
                            targetCmd.process(args);
                        } catch (Exception ex) {
                            log.error("Error processing your command. Stacktrace:", ex);
                        }
                    } else
                        log.warn(UNKNOWN_COMMAND_MESSAGE);
                }
            }
        } catch (Exception ex) {
            log.error("Unhandled exception in console command dispatcher thread.");
            log.error("Console commands will not work until restart. Stacktrace:", ex);
        }
    }

}
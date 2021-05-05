package gg.aswedrown.server;

import gg.aswedrown.net.PacketManager;
import gg.aswedrown.server.command.ConsoleCommandDispatcher;
import gg.aswedrown.server.data.DbInfo;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.data.lobby.MongoLobbyRepository;
import gg.aswedrown.server.udp.AwdUdpServer;
import gg.aswedrown.server.udp.UdpServer;
import gg.aswedrown.server.vircon.Pinger;
import gg.aswedrown.server.vircon.VirtualConnectionManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class AwdServer {

    private AwdServer() {}

    @Getter
    private static final AwdServer server = new AwdServer();

    private long startupBeginTime;

    @Getter
    private AwdServerConfig config;

    @Getter
    private ExecutorService executor;

    @Getter
    private ConsoleCommandDispatcher consoleCmdsDisp;

    @Getter
    private MongoManager db;

    @Getter
    private LobbyManager lobbyManager;

    @Getter
    private VirtualConnectionManager virConManager;

    @Getter
    private PacketManager packetManager;

    @Getter
    private Pinger pinger;

    @Getter
    private UdpServer udpServer;

    void bootstrap() throws Exception {
        if (startupBeginTime != 0)
            throw new IllegalStateException("cannot bootstrap twice");

        startupBeginTime = System.currentTimeMillis();

        loadConfig();

        setupShutdownHook();
        setupExecutor();
        setupDatabase();
        setupConsoleCommands();
        setupVirtualConnectivity();

        double startupTookSeconds = (System.currentTimeMillis() - startupBeginTime) / 1000.0D;
        log.info("Startup done! Everything took {} s.", String.format("%.2f", startupTookSeconds));

        // Запускаем в основном потоке в последнюю очередь. Тут ждать уже нечего.
        startUdpSocketServer();
    }

    private void loadConfig() throws IOException {
        File configFile = new File(AwdServerConfig.FILE_NAME);

        if (!configFile.exists()) {
            log.warn("Missing config file {} in the working directory. " +
                            "A default configuration file will be copied from the server JAR.",
                    AwdServerConfig.FILE_NAME);

            try (InputStream in = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(AwdServerConfig.FILE_NAME)) {
                if (in == null)
                    throw new NoSuchFileException(
                            "config file not bundled in the JAR: " + AwdServerConfig.FILE_NAME);

                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        try (Reader reader = new InputStreamReader(
                new FileInputStream("awd-server-config.yml"), StandardCharsets.UTF_8)) {
            config = new Yaml().loadAs(reader, AwdServerConfig.class);
        }

        if (config.getSchemaVersion() != AwdServerConfig.SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "incompatible configuration file, aborting startup; file schema: "
                            + config.getSchemaVersion() + ", server schema: " + AwdServerConfig.SCHEMA_VERSION);
    }

    private void setupExecutor() {
        ArrayBlockingQueue<Runnable> executorQueue
                = new ArrayBlockingQueue<>(config.getExecutorQueueCapacity());

        executor = new ThreadPoolExecutor(
                10,
                config.getExecutorMaxThreads(),
                config.getExecutorThreadsKeepAliveSeconds(),
                TimeUnit.SECONDS,
                executorQueue,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private void setupDatabase() {
        db = new MongoManager().noLogs().connectLocal().select(DbInfo.DATABASE_NAME);

        // TODO: 26.04.2021 оптимизировать работу с БД
        //  (сейчас код удобный и простой, но с производительностью не очень)
        lobbyManager = new LobbyManager(this, new MongoLobbyRepository(db));
    }

    private void setupConsoleCommands() {
        consoleCmdsDisp = new ConsoleCommandDispatcher(this);
        consoleCmdsDisp.start();
    }

    private void setupVirtualConnectivity() {
        virConManager = new VirtualConnectionManager(this);
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(this::onShutdown, "AwdServer-Shutdown-Hook"));
    }

    private void onShutdown() {
        log.info("Shutting down...");

        db.close();
        udpServer.stop();

        log.info("Bye!");
    }

    private void startUdpSocketServer() throws SocketException {
        // Менеджер и обработчики пакетов.
        packetManager = new PacketManager(this);

        // Пингер (нет, блин, Понгер).
        new Timer().schedule(pinger = new Pinger(this),
                config.getPingPeriodMillis(), config.getPingPeriodMillis());

        // Сам UDP "сервер".
        udpServer = new AwdUdpServer(
                config.getUdpServerPort(),
                config.getUdpServerBufferSize(),
                executor,
                virConManager,
                packetManager
        );

        // Запускает в ТЕКУЩЕМ ПОТОКЕ (блокирующе!)!
        udpServer.start();
    }

}

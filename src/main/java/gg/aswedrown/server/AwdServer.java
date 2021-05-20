package gg.aswedrown.server;

import gg.aswedrown.config.AwdServerConfig;
import gg.aswedrown.config.PhysicsConfig;
import gg.aswedrown.game.GameServer;
import gg.aswedrown.net.PacketManager;
import gg.aswedrown.server.command.ConsoleCommandDispatcher;
import gg.aswedrown.server.data.DbInfo;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.data.lobby.MongoLobbyRepository;
import gg.aswedrown.server.udp.AwdUdpServer;
import gg.aswedrown.server.udp.UdpServer;
import gg.aswedrown.server.vircon.VirtualConnectionManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.darksidecode.kantanj.db.mongo.MongoManager;

import java.io.IOException;
import java.net.SocketException;
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
    private PhysicsConfig physics;

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
    private GameServer gameServer;

    @Getter
    private UdpServer udpServer;

    void bootstrap() throws Exception {
        if (startupBeginTime != 0)
            throw new IllegalStateException("cannot bootstrap twice");

        startupBeginTime = System.currentTimeMillis();

        // Загружаем конфиги из файлов (или генерируем и используем дефолтные, если файлы отсутствуют).
        loadConfigs();

        // Настраиваем основные компоненты, необходимые для работы сервера.
        setupShutdownHook();
        setupExecutor();
        setupVirtualConnectivity(); // используется в БД, так что должно делаться до setupDatabase
        setupDatabase();
        setupConsoleCommands();
        startGameServer();

        double startupTookSeconds = (System.currentTimeMillis() - startupBeginTime) / 1000.0D;
        log.info("Startup done! Everything took {} s.", String.format("%.2f", startupTookSeconds));

        // Запускаем в основном (в этом) потоке в последнюю очередь. Тут ждать уже нечего.
        startUdpSocketServer();
    }

    private void loadConfigs() throws IOException {
        config  = AwdServerConfig.loadOrDefault();
        physics = PhysicsConfig  .loadOrDefault();
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

    private void setupVirtualConnectivity() {
        virConManager = new VirtualConnectionManager(this);
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

    private void startGameServer() {
        gameServer = new GameServer(this);
        gameServer.startGameLoopInNewThread();
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> shutdown(true), "AwdServer-Shutdown-Hook"));
    }

    public void shutdown() {
        shutdown(false);
    }

    private void shutdown(boolean isShutdownHook) {
        log.info("Shutting down...");

        gameServer.stop();
        udpServer.stop();
        db.close();

        if (!isShutdownHook) {
            log.info("Bye!");
            System.exit(0);
        }
    }

    private void startUdpSocketServer() throws SocketException {
        // Менеджер и обработчики пакетов.
        packetManager = new PacketManager(this);

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

package gg.aswedrown.server;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.data.DbInfo;
import gg.aswedrown.server.data.player.ConnectionData;
import gg.aswedrown.server.listener.CreateLobbyRequestListener;
import gg.aswedrown.server.listener.HandshakeRequestListener;
import gg.aswedrown.server.listener.KeepAliveListener;
import gg.aswedrown.server.listener.PacketManager;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.data.lobby.MongoLobbyRepository;
import gg.aswedrown.server.udp.AwdUdpServer;
import gg.aswedrown.server.udp.UdpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.darksidecode.kantanj.db.mongo.MongoManager;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public final class AwdServer {

    private static final long CONN_DATA_CLEANUP_FREQ_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private static final long CONN_DATA_LIFESPAN_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private AwdServer() {}

    @Getter
    private static final AwdServer server = new AwdServer();

    private long startupBeginTime;

    @Getter
    private AwdServerConfig config;

    @Getter
    private ExecutorService executor;

    /**
     * Основные сведения о текущих "подключениях".
     * В основном используется в предыгровой стадии.
     * Здесь ключ - IP-адрес, значение - основные сведения о соединении.
     */
    @Getter
    private final Map<InetAddress, ConnectionData> connDataMap = new ConcurrentHashMap<>();

    @Getter
    private MongoManager db;

    @Getter
    private LobbyManager lobbyManager;

    @Getter
    private PacketManager packetManager;

    @Getter
    private UdpServer udpServer;

    void bootstrap() throws Exception {
        if (startupBeginTime != 0)
            throw new IllegalStateException("cannot bootstrap twice");

        startupBeginTime = System.currentTimeMillis();

        loadConfig();

        setupExecutor();
        setupShutdownHook();
        setupDatabase();
        setupConnDataCleanup();

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

        try (InputStream in = new FileInputStream("awd-server-config.yml")) {
            config = new Yaml().loadAs(in, AwdServerConfig.class);
        }
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

        lobbyManager = new LobbyManager(this, new MongoLobbyRepository(db));
    }

    private void setupConnDataCleanup() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // Периодически удаляем старые, ненужные данные о больше
                // не актуальных "соединениях", чтобы не засорять память.
                List<InetAddress> toRemove = new ArrayList<>();

                connDataMap.keySet().stream()
                        .filter(ip -> connDataMap.get(ip)
                                .getMillisSinceLastAccess() > CONN_DATA_LIFESPAN_MILLIS)
                        .forEach(toRemove::add);

                toRemove.forEach(connDataMap::remove);

                log.debug("Deleted {} ancient connection data entries. " +
                        "New connection data map size: {}.", toRemove.size(), connDataMap.size());
            }
        }, CONN_DATA_CLEANUP_FREQ_MILLIS, CONN_DATA_CLEANUP_FREQ_MILLIS);
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
        // Обработчики пакетов.
        packetManager = new PacketManager(this,
                PacketWrapper.PacketCase.HANDSHAKEREQUEST, new HandshakeRequestListener(this),
                PacketWrapper.PacketCase.CREATELOBBYREQUEST, new CreateLobbyRequestListener(this),
                PacketWrapper.PacketCase.KEEPALIVE, new KeepAliveListener(this)
        );

        // Сам UDP "сервер".
        udpServer = new AwdUdpServer(
                config.getUdpServerPort(),
                config.getUdpServerBufferSize(),
                executor,
                packetManager
        );

        udpServer.start();
    }

    public void handleNewConnection(InetAddress addr, String addrStr) {
        connDataMap.put(addr, new ConnectionData(addr, addrStr));
        log.debug("Handling new connection: {}.", addrStr);
    }

    public ConnectionData getConnData(InetAddress addr) {
        return connDataMap.get(addr);
    }

}

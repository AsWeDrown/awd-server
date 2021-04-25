package gg.aswedrown.server;

import gg.aswedrown.net.PacketWrapper;
import gg.aswedrown.server.listener.HandshakeRequestListener;
import gg.aswedrown.server.listener.KeepAliveListener;
import gg.aswedrown.server.listener.PacketManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class AwdServer {

    private AwdServer() {}

    @Getter
    private static final AwdServer server = new AwdServer();

    @Getter
    private AwdServerConfig config;

    @Getter
    private ExecutorService executor;

    private long startupBeginTime;

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

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(this::onShutdown, "AwdServer-Shutdown-Hook"));
    }

    private void onShutdown() {
        udpServer.stop();
    }

    private void startUdpSocketServer() throws SocketException {
        // Обработчики пакетов.
        packetManager = new PacketManager(this,
                PacketWrapper.PacketCase.HANDSHAKEREQUEST, new HandshakeRequestListener(this),
                PacketWrapper.PacketCase.KEEPALIVE, new KeepAliveListener(this)
        );

        // Сам UDP "сервер".
        udpServer = new UdpServer(
                config.getUdpServerPort(),
                config.getUdpServerBufferSize(),
                executor,
                packetManager
        );

        udpServer.start();
    }

}

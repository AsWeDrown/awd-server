package gg.aswedrown.config;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter @Setter /* сеттеры нужны для snake-yaml */
public class AwdServerConfig {

    private static final transient String FILE_NAME = "config/awd-server.yml";

    /**
     * TODO - менять каждый раз при добавлении новых полей или удалении старых
     *        (чтобы было предупреждение о необходимости обновить файл).
     */
    private static final transient int SCHEMA_VERSION = 1;

    private int
            schemaVersion,

            executorQueueCapacity,
            executorMaxThreads,
            executorThreadsKeepAliveSeconds,

            udpServerPort,
            udpServerBufferSize,
            udpMaxVirtualConnections,

            cleanerLobbiesMaxIdleMillis,
            cleanerLobbiesCleanupPeriodMillis,
            cleanerVirConsMaxIdleMillis,

            gameTps,
            maxLobbySize,
            nonplayPingPeriodTicks,
            playPingPeriodTicks,
            minPlayersToStart,
            playerActionsBufferSize;

    public static AwdServerConfig loadOrDefault() throws IOException {
        AwdServerConfig config = ConfigurationLoader
                .loadOrDefault(FILE_NAME, AwdServerConfig.class);

        if (config.getSchemaVersion() != SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "incompatible configuration file, aborting startup; file schema: "
                            + config.getSchemaVersion() + ", server schema: " + SCHEMA_VERSION);

        if (config.getUdpMaxVirtualConnections() >= config.getExecutorMaxThreads())
            throw new IllegalArgumentException(
                    "invalid configuration: udpMaxVirtualConnections must be less than executorMaxThreads");

        return config;
    }

}

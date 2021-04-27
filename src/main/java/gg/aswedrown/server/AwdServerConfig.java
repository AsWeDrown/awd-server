package gg.aswedrown.server;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AwdServerConfig {

    static final transient String FILE_NAME = "awd-server-config.yml";

    /**
     * TODO - менять каждый раз при добавлении новых полей или удалении старых
     *        (чтобы сервер просигнализировал о необходимости обновить файл).
     */
    static final transient int SCHEMA_VERSION = 1;

    private int
            schemaVersion,

            executorQueueCapacity,
            executorMaxThreads,
            executorThreadsKeepAliveSeconds,

            udpServerPort,
            udpServerBufferSize,

            dbCleanerLobbiesMaxObjectLifespanMillis,
            dbCleanerLobbiesCleanupPeriodMillis,
            dbCleanerVirtualConnectionsMaxObjectLifespanMillis,
            dbCleanerVirtualConnectionsCleanupPeriodMillis,

            pingPeriodMillis;

}

package gg.aswedrown.server;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AwdServerConfig {

    static final transient String FILE_NAME = "awd-server-config.yml";

    private int executorQueueCapacity;

    private int executorMaxThreads;

    private int executorThreadsKeepAliveSeconds;

    private int udpServerPort;

    private int udpServerBufferSize;

}

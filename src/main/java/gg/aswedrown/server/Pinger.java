package gg.aswedrown.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

@RequiredArgsConstructor
public class Pinger extends TimerTask {

    private final Object lock = new Object();

    private final Map<InetAddress, Long> lastLatency = new HashMap<>();

    @NonNull
    private final AwdServer srv;

    @Override
    public void run() {
        synchronized (lock) {
            // TODO: 27.04.2021
        }
    }

    public void connectionEstablished(@NonNull InetAddress addr) {
        synchronized (lock) {
            lastLatency.put(addr, 0L);
        }
    }

    public void connectionClosed(@NonNull InetAddress addr) {
        synchronized (lock) {
            lastLatency.remove(addr);
        }
    }

    public Long getLastLatency(@NonNull InetAddress addr) {
        synchronized (lock) {
            return lastLatency.get(addr);
        }
    }

    public long getMeanLatency() {
        synchronized (lock) {
            return (long) lastLatency.values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
        }
    }

}

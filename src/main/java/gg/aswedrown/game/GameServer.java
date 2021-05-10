package gg.aswedrown.game;

import gg.aswedrown.server.AwdServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class GameServer {

    private static final long START_DELAY_MILLIS = 5000L; // чтобы UDP-сервер точно успел запуститься

    private final AwdServer srv;

    private final AtomicLong currentTick = new AtomicLong(0L);

    public void startGameLoopInNewThread() {
        long tickPeriod = 1000L / srv.getConfig().getGameTps();
        log.info("Starting game loop ({} TPS).", srv.getConfig().getGameTps());

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runGameLoop();
            }
        }, START_DELAY_MILLIS, tickPeriod);
    }

    private void runGameLoop() {
        currentTick.incrementAndGet();

        flushAllPacketQueues();
    }

    private void flushAllPacketQueues() {
        srv.getVirConManager().flushAllPacketQueues();
    }

}

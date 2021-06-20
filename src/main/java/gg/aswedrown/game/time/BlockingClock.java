package gg.aswedrown.game.time;

import lombok.NonNull;

import java.util.concurrent.TimeUnit;

public class BlockingClock extends SimpleClock {

    private final Object lock = new Object();

    @Override
    public Clock reset() {
        synchronized (lock) {
            return super.reset();
        }
    }

    @Override
    public long getElapsedMillis() {
        synchronized (lock) {
            return super.getElapsedMillis();
        }
    }

    @Override
    public boolean hasElapsed(long millis) {
        synchronized (lock) {
            return super.hasElapsed(millis);
        }
    }

    @Override
    public boolean hasElapsed(long time, @NonNull TimeUnit unit) {
        synchronized (lock) {
            return super.hasElapsed(time, unit);
        }
    }

}

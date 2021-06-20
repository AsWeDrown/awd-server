package gg.aswedrown.game.time;

import lombok.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicClock implements Clock {

    private final AtomicLong clockTime;

    public AtomicClock() {
        this(System.currentTimeMillis());
    }

    public AtomicClock(long clockTime) {
        if (clockTime < 0L)
            throw new IllegalArgumentException("clockTime cannot be negative");

        this.clockTime = new AtomicLong(clockTime);
    }

    @Override
    public Clock reset() {
        clockTime.set(System.currentTimeMillis());
        return this;
    }

    @Override
    public long getElapsedMillis() {
        return System.currentTimeMillis() - clockTime.get();
    }

    @Override
    public boolean hasElapsed(long millis) {
        return getElapsedMillis() >= millis;
    }

    @Override
    public boolean hasElapsed(long time, @NonNull TimeUnit unit) {
        return hasElapsed(unit.toMillis(time));
    }

}

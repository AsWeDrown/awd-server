package gg.aswedrown.game.time;

import lombok.NonNull;

import java.util.concurrent.TimeUnit;

public class SimpleClock implements Clock {

    private long clockTime;

    public SimpleClock() {
        this(System.currentTimeMillis());
    }

    public SimpleClock(long clockTime) {
        if (clockTime < 0L)
            throw new IllegalArgumentException("clockTime cannot be negative");

        this.clockTime = clockTime;
    }

    @Override
    public Clock reset() {
        clockTime = System.currentTimeMillis();
        return this;
    }

    @Override
    public long getElapsedMillis() {
        return System.currentTimeMillis() - clockTime;
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

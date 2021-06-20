package gg.aswedrown.game.time;

import lombok.NonNull;

import java.util.concurrent.TimeUnit;

public interface Clock {

    Clock reset();

    long getElapsedMillis();

    boolean hasElapsed(long millis);

    boolean hasElapsed(long time, @NonNull TimeUnit unit);
    
}

package gg.aswedrown.game.task;

import gg.aswedrown.server.AwdServer;
import lombok.*;

import java.util.concurrent.TimeUnit;

public final class Task {

    final Runnable runnable;

    @Setter
    long delayTicks;

    public Task(@NonNull Runnable runnable, long delayTicks) {
        if (delayTicks < 0L)
            throw new IllegalArgumentException("delayTicks cannot be negative");

        this.runnable = runnable;
        this.delayTicks = delayTicks;
    }

    public static long toTicks(long time, @NonNull TimeUnit unit) {
        if (time < 0L)
            throw new IllegalArgumentException("time cannot be negative");
        return AwdServer.getServer().getConfig().getGameTps() * unit.toSeconds(time);
    }

}

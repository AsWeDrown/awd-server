package gg.aswedrown.game.world;

import lombok.Getter;

public class Environment {

    private static final long ENABLE_ALARM = 0b1;

    @Getter
    private long envBitfield;

    public Environment enableAlarm(boolean enable) {
        if (enable) envBitfield |=  ENABLE_ALARM;
        else        envBitfield &= ~ENABLE_ALARM;

        return this;
    }

}

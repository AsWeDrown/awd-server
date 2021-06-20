package gg.aswedrown.game.sound;

import gg.aswedrown.game.world.Location;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Sound {

    public static final int ROOF_HEAD_HIT  = 1;
    public static final int LOCKER_FALL    = 2;
    public static final int PLAYER_STEP    = 3;
    public static final int SWITCH_TOGGLE  = 4;
    public static final int HATCH_TOGGLE   = 5;

    private final int id;

    private final float sourceX, sourceY;

    public Sound(int id, @NonNull Location sourceLoc) {
        this(id, sourceLoc.getX(), sourceLoc.getY());
    }

}

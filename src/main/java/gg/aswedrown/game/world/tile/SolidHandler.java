package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import lombok.NonNull;

public class SolidHandler implements TileHandler {

    @Override
    public boolean isPassableBy(@NonNull Entity entity) {
        return false;
    }

}

package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public class SolidHandler extends TileHandler {

    public SolidHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean isPassableBy(@NonNull Entity entity) {
        return false;
    }

}

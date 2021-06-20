package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public class HatchOpenHandler extends HatchClosedHandler {

    public HatchOpenHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean isPassableBy(@NonNull Entity entity) {
        return true;
    }

}

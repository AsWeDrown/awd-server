package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import lombok.NonNull;

public class LadderHandler implements TileHandler {

    @Override
    public boolean isPassableBy(@NonNull Entity entity) {
        return true;
    }

    @Override
    public boolean isClimbableBy(@NonNull Entity entity) {
        return entity instanceof EntityPlayer;
    }

}

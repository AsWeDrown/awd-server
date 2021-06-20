package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public class DeadlyWaterHandler extends VoidHandler {

    public DeadlyWaterHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean kills(@NonNull Entity entity) {
        return entity instanceof EntityPlayer
                && entity.getBoundingBox().intersectsWith(tile.getBoundingBox());
    }

}

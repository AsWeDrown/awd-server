package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public class FinalCaptainDoorHandler extends SolidHandler {

    private static final float MAX_INTERACTION_DISTANCE = 1.5f; // в тайлах

    public FinalCaptainDoorHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean canInteract(@NonNull Entity entity) {
        return entity instanceof EntityPlayer
                && entity.getBoundingBox().distance(tile.getBoundingBox()) <= MAX_INTERACTION_DISTANCE;
    }

}

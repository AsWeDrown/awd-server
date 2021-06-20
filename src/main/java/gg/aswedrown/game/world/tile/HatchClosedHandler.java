package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public class HatchClosedHandler extends SolidHandler {

    private static final float MAX_INTERACTION_DISTANCE = 1.25f; // в тайлах

    public HatchClosedHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean canInteract(@NonNull Entity entity) {
        // Для открытия люка игрок должен быть сверху (над люком).
        return entity instanceof EntityPlayer && entity.getPosY() < tile.posY
                && entity.getBoundingBox().distance(tile.getBoundingBox()) <= MAX_INTERACTION_DISTANCE;
    }

}

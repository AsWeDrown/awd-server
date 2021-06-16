package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public class SwitchHandler extends VoidHandler {

    private static final float MAX_INTERACTION_DISTANCE = 1.75f; // в тайлах

    public SwitchHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean canInteract(@NonNull Entity entity) {
        return entity instanceof EntityPlayer && entity.getBoundingBox()
                .distance(tile.getBoundingBox()) <= MAX_INTERACTION_DISTANCE;
    }

}

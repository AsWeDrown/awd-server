package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

public abstract class InteractableHandler extends TileHandler  {

    public InteractableHandler(@NonNull TileBlock tile) {
        super(tile);
    }

    @Override
    public boolean canInteract(@NonNull Entity entity) {
        return entity instanceof EntityPlayer;
    }

}

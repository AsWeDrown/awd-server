package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TileHandler {

    @NonNull
    protected final TileBlock tile;

    public abstract boolean isPassableBy(@NonNull Entity entity);

    public boolean isClimbableBy(@NonNull Entity entity) {
        return false;
    }

}

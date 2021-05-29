package gg.aswedrown.game.world.tile;

import gg.aswedrown.game.entity.Entity;
import lombok.NonNull;

public interface TileHandler {

    boolean isPassableBy(@NonNull Entity entity);

}

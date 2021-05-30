package gg.aswedrown.game.entity;

import gg.aswedrown.game.world.TerrainControls;

public interface Fallable {

    int getMidairTicks();

    float getLastTickFallDistance();

    float getFallDistance();

    void updateGravity(TerrainControls terrainControls);

    default boolean isOnGround() {
        return getMidairTicks() == 0;
    }

}

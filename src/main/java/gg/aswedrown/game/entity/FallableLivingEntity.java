package gg.aswedrown.game.entity;

import gg.aswedrown.game.world.TerrainControls;
import gg.aswedrown.server.AwdServer;
import lombok.Getter;

public abstract class FallableLivingEntity extends LivingEntity implements Fallable {

    @Getter
    protected int midairTicks;

    @Getter
    protected float lastTickFallDistance, fallDistance;

    public FallableLivingEntity(int entityType) {
        super(entityType);
    }

    @Override
    public void updateGravity(TerrainControls terrainControls) {
        float freeFallAcce = AwdServer.getServer().getPhysics().getFreeFallAcce();
        fallDistance = (freeFallAcce * midairTicks * midairTicks) / 2.0f;
        float deltaY = fallDistance - lastTickFallDistance;
        lastTickFallDistance = fallDistance;
        posY = terrainControls.advanceTowardsYUntilTerrainCollision(this, posY + deltaY);

        if (terrainControls.isOnGround(this)) {
            midairTicks          =    0;
            lastTickFallDistance = 0.0f;
            fallDistance         = 0.0f;
        } else
            midairTicks++;
    }

}

package gg.aswedrown.game.event;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.GameEndStatus;
import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.entity.LivingEntity;
import gg.aswedrown.game.world.TileBlock;
import gg.aswedrown.game.world.World;
import gg.aswedrown.server.AwdServer;

import java.util.Collection;

public final class GenericEventListener implements GameEventListener {

    @Override
    public void onWorldUpdate(WorldUpdateEvent e) {
        World world = e.getWorld();
        Collection<Entity> entities = world.getEntitiesList();

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                TileBlock killingTile = world.getTerrainControls()
                        .getFirstIntersectingTile(entity, tile -> tile.handler.kills(entity));

                if (killingTile != null) {
                    living.kill();

                    ActiveGameLobby lobby = AwdServer.getServer()
                            .getGameServer().getActiveGameLobby(world.getLobbyId());

                    GameEvent event = new EntityDeathEvent(living);
                    lobby.getEventDispatcher().dispatchEvent(event);
                }
            }
        }
    }

    @Override
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof EntityPlayer) {
            // Кто-то из игроков умер. Game over!
            ActiveGameLobby lobby = ((EntityPlayer) e.getEntity()).getLobby();
            lobby.setGameEndStatus(GameEndStatus.FAILURE_SOMEONE_DIED);
            lobby.endGame();
        }
    }

}

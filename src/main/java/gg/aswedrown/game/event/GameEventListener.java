package gg.aswedrown.game.event;

public interface GameEventListener {

    default void onPlayerMove(PlayerMoveEvent e) {}

    default void onPlayerTileInteract(PlayerTileInteractEvent e) {}

    default void onWorldUpdate(WorldUpdateEvent e) {}

    default void onEntityDeath(EntityDeathEvent e) {}

}

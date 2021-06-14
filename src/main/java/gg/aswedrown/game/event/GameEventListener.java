package gg.aswedrown.game.event;

public interface GameEventListener {

    default void onPlayerMove(PlayerMoveEvent e) {}

}

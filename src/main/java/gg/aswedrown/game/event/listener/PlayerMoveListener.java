package gg.aswedrown.game.event.listener;

import gg.aswedrown.game.event.PlayerMoveEvent;
import gg.aswedrown.game.event.RegisterGameEventListener;

@RegisterGameEventListener (PlayerMoveEvent.class)
public class PlayerMoveListener extends GameEventListener<PlayerMoveEvent> {

    @Override
    protected void onEvent(PlayerMoveEvent event) throws Exception {
        System.out.println("MOVE");
        System.out.println("  PLAYER " + event.getPlayer().getPlayerName());
        System.out.println("  FROM   " + event.getFrom());
        System.out.println("  TO     " + event.getTo());
        System.out.println();
    }

}

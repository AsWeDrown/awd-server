package gg.aswedrown.game.event;

import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.sound.Sound;

public final class GenericEventListener implements GameEventListener {

    @Override
    public void onPlayerMove(PlayerMoveEvent e) {
        EntityPlayer player = e.getPlayer();
        player.getLobby().playSound(player.getCurrentDimension(),
                new Sound(Sound.PLAYER_STEP, e.getTo().getX(), e.getTo().getY()));
    }

}

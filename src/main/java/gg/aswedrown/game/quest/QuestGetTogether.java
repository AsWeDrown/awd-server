package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.event.PlayerMoveEvent;
import gg.aswedrown.game.event.PlayerTileInteractEvent;
import gg.aswedrown.game.sound.Sound;

import java.util.Collection;

/**
 * Все игроки должны собраться вместе, т.е. встать достаточно близко друг к другу.
 */
public class QuestGetTogether extends Quest {

    private static final int TYPE = 4;

    private static final float REQUIRED_PROXIMITY = 1.5f;

    public QuestGetTogether() {
        super(TYPE, 1, true);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e) {
        ActiveGameLobby lobby = e.getPlayer().getLobby();
        Collection<EntityPlayer> players = lobby.getPlayersList();

        for (EntityPlayer somePlayer : players)
            for (EntityPlayer otherPlayer : players)
                if (somePlayer != otherPlayer && somePlayer.getBoundingBox()
                        .distance(otherPlayer.getBoundingBox()) > REQUIRED_PROXIMITY)
                    // По крайней мере двое из игроков находятся слишком далеко друг от друга.
                    return;

        // Все игроки находятся достаточно близко друг к другу.
        advance(lobby, 1);
    }

    @Override
    public void onPlayerTileInteract(PlayerTileInteractEvent e) {
        if (e.getTile().tileId == 15) { // HatchClosed
            ActiveGameLobby lobby = e.getPlayer().getLobby();

            lobby.playSound(e.getPlayer().getCurrentDimension(),
                    new Sound(Sound.HATCH_TOGGLE, e.getTile().posX, e.getTile().posY));
            lobby.replaceTileAt(e.getPlayer().getCurrentDimension(),
                    e.getTile().posX, e.getTile().posY, 16); // HatchOpen
        }
    }

}

package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.GameEndStatus;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.event.PlayerTileInteractEvent;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * Все игроки должны добраться до капитана подлодки.
 * На пути будут различные препятствия.
 */
public class QuestVisitCaptain extends Quest {

    private static final int TYPE = 5;

    private final Collection<EntityPlayer> ready = new HashSet<>();

    public QuestVisitCaptain(int playersInLobby) {
        super(TYPE, playersInLobby, true);
    }

    @Override
    protected void questEnded(@NonNull ActiveGameLobby lobby) throws Exception {
        // Игра пройдена!
        lobby.setGameEndStatus(GameEndStatus.SUCCESS);
        lobby.endGame();
    }

    @Override
    public void onPlayerTileInteract(PlayerTileInteractEvent e) {
        ActiveGameLobby lobby = e.getPlayer().getLobby();
        TileBlock clickedTile = e.getTile();

        if (e.getCommand() == PlayerTileInteractEvent.Command.LEFT_CLICK
                && clickedTile.tileId == 26 // FinalCaptainDoor
                && ready.add(e.getPlayer()))
            advance(lobby, 1);
    }

}

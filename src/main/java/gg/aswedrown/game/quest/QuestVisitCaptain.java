package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.GameEndStatus;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.event.PlayerMoveEvent;
import gg.aswedrown.game.event.PlayerTileInteractEvent;
import gg.aswedrown.game.sound.Sound;
import gg.aswedrown.game.time.AtomicClock;
import gg.aswedrown.game.time.Clock;
import gg.aswedrown.game.world.TileBlock;
import gg.aswedrown.game.world.World;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Все игроки должны добраться до капитана подлодки.
 * На пути будут различные препятствия.
 */
public class QuestVisitCaptain extends Quest {

    private static final int TYPE = 5;

    private static final long MIN_TOGGLE_DELAY_SECONDS = 3L;

    private final Collection<EntityPlayer> ready = new HashSet<>();

    private final Clock toggleClock = new AtomicClock(0L);

    private volatile boolean open;

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
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!toggleClock.hasElapsed(MIN_TOGGLE_DELAY_SECONDS, TimeUnit.SECONDS))
            return;

        ActiveGameLobby lobby = e.getPlayer().getLobby();
        int dim = e.getPlayer().getCurrentDimension();
        World world = lobby.getWorld(dim);
        boolean anyPlatePressed = false;

        for (EntityPlayer player : lobby.getPlayersList()) {
            TileBlock pressedPlate = world.getTerrainControls()
                    .getFirstIntersectingTile(player, tile -> tile.tileId == 30); // PressurePlate

            if (pressedPlate != null) {
                anyPlatePressed = true;
                break;
            }
        }

        if (anyPlatePressed && !open) {
            // Открываем проход.
            open = true;
            toggleBlockingTiles(lobby, dim);
        } else if (!anyPlatePressed && open) {
            // Закрываем проход.
            open = false;
            toggleBlockingTiles(lobby, dim);
        }
    }

    @Override
    public void onPlayerTileInteract(PlayerTileInteractEvent e) {
        ActiveGameLobby lobby = e.getPlayer().getLobby();
        TileBlock clickedTile = e.getTile();

        if (e.getCommand() == PlayerTileInteractEvent.Command.LEFT_CLICK
                && (clickedTile.tileId == 26 || clickedTile.tileId == 27) // FinalCaptainDoorTop || FinalCaptainDoorBottom
                && ready.add(e.getPlayer()))
            advance(lobby, 1);
    }

    private void toggleBlockingTiles(ActiveGameLobby lobby, int dim) {
        toggleClock.reset();

        // 6 - SubmarineBodyMetalContourNone; 7 - StandardMetalLadder.
        lobby.replaceTileAt(dim, 46, 35, open ? 7 : 6);
        lobby.replaceTileAt(dim, 47, 35, open ? 7 : 6);
        lobby.replaceTileAt(dim, 54, 35, open ? 7 : 6);
        lobby.replaceTileAt(dim, 55, 35, open ? 7 : 6);
        lobby.replaceTileAt(dim, 62, 35, open ? 7 : 6);
        lobby.replaceTileAt(dim, 63, 35, open ? 7 : 6);

        // TODO: 20.06.2021 отдельный звук для этого (сейчас используется звук открытия люка).
        lobby.playSound(dim, new Sound(Sound.HATCH_TOGGLE,
                54.0f, 35.0f)); // источник звука -- где-то посередине этой "полосы препятствий"
    }

}

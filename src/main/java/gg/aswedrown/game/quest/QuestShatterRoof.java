package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.event.PlayerMoveEvent;
import gg.aswedrown.game.world.TileBlock;
import gg.aswedrown.game.world.World;
import gg.aswedrown.game.world.Worlds;
import lombok.NonNull;

/**
 * Игроки в жилом отсеке должны "побиться головой" об потолок (с помощью лестниц),
 * чтобы немного "расшатать" потолок и уронить таким образом шкаф, перегораживающий
 * путь к ним другому игроку (который поднялся из отсека склада и ждёт, когда ему
 * откроют проход, чтобы он мог открыть люк над остальными и освободить их).
 */
public class QuestShatterRoof extends Quest {

    private static final int TYPE = 3;

    private static final long MIN_ROOT_HEAD_HIT_DELAY_MILLIS = 1500L;

    public QuestShatterRoof(int playersInLobby) {
        super(TYPE, 5 * playersInLobby, true);
    }

    @Override
    protected void questEnded(@NonNull ActiveGameLobby lobby) throws Exception {
        // "Роняем" шкаф. Как и со всеми квестами... TODO: 20.06.2021 сделать нормально
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 66, 42, 11); // LockerStanding1      --> SubmarineInsideMetal
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 66, 43, 11); // LockerStanding2      --> SubmarineInsideMetal
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 66, 44, 24); // LockerStanding3      --> LockerFallen3
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 65, 44, 23); // SubmarineInsideMetal --> LockerFallen2
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 64, 44, 22); // SubmarineInsideMetal --> LockerFallen1
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e) {
        EntityPlayer player = e.getPlayer();

        if (e.getTo().getY() < e.getFrom().getY()) { // игрок двигается вверх
            int tileHitX = (int) Math.floor(player.getBoundingBox().getCenterX());
            int tileHitY = (int) Math.floor(e.getTo().getY() - 1.0f);
            World world = player.getLobby().getWorld(player.getCurrentDimension());
            TileBlock tileHit = world.getTerrainControls().getTileAt(tileHitX, tileHitY);

            System.out.println("HIT " + tileHit.tileId + " at " + tileHit.posX + ", " + tileHit.posY);

            if (!tileHit.handler.isPassableBy(player)
                    && player.getRoofHeadHitClock().hasElapsed(MIN_ROOT_HEAD_HIT_DELAY_MILLIS)) {
                System.out.println("  Advance!");
                player.getRoofHeadHitClock().reset();
                advance(player.getLobby(), 1);
            }
        }
    }

}

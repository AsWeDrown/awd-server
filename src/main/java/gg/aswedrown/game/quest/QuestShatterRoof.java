package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.event.PlayerMoveEvent;
import gg.aswedrown.game.sound.Sound;
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

    private static final long MIN_ROOF_HEAD_HIT_DELAY_MILLIS = 1500L;

    public QuestShatterRoof(int playersInLobby) {
        super(TYPE, 5 * playersInLobby, true);
    }

    @Override
    protected void questEnded(@NonNull ActiveGameLobby lobby) throws Exception {
        // "Роняем" шкаф. Как и со всеми квестами... TODO: 20.06.2021 сделать нормально
        lobby.playSound(Worlds.DIM_SUBMARINE_BEGIN,
                new Sound(Sound.LOCKER_FALL, 66.0f, 44.0f));
        
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 66, 42, 11); // LockerStanding1      --> SubmarineInsideMetal
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 66, 43, 11); // LockerStanding2      --> SubmarineInsideMetal
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 66, 44, 24); // LockerStanding3      --> LockerFallen3
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 65, 44, 23); // SubmarineInsideMetal --> LockerFallen2
        lobby.replaceTileAt(Worlds.DIM_SUBMARINE_BEGIN, 64, 44, 22); // SubmarineInsideMetal --> LockerFallen1
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e) {
        EntityPlayer player = e.getPlayer();
        ActiveGameLobby lobby = player.getLobby();

        if (e.getTo().getY() < e.getFrom().getY()) { // игрок двигается вверх
            int tileHitX = (int) Math.floor(player.getBoundingBox().getCenterX());
            int tileHitY = (int) Math.floor(e.getTo().getY() - 1.0f);
            World world = lobby.getWorld(player.getCurrentDimension());
            TileBlock tileHit = world.getTerrainControls().getTileAt(tileHitX, tileHitY);

            if (!tileHit.handler.isPassableBy(player)
                    && player.getRoofHeadHitClock().hasElapsed(MIN_ROOF_HEAD_HIT_DELAY_MILLIS)) {
                player.getRoofHeadHitClock().reset();
                lobby.playSound(world.getDimension(), new Sound(Sound.ROOF_HEAD_HIT, e.getTo()));
                advance(player.getLobby(), 1);
            }
        }
    }

}

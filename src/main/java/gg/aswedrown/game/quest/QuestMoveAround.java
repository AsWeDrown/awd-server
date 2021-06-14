package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.event.PlayerMoveEvent;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * Каждый игрок должен пройти некоторое (небольшое) расстояние.
 * Служит для "привыкания" игроков к управлению персонажем в игре.
 */
public class QuestMoveAround extends Quest {

    private static final int TYPE = 1;

    private static final float TRIGGER_DISTANCE = 5.0f; // в тайлах

    private final Collection<EntityPlayer> completed = new HashSet<>();

    public QuestMoveAround(int playersInLobby) {
        super(TYPE, playersInLobby);
    }

    @Override
    protected void questAdvanced(@NonNull ActiveGameLobby lobby, int progressPointsAdded) throws Exception {
        if (progress >= maxProgress) {
            // TODO: 14.06.2021 ?
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getPlayer().getTotalDistanceMoved()
                >= TRIGGER_DISTANCE && completed.add(e.getPlayer()))
            advance(e.getPlayer().getLobby(), 1);
    }

}

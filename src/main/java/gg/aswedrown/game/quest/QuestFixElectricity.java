package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.event.PlayerTileInteractEvent;
import gg.aswedrown.game.sound.Sound;
import gg.aswedrown.game.task.Task;
import gg.aswedrown.game.world.TileBlock;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * В каждой из комнат, где появились игроки, есть рубильник.
 * Игроки должны одновременно (или почти одновременно) нажать
 * на рубильники в своих комнатах, чтобы починить электричество.
 * Чтобы активировать рубильник, игрок должен подойти к нему
 * достаточно близко. Каждый рубильник включается лишь на 3
 * секунды и ждёт включения второго рубильника. Если второй
 * рубильник в течение этих 3 секунд был включен, задание
 * считается пройденным. Иначе рубильник отключается.
 */
public class QuestFixElectricity extends Quest {

    private static final int TYPE = 2;

    private final AtomicInteger switchesOn = new AtomicInteger(0);

    public QuestFixElectricity() {
        super(TYPE, 1, true);
    }

    @Override
    protected void questEnded(@NonNull ActiveGameLobby lobby) throws Exception {
        lobby.updateEnvironment(lobby.getEnvironment().enableAlarm(false));
        lobby.getQuestManager().beginQuest(new QuestShatterRoof(lobby.getPlayers()));
        lobby.getQuestManager().beginQuest(new QuestGetTogether());
    }

    @Override
    public void onPlayerTileInteract(PlayerTileInteractEvent e) {
        ActiveGameLobby lobby = e.getPlayer().getLobby();
        TileBlock clickedTile = e.getTile();

        if (e.getCommand() == PlayerTileInteractEvent.Command.LEFT_CLICK
                && clickedTile.tileId == 14) { // SwitchOff // TODO: 16.06.2021 сделать нормально
            lobby.getScheduler().schedule(new Task(() -> {
                lobby.playSound(e.getPlayer().getCurrentDimension(),
                        new Sound(Sound.SWITCH_TOGGLE, e.getTile().posX, e.getTile().posY));
                lobby.replaceTileAt(e.getPlayer().getCurrentDimension(),
                        clickedTile.posX, clickedTile.posY, 13); // SwitchOn
            }, 0L));

            if (switchesOn.incrementAndGet() == 2)
                // Оба рубильника включены - задание выполнено.
                advance(lobby, 1);
            else {
                // Второй рубильник ещё не включен. Ждём до 3 секунд.
                lobby.getScheduler().schedule(new Task(() -> {
                    if (state == QuestState.ACTIVE) {
                        // Второй рубильник так и не был включен. Отключаем этот рубильник.
                        switchesOn.decrementAndGet();

                        lobby.playSound(e.getPlayer().getCurrentDimension(),
                                new Sound(Sound.SWITCH_TOGGLE, e.getTile().posX, e.getTile().posY));
                        lobby.replaceTileAt(e.getPlayer().getCurrentDimension(),
                                clickedTile.posX, clickedTile.posY, 14); // SwitchOff
                    }
                }, Task.toTicks(3, TimeUnit.SECONDS)));
            }
        }
    }

}

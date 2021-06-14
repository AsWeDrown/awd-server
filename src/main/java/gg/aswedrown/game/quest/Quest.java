package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.event.GameEventListener;
import gg.aswedrown.net.NetworkService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class Quest implements GameEventListener {

    private final int type;

    protected final int maxProgress;

    protected int id; // отсчёт с 1

    protected int state = QuestState.NOT_BEGUN;

    protected int progress;

    protected void questBegun(@NonNull ActiveGameLobby lobby) throws Exception {}

    protected void questAdvanced(@NonNull ActiveGameLobby lobby, int progressPointsAdded) throws Exception {}

    protected void questEnded(@NonNull ActiveGameLobby lobby) throws Exception {}

    protected void advance(@NonNull ActiveGameLobby lobby, int progressPointsToAdd) {
        progress += progressPointsToAdd;
        lobby.forEachPlayer(player -> NetworkService
                .advanceQuest(player.getVirCon(), this));

        try {
            questAdvanced(lobby, progressPointsToAdd);
        } catch (Exception ex) {
            log.error("Unhandled exception in questAdvanced", ex);
        }
    }

}

package gg.aswedrown.game.quest;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.net.NetworkService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class QuestManager {

    private final ActiveGameLobby lobby;

    private final Object lock = new Object();

    private final AtomicInteger nextQuestId = new AtomicInteger(0); // отсчёт с 1

    private final Collection<Quest> activeQuests = new HashSet<>();

    public void beginQuest(@NonNull Quest quest) {
        if (quest.state != QuestState.NOT_BEGUN)
            throw new IllegalStateException("quest has already begun");

        synchronized (lock) {
            if (activeQuests.add(quest)) {
                quest.id = nextQuestId.incrementAndGet();
                quest.state = QuestState.ACTIVE;
                lobby.getEventDispatcher().registerListener(quest);

                try {
                    quest.questBegun(lobby);
                } catch (Exception ex) {
                    log.error("Unhandled exception in questBegun", ex);
                }

                lobby.forEachPlayer(player -> NetworkService
                        .beginQuest(player.getVirCon(), quest));
            } else
                throw new IllegalStateException("duplicate quest");
        }
    }

    public void endQuest(@NonNull Quest quest) {
        synchronized (lock) {
            if (activeQuests.remove(quest)) {
                try {
                    quest.questEnded(lobby);
                } catch (Exception ex) {
                    log.error("Unhandled exception in questEnded", ex);
                }

                lobby.getEventDispatcher().unregisterListener(quest);
                lobby.forEachPlayer(player -> NetworkService
                        .endQuest(player.getVirCon(), quest));
            } else
                throw new IllegalStateException("no such quest");
        }
    }

}

package gg.aswedrown.game;

import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class GameServer {

    private static final long START_DELAY_MILLIS = 5000L; // чтобы UDP-сервер точно успел запуститься

    private final AwdServer srv;

    private final AtomicLong currentTick = new AtomicLong(0L);

    private final Collection<ActiveGameLobby> activeGameLobbies = new ArrayList<>();
    private final Object activeGameLobbiesLock = new Object();

    public void startGameLoopInNewThread() {
        long tickPeriod = 1000L / srv.getConfig().getGameTps();
        log.info("Starting game loop at {} TPS.", srv.getConfig().getGameTps());

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runGameLoop();
            }
        }, START_DELAY_MILLIS, tickPeriod);
    }

    private void runGameLoop() {
        currentTick.incrementAndGet();

        srv.getVirConManager().flushAllReceiveQueues(); // обрабатываем пакеты, полученные от игроков
        update();                                       // выполняем обновление (и, м.б., ставим на отправку пакеты)
        srv.getVirConManager().flushAllSendQueues();    // отправляем пакеты, поставленые в очередь после обновления
    }

    private void update() {
        synchronized (activeGameLobbiesLock) {
            activeGameLobbies.stream().filter(ActiveGameLobby::isGameBegun).forEach(lobby -> {
                try {
                    lobby.update();
                } catch (Exception ex) {
                    log.error("Unhandled exception during game state update in lobby {}:",
                            lobby.getLobbyId(), ex);
                }
            });
        }
    }

    public ActiveGameLobby getActiveGameLobby(int lobbyId) {
        synchronized (activeGameLobbiesLock) {
            return activeGameLobbies.stream()
                    .filter(lobby -> lobby.getLobbyId() == lobbyId)
                    .findAny()
                    .orElse(null);
        }
    }

    public void registerActiveGameLobby(@NonNull ActiveGameLobby lobby) {
        synchronized (activeGameLobbiesLock) {
            activeGameLobbies.add(lobby);
        }
    }

    public void unregisterActiveGameLobby(@NonNull ActiveGameLobby lobby) {
        synchronized (activeGameLobbiesLock) {
            activeGameLobbies.remove(lobby);
        }
    }

}

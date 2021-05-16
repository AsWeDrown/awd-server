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

    private final Collection<ActiveGameLobby> activeGameLobbies = new ArrayList<>();
    private final Object activeGameLobbiesLock = new Object();

    private final AtomicLong currentTick  = new AtomicLong(0L);
    private final AtomicLong lastPingTick = new AtomicLong(0L);

    private volatile boolean serverStopped;

    public void startGameLoopInNewThread() {
        long tickPeriod = 1000L / srv.getConfig().getGameTps();
        log.info("Starting game server at {} TPS.", srv.getConfig().getGameTps());

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (serverStopped)
                    // Не делаем return, чтобы успеть совершить последнее обновление,
                    // чтобы разослать игрокам все необходимые пакеты (о завершении).
                    cancel();

                update();
            }
        }, START_DELAY_MILLIS, tickPeriod);
    }

    private void serverStopped() {
        log.info("Stopping game server.");
        activeGameLobbies.forEach(ActiveGameLobby::endGame);
    }

    public void stop() {
        if (!serverStopped) {
            serverStopped = true;
            serverStopped();
        }
    }

    /**
     * Этот update() ПОЛНОСТЬЮ заменяет ActiveGameLobby#update() для
     * виртуальных соединений игроков, НЕ находящихся в игре (AUTH/LOBBY/... (но НЕ PLAY)).
     *
     * @see ActiveGameLobby#update()
     */
    @SuppressWarnings ("JavadocReference")
    private void update() {
        currentTick.incrementAndGet();

        srv.getVirConManager().flushAllReceiveQueues(); // обрабатываем пакеты, полученные от игроков
        updateVirtualConnections();                     // выполняем обновление (и, м.б., ставим на отправку пакеты)
        srv.getVirConManager().flushAllSendQueues();    // отправляем пакеты, поставленые в очередь после обновления
    }

    private void updateVirtualConnections() {
        sendPings(); // для поддержания соединения с клиентами
    }

    private void sendPings() {
        long curr = currentTick .get();
        long last = lastPingTick.get();

        if ((curr - last) > srv.getConfig().getNonplayPingPeriodTicks()) {
            // Пингуем клиентов, НЕ находящихся в игре.
            srv.getVirConManager().pingThose(virCon -> virCon.getGameLobby() == null);
            lastPingTick.set(curr);
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

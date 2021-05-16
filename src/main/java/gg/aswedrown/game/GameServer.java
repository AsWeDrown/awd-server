package gg.aswedrown.game;

import gg.aswedrown.game.profiling.TpsMeter;
import gg.aswedrown.server.AwdServer;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
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

    @Getter
    private final TpsMeter tpsMeter = new TpsMeter();

    private volatile boolean serverStopped;

    public void startGameLoopInNewThread() {
        // Запускаем основной сервер (ядро). Используется по сути только для соединений, не связанных с
        // игроками (т.е. для связи с ещё не начавшими игру клиентами, например, на этапах AUTH и LOBBY).
        long tickPeriod = 1000L / srv.getConfig().getGameTps();
        log.info("Starting game server at {} TPS.", srv.getConfig().getGameTps());

        // ВАЖНО использовать здесь именно scheduleAtFixedRate, чтобы сервер "изо всех сил"
        // старался придерживаться указанного значения TPS. При обычном schedule сервер будет
        // тикать ЗНАЧИТЕЛЬНО реже (например, ~21 раз в секунду вместо указанных 25), что в
        // случае игровых обновлений совершенно недопустимо. У scheduleAtFixedRate есть свой
        // недостаток: иногда фактический TPS выходит немного выше запрошенного (например,
        // 25.005 раз вместо 25 ровно) - но это несущественно, и с этим точно можно жить.
        new Timer().scheduleAtFixedRate(new TimerTask() {
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

        synchronized (activeGameLobbiesLock) {
            // Создаём копию для итерации, т.к. endGame внутри удаляет этот объект из списка (concurrent mod).
            Collection<ActiveGameLobby> copy = new ArrayList<>(activeGameLobbies);
            copy.forEach(ActiveGameLobby::endGame);
            activeGameLobbies.clear();
        }
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
        tpsMeter.onUpdate();

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

    public int getActiveGameLobbies() {
        synchronized (activeGameLobbiesLock) {
            return activeGameLobbies.size();
        }
    }

    public float getAverageGameLobbiesTps() {
        synchronized (activeGameLobbiesLock) {
            boolean approx = false;
            float sum = 0.0f;

            for (ActiveGameLobby lobby : activeGameLobbies) {
                float lobbyTps = lobby.getTpsMeter().estimateTps();

                if (lobbyTps < 0.0f) {
                    // В одной из комнат в данный момент TPS может быть вычислен лишь.
                    // приближённо. Тогда и весь результат отображаем как приближённый.
                    sum -= lobbyTps;
                    approx = true;
                } else
                    sum += lobbyTps;
            }

            float avg = sum / activeGameLobbies.size();

            return approx ? -avg : avg;
        }
    }

    public int getTotalPlayersPlaying() {
        synchronized (activeGameLobbiesLock) {
            return activeGameLobbies.stream().mapToInt(ActiveGameLobby::getPlayers).sum();
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

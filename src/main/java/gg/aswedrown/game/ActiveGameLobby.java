package gg.aswedrown.game;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.profiling.TpsMeter;
import gg.aswedrown.game.world.World;
import gg.aswedrown.net.NetworkService;
import gg.aswedrown.server.AwdServer;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ActiveGameLobby {

    private final Object lock = new Object();

    private final AwdServer srv;

    @Getter
    private final int lobbyId;

    private final int hostPlayerId;

    @NonNull
    private final Collection<EntityPlayer> players;

    private final Map<Integer, World> dimensions = new HashMap<>();

    private volatile boolean gameBegun;
    private volatile boolean gameStopped;

    private final AtomicLong currentTick  = new AtomicLong(0L);
    private final AtomicLong lastPingTick = new AtomicLong(0L);

    @Getter
    private final TpsMeter tpsMeter = new TpsMeter();

    public int getPlayers() {
        return players.size();
    }

    public int getReadyPlayers() {
        return (int) players.stream().filter(EntityPlayer::isReady).count();
    }

    public int getJoinedWorldPlayers() {
        return (int) players.stream().filter(EntityPlayer::isJoinedWorld).count();
    }

    public int getDimensions() {
        return dimensions.size();
    }

    public EntityPlayer getPlayer(int playerId) {
        synchronized (lock) {
            return players.stream()
                    .filter(player -> player.getPlayerId() == playerId)
                    .findAny()
                    .orElse(null);
        }
    }

    public void playerLoadedWorld(int playerId) {
        synchronized (lock) {
            EntityPlayer player = getPlayer(playerId);

            if (player != null) {
                // Очередной игрок комнаты сообщил об успешной загрузке мира.
                player.setReady(true);

                if (players.stream().allMatch(EntityPlayer::isReady))
                    // Все игроки в комнате сообщили об успешной загрузке мира.
                    // Рассылаем команду о присоединении к миру (отображению игры на экране).
                    players.forEach(p -> NetworkService.joinWorldCommand(p.getVirCon()));
            }
        }
    }

    public void playerJoinedWorld(int playerId) {
        synchronized (lock) {
            EntityPlayer player = getPlayer(playerId);

            if (player != null) {
                // Очередной игрок комнаты сообщил об успешном присоединении к миру.
                player.setJoinedWorld(true);

                if (players.stream().allMatch(EntityPlayer::isJoinedWorld))
                    // Все игроки в комнате сообщили об успешном присоединении к миру.
                    // Приступаем к фактическому запуску игры.
                    beginGame();
            }
        }
    }

    public void playerLeftWorld(int playerId) {
        synchronized (lock) {
            EntityPlayer player = getPlayer(playerId);

            if (player != null) {
                despawnEntity(player);
                players.remove(player);

                if (player.getPlayerId() == hostPlayerId)
                    // Завершаем игру при выходе хоста.
                    endGame();
            }
        }
    }

    /**
     * Запускает игру в этой комнате фактически. Выполняется только
     * после получения пакета JoinWorldComplete от всех игроков в комнате.
     */
    private void beginGame() {
        synchronized (lock) {
            if (gameBegun) {
                log.warn("beginGame() called twice");
                return;
            }

            gameBegun = true;
            log.info("The game in lobby {} has begun ({} players).", lobbyId, players.size());

            // Уведомляем всех игроков о спавне.. всех игроков.
            // (Фактический спавн (на сервере) каждого игрока произошёл ранее - завершении загрузки им мира.)
            players.forEach(this::broadcastEntitySpawn);

            // Запускаем игровой цикл обновления в этой комнате.
            long tickPeriod = 1000L / srv.getConfig().getGameTps();

            // ВАЖНО использовать здесь именно scheduleAtFixedRate, чтобы сервер "изо всех сил"
            // старался придерживаться указанного значения TPS. При обычном schedule сервер будет
            // тикать ЗНАЧИТЕЛЬНО реже (например, ~21 раз в секунду вместо указанных 25), что в
            // случае игровых обновлений совершенно недопустимо. У scheduleAtFixedRate есть свой
            // недостаток: иногда фактический TPS выходит немного выше запрошенного (например,
            // 25.005 раз вместо 25 ровно) - но это несущественно, и с этим точно можно жить.
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (gameStopped)
                        // Не делаем return, чтобы успеть совершить последнее обновление,
                        // чтобы разослать игрокам все необходимые пакеты (о завершении).
                        cancel();

                    update();
                }
            }, tickPeriod, tickPeriod);
        }
    }

    private void gameEnded() {
        synchronized (lock) {
            log.info("Ending game in lobby {} ({} players).", lobbyId, players.size());
            srv.getGameServer().unregisterActiveGameLobby(this);
            // TODO: 16.05.2021 завершающие пакеты?
        }
    }

    public void endGame() {
        synchronized (lock) {
            if (!gameStopped) {
                gameStopped = true;
                gameEnded();
            }
        }
    }

    public void enqueuePlayerInputs(int playerId, int sequence, long inputsBitfield) {
        synchronized (lock) {
            EntityPlayer player = getPlayer(playerId);

            if (player != null)
                player.enqueuePlayerInputs(sequence, inputsBitfield);
        }
    }

    public void broadcastEntitySpawn(@NonNull Entity spawnedEntity) {
        synchronized (lock) {
            int                 entityType = spawnedEntity.getEntityType ();
            int                 entityId   = spawnedEntity.getEntityId   ();
            Map<String, String> entityData = spawnedEntity.formEntityData();

            players.forEach(player -> NetworkService
                    .spawnEntity(player.getVirCon(), entityType, entityId, entityData));
        }
    }

    public void setWorld(int dimension, @NonNull World world) {
        synchronized (lock) {
            dimensions.put(dimension, world);
        }
    }

    public World getWorld(int dimension) {
        synchronized (lock) {
            return dimensions.get(dimension);
        }
    }

    /**
     * Перемещает указанную сущность из её текущего мира в новый, указанный.
     *
     * Для удаления используется метод despawnEntity. Это означает, что все
     * игроки, находящиеся в текущем мире сущности (в котором она находится
     * на момент удаления), будут оповещены об удалении сущности из их мира.
     *
     * С другой стороны, при перемещении используется прямое добавление сущности
     * в новый мир. Это означает, что вызвавший этот метод сам несёт ответственность
     * за уведомление игроков в новом мире (указанном мире, куда будет перенесена
     * сущность) о добавлении новой сущности в их мир.
     */
    public void transitEntity(@NonNull Entity entity, @NonNull World newWorld) {
        synchronized (lock) {
            if (entity.getCurrentDimension() != newWorld.getDimension()) {
                despawnEntity(entity);
                entity.setCurrentDimension(newWorld.getDimension());
                newWorld.addEntity(entity);
            }
        }
    }

    /**
     * Удаляет указанную сущность из её текущего измерения и уведомляет всех
     * игроков в мире, в котором она находилась, об удалении этой сущности из
     * мира. Кроме того, сбрасывает текущее измерение этой сущности на ноль.
     */
    public void despawnEntity(@NonNull Entity entity) {
        synchronized (lock) {
            World currentWorld = getWorld(entity.getCurrentDimension());

            if (currentWorld != null) {
                // Удаляем сущность из её текущего мира.
                if (currentWorld.removeEntity(entity)) {
                    // Оповещаем всех игроков в старом мире этой сущности об её удалении из этого мира.
                    players.stream()
                            .filter(player ->
                                    player.getCurrentDimension() == entity.getCurrentDimension())
                            .forEach(player ->
                                    NetworkService.despawnEntity(player.getVirCon(), entity.getEntityId()));
                }

                // Сбрасываем текущее измерение этой сущности на ноль.
                entity.setCurrentDimension(0);
            }
        }
    }

    /**
     * Этот update() ПОЛНОСТЬЮ заменяет GameServer#update() для
     * виртуальных соединений игроков, находящихся в игре (PLAY).
     *
     * @see GameServer#update()
     */
    @SuppressWarnings ("JavadocReference")
    private void update() {
        synchronized (lock) {
            currentTick.incrementAndGet();
            tpsMeter.onUpdate();

            srv.getVirConManager().flushAllReceiveQueues(); // обрабатываем пакеты, полученные от игроков
            updateGameState();                              // выполняем обновление (и, м.б., ставим на отправку пакеты)
            srv.getVirConManager().flushAllSendQueues();    // отправляем пакеты, поставленые в очередь после обновления
        }
    }

    private void updateGameState() {
        sendPings   (); // для поддержания соединения с клиентами
        updateWorlds(); // обновляем всё, что происходит во всех измерениях
    }

    private void sendPings() {
        long curr = currentTick .get();
        long last = lastPingTick.get();

        if ((curr - last) > srv.getConfig().getPlayPingPeriodTicks()) {
            // Пингуем клиентов, находящихся в игре, причём именно в ЭТОЙ комнате.
            srv.getVirConManager().pingThose(virCon -> virCon.getGameLobby() == this);
            lastPingTick.set(curr);
        }
    }

    private void updateWorlds() {
        dimensions.values().forEach(world -> {
            try {
                // Обновляем всё, что происходит в этом измерении.
                world.update();

                // Рассылаем всем игрокам в этом измерении обновлённые состояния сущностей (Entity) в нём.
                world.forEachEntity(
                        entity -> players.forEach(
                                player -> NetworkService.updateEntityPosition(
                                        player.getVirCon(),
                                        player.getNewestAppliedPacketSequence(),
                                        entity.getEntityId(),
                                        entity.getPosX(),
                                        entity.getPosY(),
                                        entity.getFaceAngle()
                                )
                        )
                );
            } catch (Exception ex) {
                log.error("Unhandled exception during game state update in dimension {} of lobby {}:",
                        world.getDimension(), lobbyId, ex);
            }
        });
    }

}

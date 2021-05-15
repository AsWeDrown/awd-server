package gg.aswedrown.game;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.World;
import gg.aswedrown.net.NetworkService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ActiveGameLobby {

    @Getter
    private final int lobbyId;

    @NonNull
    private final Collection<EntityPlayer> players;

    private final Map<Integer, World> dimensions = new HashMap<>();

    @Getter
    private volatile boolean gameBegun;

    public EntityPlayer getPlayer(int playerId) {
        return players.stream()
                .filter(player -> player.getPlayerId() == playerId)
                .findAny()
                .orElse(null);
    }

    public void playerLoadedWorld(int playerId) {
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

    public void playerJoinedWorld(int playerId) {
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

    /**
     * Запускает игру в этой комнате фактически. Выполняется только
     * после получения пакета JoinWorldComplete от всех игроков в комнате.
     */
    private void beginGame() {
        log.info("The game in lobby {} has begun ({} players).", lobbyId, players.size());

        // Уведомляем всех игроков о спавне.. всех игроков.
        // (Фактический спавн (на сервере) каждого игрока произошёл ранее - завершении загрузки им мира.)
        players.forEach(this::broadcastEntitySpawn);

        // Помечаем комнату как комнату, в которой должны происходить игровые обновления (тики).
        gameBegun = true;
    }

    public void updatePlayerInputs(int playerId, long inputsBitfield) {
        EntityPlayer player = getPlayer(playerId);

        if (player != null)
            player.updatePlayerInputs(inputsBitfield);
    }

    public void broadcastEntitySpawn(@NonNull Entity spawnedEntity) {
        int                 entityType = spawnedEntity.getEntityType ();
        int                 entityId   = spawnedEntity.getEntityId   ();
        Map<String, String> entityData = spawnedEntity.formEntityData();

        players.forEach(player -> NetworkService
                .spawnEntity(player.getVirCon(), entityType, entityId, entityData));
    }

    public void setWorld(int dimension, @NonNull World world) {
        dimensions.put(dimension, world);
    }

    public World getWorld(int dimension) {
        return dimensions.get(dimension);
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
        if (entity.getCurrentDimension() != newWorld.getDimension()) {
            despawnEntity(entity);
            entity.setCurrentDimension(newWorld.getDimension());
            newWorld.addEntity(entity);
        }
    }

    /**
     * Удаляет указанную сущность из её текущего измерения и уведомляет всех
     * игроков в мире, в котором она находилась, об удалении этой сущности из
     * мира. Кроме того, сбрасывает текущее измерение этой сущности на ноль.
     */
    public void despawnEntity(@NonNull Entity entity) {
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

    void update() throws Exception {
        updateWorlds(); // обновляем всё, что происходит во всех измерениях
    }

    private void updateWorlds() {
        dimensions.values().forEach(world -> {
            try {
                // Обновляем всё, что происходит в этом измерении.
                world.update();
            } catch (Exception ex) {
                log.error("Unhandled exception during game state update in dimension {} of lobby {}:",
                        lobbyId, world.getDimension(), ex);
            }
        });
    }

}

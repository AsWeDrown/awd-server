package gg.aswedrown.game.world;

import gg.aswedrown.game.FatalGameException;
import gg.aswedrown.game.entity.Entity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class World {

    private final Object lock = new Object();

    @Getter
    private final int lobbyId;

    @Getter
    private final int dimension;

    private final Collection<Entity> entities = new ArrayList<>();

    private final AtomicInteger lastEntityId = new AtomicInteger(0);

    @Getter
    private WorldData worldData;

    private volatile boolean loaded;

    public void load() throws FatalGameException {
        if (loaded) {
            log.warn("World load() called twice");
            return;
        }

        loaded = true;

        WorldLoader worldLoader = new WorldLoader(dimension);
        worldData = worldLoader.loadWorld();

        if (worldLoader.getLoadStatus() != WorldLoader.WorldLoadStatus.LOADED)
            throw new FatalGameException("failed to load world (dimension " + dimension + ")");
    }

    public void update() {
        synchronized (lock) {
            entities.forEach(entity -> {
                try {
                    entity.update(this);
                } catch (Exception ex) {
                    log.error("Unhandled exception during game state update of entity {} in dimension {} of lobby {}:",
                            entity.getEntityId(), lobbyId, dimension, ex);
                }
            });
        }
    }

    public void forEachEntity(@NonNull Consumer<? super Entity> consumer) {
        synchronized (lock) {
            entities.forEach(consumer);
        }
    }

    public Entity getEntityById(int entityId) {
        synchronized (lock) {
            return entities.stream()
                    .filter(entity -> entity.getEntityId() == entityId)
                    .findAny()
                    .orElse(null);
        }
    }

    private int nextEntityId() {
        return lastEntityId.incrementAndGet();
    }

    /**
     * Присваивает указанной сущности следующий свободный entity ID в этом
     * измерении, устанавливает её текущее измерение на номер этого измерения
     * и добавляет её в список сущностей этого измерения.
     */
    public void addEntity(@NonNull Entity entity) {
        synchronized (lock) {
            entity.setCurrentDimension(dimension);
            entity.setEntityId(nextEntityId());
            entities.add(entity);
        }
    }

    /**
     * Сбрасывает entity ID и текущее измерение указанной сущности
     * и удаляет её из списка сущностей этого измерения, если возможно.
     *
     * @return true, если эта сущность действительно была в этом мире и была успешно удалена,
     *         false в противном случае.
     */
    public boolean removeEntity(@NonNull Entity entity) {
        synchronized (lock) {
            entity.setCurrentDimension(0);
            entity.setEntityId(0);
            return entities.remove(entity);
        }
    }

    /**
     * Вычисляет значение X, в которое может переместиться указанная сущность в этом мире,
     * максимально близкое к указанному (желаемому). Для этого учитывается "начинка" мира
     * (окружающие сущность тайлы и прочее), а также размеры и текущая позиция сущности.
     *
     * @param entity сущность, которая хочет передвинуться по X.
     * @param destWorldX желаемая координата X в мире.
     *
     * @return координата X в мире, в которую указанная сущность может (имеет право) передвинуться
     *         с учётом различных законов физики и взаимодействий, наиболее близкая к желаемому X.
     */
    public float pathTowardsX(@NonNull Entity entity, float destWorldX) {
        float entityX = entity.getPosX();
        float dx = destWorldX - entityX;

        if (dx == 0.0f)
            return 0.0f;

        BoundingBox entityBb = entity.getBoundingBox();

        int leftmostTileX   = (int) Math.floor(entityBb.getMinX());
        int rightmostTileX  = (int) Math.ceil (entityBb.getMaxX());
        int topmostTileY    = (int) Math.floor(entityBb.getMinY());
        int bottommostTileY = (int) Math.ceil (entityBb.getMaxY());

        boolean pathingRight = dx > 0.0f;
        int tileX = pathingRight ? rightmostTileX : leftmostTileX;
        
        for (int tileY = topmostTileY; tileY < bottommostTileY; tileY++) {
            TileBlock nearbyTile = getTileAt(tileX, tileY);
            BoundingBox nearbyTileBb = nearbyTile.getBoundingBox();

            if (nearbyTile.isSolid() && entityBb.intersectsWith(nearbyTileBb))
                return pathingRight
                        ? entityX + (nearbyTileBb.getMinX() - entityBb.getMaxX())
                        : entityX - (entityBb.getMinX() - nearbyTileBb.getMaxX());
        }

        return destWorldX;
    }

    public TileBlock getTileAt(int posX, int posY) {
        return worldData.tiles.stream()
                .filter(tile -> tile.posX == posX && tile.posY == posY)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "tile position out of world range: (" + posX + ", " + posY + "); expected " +
                                "x in range [0; " + worldData.width + "), " +
                                "y in range [0; " + worldData.height + ")"));
    }

}

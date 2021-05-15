package gg.aswedrown.game.world;

import gg.aswedrown.game.entity.Entity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

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

    public void update() {
        synchronized (lock) {
            entities.forEach(entity -> {
                try {
                    entity.update();
                } catch (Exception ex) {
                    log.error("Unhandled exception during game state update of entity {} in dimension {} of lobby {}:",
                            entity.getEntityId(), lobbyId, dimension, ex);
                }
            });
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

}

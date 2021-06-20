package gg.aswedrown.game.event;

import gg.aswedrown.game.entity.LivingEntity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityDeathEvent implements GameEvent {

    @NonNull @Getter
    private final LivingEntity entity;

    @Override
    public void dispatch(@NonNull GameEventListener listener) throws Exception {
        listener.onEntityDeath(this);
    }

}

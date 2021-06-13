package gg.aswedrown.game.event.listener;

import gg.aswedrown.game.event.GameEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class GameEventListener<EventType extends GameEvent> {

    public final void onEvent(@NonNull Object event) {
        try {
            onEvent((EventType) event);
        } catch (Exception ex) {
            log.error("Unhandled exception in game event listener {}: failed to process event of type {}",
                    getClass().getName(), event.getClass().getName(), ex);
        }
    }

    protected abstract void onEvent(EventType event) throws Exception;

}

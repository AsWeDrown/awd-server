package gg.aswedrown.game.event;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;

@Slf4j
public class EventDispatcher {

    private final Object lock = new Object();

    private final Collection<GameEventListener> listeners = new HashSet<>();

    public void registerListener(@NonNull GameEventListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(@NonNull GameEventListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    public void dispatchEvent(@NonNull GameEvent event) {
        synchronized (lock) {
            for (GameEventListener listener : listeners) {
                try {
                    event.dispatch(listener);
                } catch (Exception ex) {
                    log.error("Unhandled exception occurred " +
                              "while dispatching event of type {} to listener {}",
                            event.getClass().getSimpleName(), listener.getClass().getName(), ex);
                }
            }
        }
    }

}

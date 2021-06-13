package gg.aswedrown.game.event;

import gg.aswedrown.game.event.listener.GameEventListener;
import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EventDispatcher {

    private final Map<Class<? extends GameEvent>,
            GameEventListener<? extends GameEvent>> listeners = new HashMap<>();

    private final AwdServer srv;

    public EventDispatcher(@NonNull AwdServer srv) {
        this.srv = srv;
        wirePacketListeners();
    }

    /**
     * Автоматически регистрирует все обработчики игровых событий из пакета/папки
     *
     *     gg.aswedrown.game.event.listener
     *
     * (при условии, что они "помечены" аннотацией @RegisterGameEventListener).
     */
    private void wirePacketListeners() {
        Reflections reflections = new Reflections("gg.aswedrown.game.event.listener");

        for (Class<?> clazz : reflections.getTypesAnnotatedWith(RegisterGameEventListener.class)) {
            try {
                // Валидация.
                if (!GameEventListener.class.isAssignableFrom(clazz))
                    throw new ClassCastException(
                            "class " + clazz.getName() + " is annotated with @RegisterGameEventListener, " +
                                    "but does not inherit from " + GameEventListener.class.getName());

                // Инициализация.
                RegisterGameEventListener anno = clazz.getAnnotation(RegisterGameEventListener.class);
                Class<? extends GameEvent> eventType = anno.value();
                String eventTypeName = eventType.getSimpleName();
                Constructor<GameEventListener<? extends GameEvent>> constructor
                        = (Constructor<GameEventListener<? extends GameEvent>>)
                                clazz.getDeclaredConstructor(AwdServer.class);
                GameEventListener<? extends GameEvent> listener = constructor.newInstance(srv);

                // Регистрация.
                GameEventListener<? extends GameEvent> otherListener = listeners.get(eventType);

                if (otherListener != null)
                    throw new IllegalStateException(
                            "multiple classes attempt to listen for game events of type " + eventTypeName
                                    + ": " + otherListener.getClass().getName() + " and " + clazz.getName());

                log.info("Wired game events of type {} to {}.", eventTypeName, clazz.getSimpleName());
                listeners.put(eventType, listener);
            } catch (Exception ex) {
                log.error("Ignoring misconfigured game event listener: {}.", ex.toString());
            }
        }
    }

    public void dispatchEvent(@NonNull GameEvent event) {
        GameEventListener<? extends GameEvent> listener = listeners.get(event.getClass());

        if (listener != null)
            listener.onEvent(event);
        else
            log.error("Failed to dispatch an event of type {}: " +
                    "it is not wired to any listener",
                    event.getClass().getSimpleName());
    }

}

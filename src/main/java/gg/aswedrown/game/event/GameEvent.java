package gg.aswedrown.game.event;

import lombok.NonNull;

public interface GameEvent {

    void dispatch(@NonNull GameEventListener listener) throws Exception;

}

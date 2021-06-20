package gg.aswedrown.game.event;

import gg.aswedrown.game.world.World;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorldUpdateEvent implements GameEvent {

    @NonNull @Getter
    private final World world;

    @Override
    public void dispatch(@NonNull GameEventListener listener) throws Exception {
        listener.onWorldUpdate(this);
    }

}

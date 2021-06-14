package gg.aswedrown.game.event;

import gg.aswedrown.game.entity.EntityPlayer;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PlayerEvent implements GameEvent {

    @Getter @NonNull
    protected final EntityPlayer player;

}

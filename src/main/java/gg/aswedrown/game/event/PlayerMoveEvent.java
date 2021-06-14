package gg.aswedrown.game.event;

import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.Location;
import lombok.Getter;
import lombok.NonNull;

public class PlayerMoveEvent extends PlayerEvent {

    @Getter
    private final Location from, to;

    public PlayerMoveEvent(@NonNull EntityPlayer player,
                           @NonNull Location from, @NonNull Location to) {
        super(player);

        this.from = from;
        this.to = to;
    }

    @Override
    public void dispatch(@NonNull GameEventListener listener) throws Exception {
        listener.onPlayerMove(this);
    }

}

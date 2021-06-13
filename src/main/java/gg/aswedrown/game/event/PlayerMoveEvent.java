package gg.aswedrown.game.event;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.Location;
import lombok.Getter;
import lombok.NonNull;

public class PlayerMoveEvent extends PlayerEvent {

    @Getter
    private final Location from, to;

    public PlayerMoveEvent(ActiveGameLobby lobby, @NonNull EntityPlayer player,
                           @NonNull Location from, @NonNull Location to) {
        super(lobby, player);

        this.from = from;
        this.to = to;
    }

}

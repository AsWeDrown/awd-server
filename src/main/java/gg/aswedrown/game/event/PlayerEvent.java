package gg.aswedrown.game.event;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.entity.EntityPlayer;
import lombok.Getter;
import lombok.NonNull;

public class PlayerEvent extends GameEvent {

    @Getter
    private final EntityPlayer player;

    public PlayerEvent(ActiveGameLobby lobby, @NonNull EntityPlayer player) {
        super(lobby);
        this.player = player;
    }

}

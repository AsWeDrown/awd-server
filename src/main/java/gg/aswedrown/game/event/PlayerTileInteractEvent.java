package gg.aswedrown.game.event;

import gg.aswedrown.game.entity.EntityPlayer;
import gg.aswedrown.game.world.TileBlock;
import lombok.Getter;
import lombok.NonNull;

public class PlayerTileInteractEvent extends PlayerEvent {

    @Getter
    private final TileBlock tile;

    @Getter
    private final int command;

    public PlayerTileInteractEvent(@NonNull EntityPlayer player, @NonNull TileBlock tile, int command) {
        super(player);

        this.tile = tile;
        this.command = command;
    }

    @Override
    public void dispatch(@NonNull GameEventListener listener) throws Exception {
        listener.onPlayerTileInteract(this);
    }

    public static final class Command {
        private Command() {}

        public static final int LEFT_CLICK  = 1;
        public static final int RIGHT_CLICK = 2;
    }

}

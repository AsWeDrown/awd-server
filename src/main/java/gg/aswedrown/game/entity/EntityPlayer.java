package gg.aswedrown.game.entity;

import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class EntityPlayer extends LivingEntity {

    @Getter @Setter
    private volatile boolean ready,       // true - этот игрок уже загрузил измерение и готов к игре
                             joinedWorld; // true - этот игрок уже начал отображать у себя на экране игровой процесс

    @Getter
    private final int playerId; // локальный ID игрока в комнате

    @Getter
    private final String playerName; // имя игрока в комнате

    @Getter
    private final int character; // персонаж игрока

    @Getter
    private final VirtualConnection virCon; // виртуальное соединение, связанное с этим игроком

    private final PlayerInputs playerInputs = new PlayerInputs(); // данные о базовом игровом вводе игрока

    public EntityPlayer(int playerId, @NonNull String playerName,
                        int character, @NonNull VirtualConnection virCon) {
        super(Entities.TYPE_ENTITY_PLAYER);

        this.playerId = playerId;
        this.playerName = playerName;
        this.character = character;
        this.virCon = virCon;

        // TODO: 15.05.2021 remove
        posX = 65.0f;
        posY = 40.0f;
        // TODO: 15.05.2021 remove
    }

    @Override
    public void update() {
        if (playerInputs.movingLeft)
            posX -= 0.095f;

        if (playerInputs.movingRight)
            posX += 0.095f;
    }

    @Override
    public Map<String, String> formEntityData() {
        Map<String, String> data = new HashMap<>();
        data.put("player_id", Integer.toString(playerId));

        return data;
    }

    public void updatePlayerInputs(long inputsBitfield) {
        playerInputs.movingLeft  = (inputsBitfield & PlayerInputs.BIT_MOVING_LEFT ) != 0;
        playerInputs.movingRight = (inputsBitfield & PlayerInputs.BIT_MOVING_RIGHT) != 0;
    }

    private static final class PlayerInputs {
        private static final long BIT_MOVING_LEFT  = 1, // 2^0
                                  BIT_MOVING_RIGHT = 2; // 2^1

        private boolean movingLeft,
                        movingRight;
    }

}

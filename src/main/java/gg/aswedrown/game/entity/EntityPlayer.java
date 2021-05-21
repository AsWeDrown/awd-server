package gg.aswedrown.game.entity;

import gg.aswedrown.net.SequenceNumberMath;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class EntityPlayer extends LivingEntity {

    private final Object lock = new Object();

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
    
    private final Queue<PlayerInputs> playerInputsQueue = new PriorityQueue<>();

    private final AtomicInteger newestAppliedPacketSequence = new AtomicInteger(0);

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
        synchronized (lock) {
            if (!playerInputsQueue.isEmpty()) {
                PlayerInputs oldestInputs = playerInputsQueue.poll();
                newestAppliedPacketSequence.set(oldestInputs.getSequence());
                oldestInputs.apply(this);
            }
        }
    }

    @Override
    public Map<String, String> formEntityData() {
        Map<String, String> data = new HashMap<>();
        data.put("player_id", Integer.toString(playerId));

        return data;
    }

    public void enqueuePlayerInputs(int sequence, long inputsBitfield) {
        synchronized (lock) {
            if (playerInputsQueue.size() == AwdServer.getServer().getPhysics().getMaxLag()) {
                playerInputsQueue.poll();
                log.warn("Player {}#{} appears to be lagging heavily: dropped PlayerInputs #{} (0b{}).",
                        playerName, playerId, sequence, Long.toString(inputsBitfield, 2));
            }
			
            playerInputsQueue.add(new PlayerInputs(sequence, inputsBitfield));
        }
    }

    public int getNewestAppliedPacketSequence() {
        synchronized (lock) {
            return newestAppliedPacketSequence.get();
        }
    }

    @RequiredArgsConstructor
    private static final class PlayerInputs implements Comparable<PlayerInputs> {
        private static final long INPUT_MOVING_LEFT  = 0b1,
                                  INPUT_MOVING_RIGHT = 0b10;

        @Getter (AccessLevel.PRIVATE)
        private final int sequence;

        private final long inputsBitfield;

        private boolean movingLeft() {
            return (inputsBitfield & INPUT_MOVING_LEFT) != 0;
        }

        private boolean movingRight() {
            return (inputsBitfield & INPUT_MOVING_RIGHT) != 0;
        }

        private void apply(EntityPlayer player) {
            if (movingLeft()) {
                player.posX -= AwdServer.getServer().getPhysics().getPlayerBaseHorMs();
                player.faceAngle = 180.0f; // лицом влево
            }

            if (movingRight()) {
                player.posX += AwdServer.getServer().getPhysics().getPlayerBaseHorMs();
                player.faceAngle = 0.0f; // лицом вправо
            }
        }

        @Override
        public int compareTo(@NonNull PlayerInputs other) {
            if (sequence == other.sequence)
                return 0;  // эти (this и other) PlayerInputs одинаковой "новизны"
            else if (SequenceNumberMath.isMoreRecent(sequence, other.sequence))
                return 1;  // этот (this) PlayerInputs "новее" указанного (other)
            else
                return -1; // этот (this) PlayerInputs "старее" указанного (other)
        }
    }

}

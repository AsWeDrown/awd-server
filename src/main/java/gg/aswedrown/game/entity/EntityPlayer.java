package gg.aswedrown.game.entity;

import gg.aswedrown.game.ActiveGameLobby;
import gg.aswedrown.game.event.GameEvent;
import gg.aswedrown.game.event.PlayerMoveEvent;
import gg.aswedrown.game.world.Location;
import gg.aswedrown.game.world.TerrainControls;
import gg.aswedrown.game.world.TileBlock;
import gg.aswedrown.net.SequenceNumberMath;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class EntityPlayer extends FallableLivingEntity {

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

    private boolean climbing; // карабкается ли игрок прямо сейчас по лестнице? (отключает гравитацию)

    @Getter
    private final VirtualConnection virCon; // виртуальное соединение, связанное с этим игроком
    
    private final Queue<PlayerInputs> playerInputsQueue = new PriorityQueue<>();

    private final AtomicInteger newestAppliedPacketSequence = new AtomicInteger(0);

    @Getter
    private float totalDistanceMoved;

    public EntityPlayer(int playerId, @NonNull String playerName,
                        int character, @NonNull VirtualConnection virCon) {
        super(Entities.EntityPlayer.TYPE);

        this.playerId = playerId;
        this.playerName = playerName;
        this.character = character;
        this.virCon = virCon;

        spriteWidth  = AwdServer.getServer().getPhysics().getBaseEntityPlayerW();
        spriteHeight = AwdServer.getServer().getPhysics().getBaseEntityPlayerH();
    }

    @Override
    public void update(TerrainControls terrainControls) {
        synchronized (lock) {
            if (!playerInputsQueue.isEmpty()) {
                PlayerInputs oldestInputs = playerInputsQueue.poll();
                newestAppliedPacketSequence.set(oldestInputs.getSequence());
                oldestInputs.apply(this, terrainControls);
            }
        }
    }

    @Override
    public Map<String, String> formEntityData() {
        Map<String, String> data = new HashMap<>();
        data.put("player_id", Integer.toString(playerId));
        return data;
    }

    @Override
    public void updateGravity(TerrainControls terrainControls) {
        if (!climbing) super.updateGravity(terrainControls);
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

    public ActiveGameLobby getLobby() {
        return AwdServer.getServer().getGameServer()
                .getActiveGameLobby(virCon.getCurrentlyJoinedLobbyId());
    }

    @RequiredArgsConstructor
    private static final class PlayerInputs implements Comparable<PlayerInputs> {
        private static final long INPUT_MOVING_LEFT  = 0b1,
                                  INPUT_MOVING_RIGHT = 0b10,
                                  INPUT_MOVING_UP    = 0b100,
                                  INPUT_MOVING_DOWN  = 0b1000;

        @Getter (AccessLevel.PRIVATE)
        private final int sequence;

        private final long inputsBitfield;

        private boolean movingLeft() {
            return (inputsBitfield & INPUT_MOVING_LEFT) != 0;
        }

        private boolean movingRight() {
            return (inputsBitfield & INPUT_MOVING_RIGHT) != 0;
        }

        private boolean movingUp() {
            return (inputsBitfield & INPUT_MOVING_UP) != 0;
        }

        private boolean movingDown() {
            return (inputsBitfield & INPUT_MOVING_DOWN) != 0;
        }

        private void apply(EntityPlayer player, TerrainControls terrainControls) {
            Location locFrom = player.takePositionSnapshot();

            float newX = player.posX;
            float newY = player.posY;

            // Вычисляем "желаемую" позицию (куда игрок хочет передвинуться).
            if (movingLeft()) {
                newX -= player.physics.getBaseEntityPlayerMs();
                player.faceAngle = 270.0f; // лицом влево
            }

            if (movingRight()) {
                newX += player.physics.getBaseEntityPlayerMs();
                player.faceAngle = 90.0f; // лицом вправо
            }

            player.climbing = false; // сброс

            if (movingUp() || movingDown()) {
                TileBlock intersectedLadder = terrainControls
                        .getFirstIntersectingTile(player, tile
                                -> tile.handler.isClimbableBy(player));

                if (intersectedLadder != null) {
                    // Игрок действительно находится на лестнице и может карабкаться.
                    float climbDeltaY = player.physics.getBaseEntityPlayerClimbSpeed();

                    if (movingUp())
                        climbDeltaY *= -1.0f; // движемся вверх -> Y уменьшается; вниз - увеличивается

                    newY += climbDeltaY;
                    player.climbing = true;

                    // Сбрасываем гравитацию (вдруг игрок до этого был ей подвержен).
                    player.midairTicks          =    0;
                    player.lastTickFallDistance = 0.0f;
                    player.fallDistance         = 0.0f;
                }
            }

            // Вычисляем "фактическую" позицию (куда игрок может передвинуться, с учётом его "желания").
            player.posX = terrainControls.advanceTowardsXUntilTerrainCollision(player, newX);
            player.posY = terrainControls.advanceTowardsYUntilTerrainCollision(player, newY);

            Location locTo = player.takePositionSnapshot();

            if (!locTo.equals(locFrom)) {
                player.totalDistanceMoved += locTo.distance(locFrom);
                GameEvent event = new PlayerMoveEvent(player, locFrom, locTo);
                player.getLobby().getEventDispatcher().dispatchEvent(event);
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

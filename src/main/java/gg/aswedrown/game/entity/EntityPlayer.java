package gg.aswedrown.game.entity;

import gg.aswedrown.net.SequenceNumberMath;
import gg.aswedrown.server.AwdServer;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityPlayer extends LivingEntity {

    private static final int MAX_PLAYER_ACTIONS_PER_TICK = 3;

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

    private final List<PlayerActions> playerActionsQueue = new ArrayList<>();

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
            applyQueuedPlayerActions();
        }
    }

    private void applyQueuedPlayerActions() {
        if (!playerActionsQueue.isEmpty()) {
            // Сортируем полученные команды по возрастанию порядкового номера пакета.
            // (Нужно, т.к. UDP не даёт никаких гарантий по поводу порядка получения пакетов.)
            playerActionsQueue.sort((o1, o2) -> {
                int seq1 = o1.getSequence();
                int seq2 = o2.getSequence();

                if (seq1 == seq2)
                    return 0; // o1 и o2 одинаково "новы"
                else if (SequenceNumberMath.isMoreRecent(seq1, seq2))
                    return 1; // o1 "новее", чем o2 (o2 "старее", чем o1)
                else
                    return -1; // o1 "старее", чем o2 (o2 "новее", чем o1)
            });

            // Применяем полученные команды в хронологическом порядке.
            playerActionsQueue.forEach(playerActions -> playerActions.apply(this));

            // Обновляем номер последней УЧТЁННОЙ (ОБРАБОТАННОЙ, ПРИМЕНЁННОЙ) команды.
            // Ввиду сортировки выше, этот номер будет иметь последняя команда в списке.
            newestAppliedPacketSequence.set(
                    playerActionsQueue.get(playerActionsQueue.size() - 1).getSequence());

            // Наконец, очищаем очередь команд для обработки в этом тике.
            playerActionsQueue.clear();
        }
    }

    @Override
    public Map<String, String> formEntityData() {
        Map<String, String> data = new HashMap<>();
        data.put("player_id", Integer.toString(playerId));

        return data;
    }

    public void enqueuePlayerActions(int sequence, long actionsBitfield) {
        synchronized (lock) {
            if (playerActionsQueue.size() == MAX_PLAYER_ACTIONS_PER_TICK)
                playerActionsQueue.remove(0);

            playerActionsQueue.add(new PlayerActions(sequence, actionsBitfield));
        }
    }

    public int getNewestAppliedPacketSequence() {
        synchronized (lock) {
            return newestAppliedPacketSequence.get();
        }
    }

    @RequiredArgsConstructor
    private static final class PlayerActions {
        private static final long ACTION_MOVE_LEFT  = 0b1,
                                  ACTION_MOVE_RIGHT = 0b10;

        @Getter (AccessLevel.PRIVATE)
        private final int sequence;

        private final long actionsBitfield;

        private boolean empty() {
            return actionsBitfield == 0;
        }

        private boolean moveLeft() {
            return (actionsBitfield & ACTION_MOVE_LEFT) != 0;
        }

        private boolean moveRight() {
            return (actionsBitfield & ACTION_MOVE_RIGHT) != 0;
        }

        private void apply(EntityPlayer player) {
            if (moveLeft())
                player.posX -= AwdServer.getServer().getPhysics().getPlayerBaseHorizontalMoveSpeed();

            if (moveRight())
                player.posX += AwdServer.getServer().getPhysics().getPlayerBaseHorizontalMoveSpeed();
        }
    }

}

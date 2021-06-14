package gg.aswedrown.net;

import gg.aswedrown.game.entity.Entity;
import gg.aswedrown.game.entity.Fallable;
import gg.aswedrown.game.quest.Quest;
import gg.aswedrown.server.data.lobby.LobbyManager;
import gg.aswedrown.server.vircon.VirtualConnection;
import lombok.NonNull;

import java.util.Map;

public class NetworkService {

    private NetworkService() {}

    public static void handshakeResponse(@NonNull VirtualConnection virCon) {
        virCon.sendImportantPacket(HandshakeResponse.newBuilder()
                .setProtocolVersion(PacketManager.PROTOCOL_VERSION)
                .build()
        );
    }

    public static void createLobbyResponse(@NonNull VirtualConnection virCon,
                                           @NonNull LobbyManager.CreationResult result) {
        virCon.sendImportantPacket(CreateLobbyResponse.newBuilder()
                .setLobbyId(result.getLobbyId())
                .setPlayerId(result.getPlayerId())
                .setCharacter(result.getCharacter())
                .build()
        );
    }

    public static void joinLobbyResponse(@NonNull VirtualConnection virCon,
                                         @NonNull LobbyManager.JoinResult result) {
        virCon.sendImportantPacket(JoinLobbyResponse.newBuilder()
                .setPlayerId(result.getPlayerId())
                .setCharacter(result.getCharacter())
                .setHostId(result.getHostId())
                .putAllOthersNames(result.getMembersNames())
                .putAllOthersCharacters(result.getMembersCharacters())
                .build()
        );
    }

    public static void leaveLobbyResponse(@NonNull VirtualConnection virCon, int result) {
        virCon.sendImportantPacket(LeaveLobbyResponse.newBuilder()
                .setStatusCode(result)
                .build()
        );
    }

    public static void ping(@NonNull VirtualConnection virCon, int testId, int rtt) {
        virCon.sendPacket(Ping.newBuilder() // Для пакетов Ping/Pong используем мгновенные отправку/получение.
                .setTestId(testId)
                .setRtt(rtt)
                .build()
        );
    }

    public static void updatedMembersList(@NonNull VirtualConnection virCon,
                                          @NonNull Map<Integer, String> newAllNames,
                                          @NonNull Map<Integer, Integer> newAllCharacters) {
        virCon.sendImportantPacket(UpdatedMembersList.newBuilder()
                .putAllNewAllNames(newAllNames)
                .putAllNewAllCharacters(newAllCharacters)
                .build()
        );
    }

    public static void kickedFromLobby(@NonNull VirtualConnection virCon, int reason) {
        virCon.sendImportantPacket(KickedFromLobby.newBuilder()
                .setReason(reason)
                .build()
        );
    }

    public static void beginPlayStateResponse(@NonNull VirtualConnection virCon, int result) {
        virCon.sendImportantPacket(BeginPlayStateResponse.newBuilder()
                .setStatusCode(result)
                .build()
        );
    }

    public static void updateDimensionCommand(@NonNull VirtualConnection virCon, int dimension) {
        virCon.sendImportantPacket(UpdateDimensionCommand.newBuilder()
                .setDimension(dimension)
                .build()
        );
    }

    public static void joinWorldCommand(@NonNull VirtualConnection virCon) {
        virCon.sendImportantPacket(JoinWorldCommand.newBuilder()
                .build()
        );
    }

    public static void spawnEntity(@NonNull VirtualConnection virCon,
                                   int entityType, int entityId, @NonNull Map<String, String> entityData) {
        virCon.sendImportantPacket(SpawnEntity.newBuilder()
                .setEntityType(entityType)
                .setEntityId(entityId)
                .putAllEntityData(entityData)
                .build()
        );
    }

    public static void despawnEntity(@NonNull VirtualConnection virCon, int entityId) {
        virCon.sendImportantPacket(DespawnEntity.newBuilder()
                .setEntityId(entityId)
                .build()
        );
    }

    public static void updateEntityPosition(@NonNull VirtualConnection virCon, int ack, @NonNull Entity entity) {
        int   midairTicks          =    0;
        float lastTickFallDistance = 0.0f;
        float fallDistance         = 0.0f;

        if (entity instanceof Fallable) {
            Fallable fallable = (Fallable) entity;

            midairTicks          = fallable.getMidairTicks         ();
            lastTickFallDistance = fallable.getLastTickFallDistance();
            fallDistance         = fallable.getFallDistance        ();
        }

        updateEntityPosition(virCon, ack,
                entity.getEntityId(), entity.getPosX(), entity.getPosY(), entity.getFaceAngle(),
                midairTicks, lastTickFallDistance, fallDistance);
    }

    private static void updateEntityPosition(@NonNull VirtualConnection virCon, int ack,
                                            int entityId, float posX, float posY, float faceAngle,
                                            int midairTicks, float lastTickFallDistance, float fallDistance) {
        // Пакеты PlayerActions обрабатываются в игровом цикле (во время игровых
        // серверных тиков), из-за чего могут возникать ситуации вроде этой:
        //
        // ============================================================================================================
        //
        //     - клиент отправил PlayerActions, seq = #15
        //     - клиент отправил PlayerActions, seq = #16
        //     < . . . >
        //     - сервер получил PlayerActions, seq = #15
        //         * сервер вошёл в synchronized-блок игрока (начало блокировки объекта игрока)
        //         * сервер добавил этот PlayerActions (#15) в очередь на обработку
        //         * сервер вышел из synchronized-блока игрока (конец блокировки объекта игрока)
        //     - сервер получил PlayerActions, seq = #16
        //         * сервер попытался войти в synchronized-блок игрока, но объект уже заблокирован:
        //           в этот момент происходит игровое обновление (серверный тик), которое успело
        //           заблокировать объект игрока до того, как пакет PlayerActions (#16) был добавлен
        //           в очередь на обработку - поток получения пакетов ожидает завершения игрового тика (...)
        //     - происходит очередное обновление сервера (игровой тик) (см. выше)
        //         * сервер вошёл в synchronized-блок игрока (начало блокировки объекта игрока)
        //         * сервер обрабатывает поставленные в очередь пакеты, в данном случае это только PlayerActions (#15)
        //         * сервер обновляет игровое состояние и рассылает его игрокам
        //         * при отправке обновлённого игрового состояния игроку сервер указывает, что последний пакет,
        //           который сервер получил от этого игрока, имеет номер #16, при этом, хоть это и так,
        //           последним пакетом, который сервер успел обработать, является пакет с номером #15 (!!!)
        //         * сервер вышел из synchronized-блока игрока (конец блокировки объекта игрока)
        //     - (...) ожидавший завершения игрового тика поток получения пакетов дожидается своей очереди
        //             на получение возможности блокировки объекта игрока...
        //       ...теперь, наконец,
        //         * сервер вошёл в synchronized-блок игрока (начало блокировки объекта игрока)
        //         * сервер добавил полученный ранее PlayerActions (#16) в очередь на обработку
        //         * сервер вышел из synchronized-блока игрока (конец блокировки объекта игрока)
        //     < . . . >
        //     - клиент получает от сервера обновлённое состояние игры, в котором сервер указал,
        //       что номер последнего пакета клиента, который он ПОЛУЧИЛ (НО НЕ УЧЁЛ - ОДНАКО КЛИЕНТ
        //       ЭТОГО НЕ ЗНАЕТ) - #16
        //     - клиент думает, что состояние сервера совпадает с локальным состоянием, и не применяет
        //       никаких локальных мер по его корректировке (таких как повторное локальное применение
        //       ввода), хотя на самом же деле сервер не успел учесть в этом (полученном только что)
        //       состоянии пакет #16 - последним был пакет #15 (НО КЛИЕНТ ЭТОГО НЕ ЗНАЕТ)
        //     - клиент дёргается/глючит/телепортируется (потому что фактически был обманут сервером)
        //
        // ============================================================================================================
        //
        // Для того, чтобы избавиться от этого "обмана" клиента, мы в пункте, помеченном (!!!), подменяем
        // номер последнего ПОЛУЧЕННОГО от клиента пакета на номер последнего УЧТЁННОГО (ПРИМЕНЁННОГО) пакета.
        // Это позволит клиенту точно понять, что у него с сервером есть небольшой рассинхрон (что совершенно
        // нормально), и, соответственно, скорректировать соответствующим образом своё локальное состояние,
        // благодаря чему пользователь (игрок) будет видеть плавный геймплей без подёргиваний/глюков/телепортаций.
        virCon.sendPacket(UpdateEntityPosition.newBuilder()
                .setEntityId(entityId)
                .setPosX(posX)
                .setPosY(posY)
                .setFaceAngle(faceAngle)
                .setMidairTicks(midairTicks)
                .setLastTickFallDistance(lastTickFallDistance)
                .setFallDistance(fallDistance)
                .build(),
                ack
        );
    }

    public static void beginQuest(@NonNull VirtualConnection virCon, @NonNull Quest quest) {
        virCon.sendImportantPacket(BeginQuest.newBuilder()
                .setQuestId(quest.getId())
                .setQuestType(quest.getType())
                .build()
        );
    }

    public static void advanceQuest(@NonNull VirtualConnection virCon, @NonNull Quest quest) {
        virCon.sendPacket(AdvanceQuest.newBuilder()
                .setQuestId(quest.getId())
                .setProgress(quest.getProgress())
                .build()
        );
    }

    public static void endQuest(@NonNull VirtualConnection virCon, @NonNull Quest quest) {
        virCon.sendImportantPacket(EndQuest.newBuilder()
                .setQuestId(quest.getId())
                .setStatus(quest.getState())
                .build()
        );
    }

}

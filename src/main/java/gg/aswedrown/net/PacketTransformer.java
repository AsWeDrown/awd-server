package gg.aswedrown.net;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   TODO: при добавлении в протокол новых пакетов ОБЯЗАТЕЛЬНО ДОБАВЛЯТЬ ИХ СЮДА!
 *         Для добавления использовать утилиту-генератор кода awd-ptrans-codegen.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
public final class PacketTransformer {

    private static final Map<Class<? extends Message>, PacketWrapper.PacketCase>
            packetCaseNameMapping = new ConcurrentHashMap<>();

    private PacketTransformer() {}

    private static String camelToSnakeUpper(String camel) {
        // "HandshakeRequest" --> "HANDSHAKE_REQUEST"
        StringBuilder snakeUpper = new StringBuilder();
        char[] camelChars = camel.toCharArray();
        boolean firstChar = true;

        for (char ch : camelChars) {
            if (firstChar) {
                firstChar = false;
                snakeUpper.append(ch);
            } else if (Character.isUpperCase(ch))
                snakeUpper.append('_').append(ch);
            else
                snakeUpper.append(Character.toUpperCase(ch));
        }

        return snakeUpper.toString();
    }

    private static PacketWrapper.PacketCase getPacketCase(Class<? extends Message> packetClass) {
        PacketWrapper.PacketCase packetCase = packetCaseNameMapping.get(packetClass);

        if (packetCase == null) { // ленивое вычисление
            packetCase = PacketWrapper.PacketCase.valueOf(
                    camelToSnakeUpper(packetClass.getSimpleName()));
            packetCaseNameMapping.put(packetClass, packetCase);
        }

        return packetCase;
    }

    /**
     * Принимает на вход сам пакет (то, что мы и должны отправить), преобразовывает (обычный cast)
     * к пакету соответствующего типа (напр., Message --> KeepAlive [implements Message]), создаёт
     * обёртку PacketWrapper над преобразованным пакетов (т.е., напр., не над Message, а прямо над
     * KeepAlive), помещая преобразованный пакет в нужное поле в обёртке (в данном примере это через
     * PacketWrapper#setKeepAlive), а затем сериализует полученную обёртку над этим пакетом в "сырой"
     * массив байтов, который уже можно передавать по сети.
     *
     * @see #unwrap(byte[]) для обратного действия.
     */
    public static byte[] wrap(@NonNull Message packet, int sequence, int ack, long ackBitfield) {
        return internalGeneratedWrap(packet, sequence, ack, ackBitfield);
    }

    /**
     * Принимает на вход "сырой" массив байтов, полученный от какого-то клиента по UDP,
     * десериализует этот массив байтов в общую обёртку для всех пакетов, PacketWrapper,
     * определяет тип (в данном случае - enum PacketWrapper.PacketCase) пакета, обёрнутого
     * этим PacketWrapper'ом, и распаковывает сам пакет (то, что мы и должны обработать).
     *
     * @see #wrap(Message, int, int, long) для обратного действия.
     */
    public static UnwrappedPacketData unwrap(@NonNull byte[] rawProto3PacketData) throws InvalidProtocolBufferException {
        if (rawProto3PacketData.length == 0)
            throw new IllegalArgumentException("rawProto3PacketData cannot be empty");

        return internalGeneratedUnwrap(rawProto3PacketData);
    }

    /*

    Код ниже сгенерирован автоматически с помощью утилиты awd-ptrans-codegen.
    Руками не трогать. Не кормить.

     */

    // Сгенерировано с помощью awd-ptrans-codegen. (ОБЯЗАТЕЛЬНО ДОЛЖНО БЫТЬ В ОДНУ СТРОЧКУ - НИЧЕГО НЕ ПЕРЕНОСИТЬ!!!)
    private static byte[] internalGeneratedWrap(Message packet, int sequence, int ack, long ackBitfield) {
        String packetClassNameUpper = packet.getClass().getSimpleName().toUpperCase();
        PacketWrapper.PacketCase packetType = getPacketCase(packet.getClass());

        switch (packetType) {
            case PING:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setPing((Ping) packet)
                        .build()
                        .toByteArray();

            case PONG:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setPong((Pong) packet)
                        .build()
                        .toByteArray();

            case HANDSHAKE_REQUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setHandshakeRequest((HandshakeRequest) packet)
                        .build()
                        .toByteArray();

            case HANDSHAKE_RESPONSE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setHandshakeResponse((HandshakeResponse) packet)
                        .build()
                        .toByteArray();

            case CREATE_LOBBY_REQUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setCreateLobbyRequest((CreateLobbyRequest) packet)
                        .build()
                        .toByteArray();

            case CREATE_LOBBY_RESPONSE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setCreateLobbyResponse((CreateLobbyResponse) packet)
                        .build()
                        .toByteArray();

            case JOIN_LOBBY_REQUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setJoinLobbyRequest((JoinLobbyRequest) packet)
                        .build()
                        .toByteArray();

            case JOIN_LOBBY_RESPONSE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setJoinLobbyResponse((JoinLobbyResponse) packet)
                        .build()
                        .toByteArray();

            case LEAVE_LOBBY_REQUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setLeaveLobbyRequest((LeaveLobbyRequest) packet)
                        .build()
                        .toByteArray();

            case LEAVE_LOBBY_RESPONSE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setLeaveLobbyResponse((LeaveLobbyResponse) packet)
                        .build()
                        .toByteArray();

            case KICKED_FROM_LOBBY:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setKickedFromLobby((KickedFromLobby) packet)
                        .build()
                        .toByteArray();

            case UPDATED_MEMBERS_LIST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setUpdatedMembersList((UpdatedMembersList) packet)
                        .build()
                        .toByteArray();

            case BEGIN_PLAY_STATE_REQUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setBeginPlayStateRequest((BeginPlayStateRequest) packet)
                        .build()
                        .toByteArray();

            case BEGIN_PLAY_STATE_RESPONSE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setBeginPlayStateResponse((BeginPlayStateResponse) packet)
                        .build()
                        .toByteArray();

            case UPDATE_DIMENSION_COMMAND:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setUpdateDimensionCommand((UpdateDimensionCommand) packet)
                        .build()
                        .toByteArray();

            case UPDATE_DIMENSION_COMPLETE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setUpdateDimensionComplete((UpdateDimensionComplete) packet)
                        .build()
                        .toByteArray();

            case JOIN_WORLD_COMMAND:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setJoinWorldCommand((JoinWorldCommand) packet)
                        .build()
                        .toByteArray();

            case JOIN_WORLD_COMPLETE:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setJoinWorldComplete((JoinWorldComplete) packet)
                        .build()
                        .toByteArray();

            case SPAWN_ENTITY:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setSpawnEntity((SpawnEntity) packet)
                        .build()
                        .toByteArray();

            case DESPAWN_ENTITY:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setDespawnEntity((DespawnEntity) packet)
                        .build()
                        .toByteArray();

            case UPDATE_PLAYER_INPUTS:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setUpdatePlayerInputs((UpdatePlayerInputs) packet)
                        .build()
                        .toByteArray();

            case UPDATE_ENTITY_POSITION:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setUpdateEntityPosition((UpdateEntityPosition) packet)
                        .build()
                        .toByteArray();

            case BEGIN_QUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setBeginQuest((BeginQuest) packet)
                        .build()
                        .toByteArray();

            case ADVANCE_QUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setAdvanceQuest((AdvanceQuest) packet)
                        .build()
                        .toByteArray();

            case END_QUEST:
                return PacketWrapper.newBuilder()
                        .setSequence(sequence)
                        .setAck(ack)
                        .setAckBitfield(ackBitfield)
                        .setEndQuest((EndQuest) packet)
                        .build()
                        .toByteArray();

            default:
                // Код "case ..." для пакетов этого типа отсутствует выше.
                // Нужно добавить! (исп. awd-ptrans-codegen)
                throw new RuntimeException("no implemented transformer for packet type "
                        + packetClassNameUpper + " (" + packet.getClass().getName() + ")");
        }
    }

    // Сгенерировано с помощью awd-ptrans-codegen. (ОБЯЗАТЕЛЬНО ДОЛЖНО БЫТЬ В ОДНУ СТРОЧКУ - НИЧЕГО НЕ ПЕРЕНОСИТЬ!!!)
    private static UnwrappedPacketData internalGeneratedUnwrap(byte[] data) throws InvalidProtocolBufferException {
        PacketWrapper wrapper = PacketWrapper.parseFrom(data);

        int  sequence    = wrapper.getSequence();
        int  ack         = wrapper.getAck();
        long ackBitfield = wrapper.getAckBitfield();

        PacketWrapper.PacketCase packetType = wrapper.getPacketCase();

        switch (packetType) {
            case PING:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getPing());

            case PONG:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getPong());

            case HANDSHAKE_REQUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getHandshakeRequest());

            case HANDSHAKE_RESPONSE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getHandshakeResponse());

            case CREATE_LOBBY_REQUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getCreateLobbyRequest());

            case CREATE_LOBBY_RESPONSE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getCreateLobbyResponse());

            case JOIN_LOBBY_REQUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getJoinLobbyRequest());

            case JOIN_LOBBY_RESPONSE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getJoinLobbyResponse());

            case LEAVE_LOBBY_REQUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getLeaveLobbyRequest());

            case LEAVE_LOBBY_RESPONSE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getLeaveLobbyResponse());

            case KICKED_FROM_LOBBY:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getKickedFromLobby());

            case UPDATED_MEMBERS_LIST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getUpdatedMembersList());

            case BEGIN_PLAY_STATE_REQUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getBeginPlayStateRequest());

            case BEGIN_PLAY_STATE_RESPONSE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getBeginPlayStateResponse());

            case UPDATE_DIMENSION_COMMAND:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getUpdateDimensionCommand());

            case UPDATE_DIMENSION_COMPLETE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getUpdateDimensionComplete());

            case JOIN_WORLD_COMMAND:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getJoinWorldCommand());

            case JOIN_WORLD_COMPLETE:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getJoinWorldComplete());

            case SPAWN_ENTITY:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getSpawnEntity());

            case DESPAWN_ENTITY:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getDespawnEntity());

            case UPDATE_PLAYER_INPUTS:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getUpdatePlayerInputs());

            case UPDATE_ENTITY_POSITION:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getUpdateEntityPosition());

            case BEGIN_QUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getBeginQuest());

            case ADVANCE_QUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getAdvanceQuest());

            case END_QUEST:
                return new UnwrappedPacketData(
                        sequence, ack, ackBitfield, packetType, wrapper.getEndQuest());

            default:
                // Неизвестный пакет - он будет проигнорирован (не передан никакому PacketListener'у).
                return null;
        }
    }

}

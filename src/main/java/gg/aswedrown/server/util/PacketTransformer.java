package gg.aswedrown.server.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import gg.aswedrown.net.*;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   TODO: при добавлении в протокол новых пакетов ОБЯЗАТЕЛЬНО ДОБАВЛЯТЬ ИХ СЮДА!
 *         Для добавления использовать утилиту-генератор кода awd-ptrans-codegen.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
public final class PacketTransformer {

    private PacketTransformer() {}

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
    public static byte[] wrap(Message packet) {
        if (packet == null)
            throw new NullPointerException("packet cannot be null");

        return internalGeneratedWrap(packet);
    }

    /**
     * Принимает на вход "сырой" массив байтов, полученный от какого-то клиента по UDP,
     * десериализует этот массив байтов в общую обёртку для всех пакетов, PacketWrapper,
     * определяет тип (в данном случае - enum PacketWrapper.PacketCase) пакета, обёрнутого
     * этим PacketWrapper'ом, и распаковывает сам пакет (то, что мы и должны обработать).
     *
     * @see #wrap(Message) для обратного действия.
     */
    public static UnwrappedPacketData unwrap(byte[] rawProto3PacketData) throws InvalidProtocolBufferException {
        if (rawProto3PacketData == null || rawProto3PacketData.length == 0)
            throw new IllegalArgumentException("rawProto3PacketData cannot be null or empty");

        return internalGeneratedUnwrap(rawProto3PacketData);
    }

    /*

    Код ниже сгенерирован автоматически с помощью утилиты awd-ptrans-codegen.
    Руками не трогать. Не кормить.

     */

    // Сгенерировано с помощью awd-ptrans-codegen.
    private static byte[] internalGeneratedWrap(Message packet) {
        String packetClassNameUpper = packet.getClass().getSimpleName().toUpperCase();
        PacketWrapper.PacketCase packetType;

        try {
            packetType = PacketWrapper.PacketCase.valueOf(packetClassNameUpper);
        } catch (IllegalArgumentException ex) {
            // Тип этого пакета отсутствует в енуме PacketWrapper.PacketCase.
            // Значит, этот пакет не указан в спецификации (packets.proto - message PacketWrapper).
            // Нужно указать! (вручную)
            throw new RuntimeException("illegal packet type: "
                    + packetClassNameUpper + " (" + packet.getClass().getName() + ")");
        }

        switch (packetType) {
            case HANDSHAKEREQUEST:
                return PacketWrapper.newBuilder().setHandshakeRequest(
                        (HandshakeRequest) packet).build().toByteArray();

            case HANDSHAKERESPONSE:
                return PacketWrapper.newBuilder().setHandshakeResponse(
                        (HandshakeResponse) packet).build().toByteArray();

            case CREATELOBBYREQUEST:
                return PacketWrapper.newBuilder().setCreateLobbyRequest(
                        (CreateLobbyRequest) packet).build().toByteArray();

            case CREATELOBBYRESPONSE:
                return PacketWrapper.newBuilder().setCreateLobbyResponse(
                        (CreateLobbyResponse) packet).build().toByteArray();

            case KEEPALIVE:
                return PacketWrapper.newBuilder().setKeepAlive(
                        (KeepAlive) packet).build().toByteArray();

            default:
                // Код "case ..." для пакетов этого типа отсутствует выше.
                // Нужно добавить! (исп. awd-ptrans-codegen)
                throw new RuntimeException("no implemented transformer for packet type "
                        + packetClassNameUpper + " (" + packet.getClass().getName() + ")");
        }
    }

    // Сгенерировано с помощью awd-ptrans-codegen.
    private static UnwrappedPacketData internalGeneratedUnwrap(byte[] data) throws InvalidProtocolBufferException {
        PacketWrapper wrapper = PacketWrapper.parseFrom(data);
        PacketWrapper.PacketCase packetType = wrapper.getPacketCase();

        switch (packetType) {
            case HANDSHAKEREQUEST:
                return new UnwrappedPacketData(packetType, wrapper.getHandshakeRequest());

            case HANDSHAKERESPONSE:
                return new UnwrappedPacketData(packetType, wrapper.getHandshakeResponse());

            case CREATELOBBYREQUEST:
                return new UnwrappedPacketData(packetType, wrapper.getCreateLobbyRequest());

            case CREATELOBBYRESPONSE:
                return new UnwrappedPacketData(packetType, wrapper.getCreateLobbyResponse());

            case KEEPALIVE:
                return new UnwrappedPacketData(packetType, wrapper.getKeepAlive());

            default:
                // Неизвестный пакет - он будет проигнорирован (не передан никакому PacketListener'у).
                return null;
        }
    }

}

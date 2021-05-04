package gg.aswedrown.net;

import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;

/**
 * Хранит содержимое отправленных пакетов (при этом беря НЕ на себя ответственность
 * за управление памятью, занимаемой указателями на пакеты). Используется для
 * временного хранения пакетов в очереди отправленных пакетов на случай, если
 * какой-то пакет нужно будет отправить повторно.
 *
 * Ещё раз: ответственность за высвобождение памяти, занимаемой содержимым оригинального
 * пакета ("delete packet;") лежит на пользователе этого класса. Сам класс никаких активных
 * действий предпринимать в этом плане не будет.
 */
@RequiredArgsConstructor
public class PacketContainer {

    /**
     * Если true, то в случаях, когда будет возникать достаточная уверенность в том,
     * что пакет не был доставлен до цели, будет сконструирован новый пакет, с новыми
     * sequence number и прочими "протоколообразующими" полями, однако с тем же самым
     * содержимым (см. поле packet).
     */
    private final boolean ensureDelivered;

    /**
     * Sequence number, который был у этого пакета при отправке.
     */
    private final int sequence;

    /**
     * Указатель на оригинальное содержимое отправленного пакета.
     */
    private final Message packet;

    public boolean shouldEnsureDelivered() {
        return ensureDelivered;
    }

    public int getOriginalSequence() {
        return sequence;
    }

    public Message getOriginalPacket() {
        return packet;
    }

}

package gg.aswedrown.net;

import com.google.protobuf.Message;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
public final class UnwrappedPacketData {

    private final int sequence, ack;
    private final long ackBitfield;

    @NonNull
    private final PacketWrapper.PacketCase packetType;

    @NonNull
    private final Message packet;

}

package gg.aswedrown.server.util;

import com.google.protobuf.Message;
import gg.aswedrown.net.PacketWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
@Getter
public final class UnwrappedPacketData {

    @NonNull
    private final PacketWrapper.PacketCase packetType;

    @NonNull
    private final Message packet;

}

package gg.aswedrown.server.util;

import com.google.protobuf.Message;
import gg.aswedrown.net.PacketWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
@Getter
public final class UnwrappedPacketData {

    private final PacketWrapper.PacketCase packetType;

    private final Message packet;

}

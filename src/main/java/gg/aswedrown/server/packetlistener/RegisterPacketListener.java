package gg.aswedrown.server.packetlistener;

import gg.aswedrown.net.PacketWrapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target (ElementType.TYPE)
@Retention (RetentionPolicy.RUNTIME)
public @interface RegisterPacketListener {

    PacketWrapper.PacketCase value();

}

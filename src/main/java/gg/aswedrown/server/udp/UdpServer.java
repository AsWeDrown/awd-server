package gg.aswedrown.server.udp;

import lombok.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public interface UdpServer {

    void start() throws SocketException;

    void sendRaw(@NonNull InetAddress targetAddr, @NonNull byte[] data) throws IOException;

    void stop();

}

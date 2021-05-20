package gg.aswedrown.server.vircon;

import gg.aswedrown.server.AwdServer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class VirtualConnectionManager {

    private final AwdServer srv;

    private final Map<InetAddress, VirtualConnection> virConMap = new ConcurrentHashMap<>();

    public int getActiveVirtualConnections() {
        return virConMap.size();
    }

    public int getAuthorizedVirtualConnections() {
        return (int) virConMap.values().stream()
                .filter(VirtualConnection::isAuthorized)
                .count();
    }

    public VirtualConnection resolveVirtualConnection(int lobbyId, int playerId) {
        return virConMap.values().stream()
                .filter(virCon -> virCon.getCurrentlyJoinedLobbyId() == lobbyId
                        && virCon.getCurrentLocalPlayerId() == playerId)
                .findAny()
                .orElse(null);
    }

    public VirtualConnection strictGetVirtualConnection(@NonNull InetAddress addr) {
        return virConMap.get(addr);
    }

    public VirtualConnection getVirtualConnection(@NonNull InetAddress addr) {
        VirtualConnection virCon = virConMap.get(addr);

        if (virCon == null) {
            // Виртуального соединения, связанного с этим IP-адресом, ещё нет. Пытаемся открыть его.
            if (virConMap.size() < srv.getConfig().getUdpMaxVirtualConnections()) {
                // Свободные места ещё есть - открываем.
                log.info("Virtual connection established: {}.", addr.getHostAddress());
                virCon = new VirtualConnection(srv, addr);
                virConMap.put(addr, virCon);
            } else
                // Свободных мест больше нет - возвращаем null (отказываем в соединении).
                log.warn("Virtual connection denied: {} (the server is full).", addr.getHostAddress());
        }

        return virCon;
    }

    private void closeVirtualConnection(@NonNull VirtualConnection virCon) {
        virConMap.remove(virCon.getAddr());

        try {
            virCon.connectionClosed();
        } catch (Exception ex) {
            log.error("Error in connectionClosed({}):", virCon.getAddr().getHostAddress(), ex);
        }
    }

    public long getAverageRtt() {
        return (long) virConMap.values().stream()
                .mapToLong(VirtualConnection::getLastRtt)
                .average()
                .orElse(0.0);
    }

    public void flushAllReceiveQueues() {
        virConMap.values().forEach(VirtualConnection::flushReceiveQueue);
    }

    public void flushAllSendQueues() {
        virConMap.values().forEach(VirtualConnection::flushSendQueue);
    }

    /**
     * Пингует клиентов из списка подключённых, удовлетворяющих указанному,
     * условию а также сразу отключает (закрывает) неактивные соединения.
     */
    public void pingThose(@NonNull Predicate<? super VirtualConnection> pred) {
        // Делаем копию во избежание concurrent mod.
        List<VirtualConnection> closeQueue = new ArrayList<>();

        virConMap.values().stream()
                .filter(pred)
                .filter(VirtualConnection::ping)
                .forEach(closeQueue::add);

        closeQueue.forEach(this::closeVirtualConnection);
    }

}

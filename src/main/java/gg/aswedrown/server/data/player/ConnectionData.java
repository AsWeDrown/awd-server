package gg.aswedrown.server.data.player;

import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@RequiredArgsConstructor
public class ConnectionData {

    private final Object lock = new Object();

    /**
     * Для удобства.
     * Оба устанавливаются в конструкторе.
     */
    private final InetAddress addr;
    private final String addStr;

    /**
     * Время последнего обращения к этому объекту сведений о "соединении".
     * Если с этого момента прошло очень много времени, то, скорее всего, это
     * "соединение" уже не актуально, и эти сведения можно удалять из памяти.
     */
    private long lastAccessTime = System.currentTimeMillis();

    /**
     * ID активной комнаты, которую создал этот игрок,
     * или 0, если он сейчас не имеет активной созданной комнаты.
     *
     * Если это поле = 0, то и currentLocalPlayerId = 0.
     * ОБРАТНОЕ (ДЛЯ != 0) ЗДЕСЬ МОЖЕТ БЫТЬ И НЕ ВЕРНО!
     */
    private int currentlyHostedLobbyId;

    /**
     * ID активной комнаты, в которой сейчас числится этот игрок,
     * или 0, если он сейчас не состоит ни в одной активной комнате.
     *
     * Если это поле = 0, то и currentLocalPlayerId = 0. 
     * Верно и обратное (для != 0).
     */
    private int currentlyJoinedLobbyId;

    /**
     * Локальный ID этого игрока в комнате, в которой он состоит,
     * или 0, если он сейчас не состоит в комнате.
     *
     * Если это поле = 0, то и currentlyJoinedLobbyId = 0. 
     * Верно и обратное (для != 0).
     */
    private int currentLocalPlayerId;

    public InetAddress getAddr() {
        synchronized (lock) {
            resetLastAccessTime();
            return addr;
        }
    }

    public String getAddStr() {
        synchronized (lock) {
            resetLastAccessTime();
            return addStr;
        }
    }

    public long getMillisSinceLastAccess() {
        synchronized (lock) {
            return System.currentTimeMillis() - lastAccessTime;
        }
    }

    public int getCurrentlyHostedLobbyId() {
        synchronized (lock) {
            resetLastAccessTime();
            return currentlyHostedLobbyId;
        }
    }

    public void setCurrentlyHostedLobbyId(int currentlyHostedLobbyId) {
        synchronized (lock) {
            resetLastAccessTime();
            this.currentlyHostedLobbyId = currentlyHostedLobbyId;
        }
    }

    public int getCurrentlyJoinedLobbyId() {
        synchronized (lock) {
            resetLastAccessTime();
            return currentlyJoinedLobbyId;
        }
    }

    public void setCurrentlyJoinedLobbyId(int currentlyJoinedLobbyId) {
        synchronized (lock) {
            resetLastAccessTime();
            this.currentlyJoinedLobbyId = currentlyJoinedLobbyId;
        }
    }

    public int getCurrentLocalPlayerId() {
        synchronized (lock) {
            resetLastAccessTime();
            return currentLocalPlayerId;
        }
    }

    public void setCurrentLocalPlayerId(int currentLocalPlayerId) {
        synchronized (lock) {
            resetLastAccessTime();
            this.currentLocalPlayerId = currentLocalPlayerId;
        }
    }

    private void resetLastAccessTime() {
        synchronized (lock) {
            lastAccessTime = System.currentTimeMillis();
        }
    }

}

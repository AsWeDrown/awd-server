package gg.aswedrown.server.data.lobby;

import lombok.NonNull;
import org.bson.Document;

import java.util.Map;

public interface LobbyRepository {

    void createLobby(int lobbyId, int hostPlayerId, @NonNull String hostPlayerName);

    void deleteLobby(int lobbyId);

    boolean lobbyExists(int lobbyId);

    /**
     * @return true - имя успешно обновлено, false - в этой комнате нет игрока с таким локальным ID.
     */
    boolean updateMemberName(int lobbyId, int playerId, @NonNull String newPlayerName);

    /**
     * @return true - игрок успешно добавлен, false - игрок уже состоит в этой комнате.
     */
    boolean addMember(int lobbyId, int playerId, @NonNull String playerName);

    /**
     * @return true - игрок успешно исключён, false - игрок не состоит в этой комнате.
     */
    boolean removeMember(int lobbyId, int playerId);

    boolean isMemberOf(int lobbyId, int playerId);

    int getHost(int lobbyId);

    Map<String, String> getMembers(int lobbyId);

    int getGameState(int lobbyId);

    void setGameState(int lobbyId, int gameState);

    Document fetchLobbyData(int lobbyId);

}

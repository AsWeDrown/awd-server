package gg.aswedrown.server.data;

public final class DbInfo {

    private DbInfo() {}

    public static final String DATABASE_NAME = "aswedrown";

    public static final class Lobbies {
        private Lobbies() {}

        public static final String COLLECTION_NAME = "lobbies";

        public static final String LOBBY_ID       = "lobbyId";
        public static final String CURRENT_STATE  = "currentState";
        public static final String HOST_PLAYER_ID = "hostPlayerId";
        public static final String MEMBERS        = "members";
    }

}

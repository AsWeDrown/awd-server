package gg.aswedrown.server.data;

public final class DbInfo {

    private DbInfo() {}

    public static final String DATABASE_NAME = "aswedrown";

    /**
     * Данные, связанные с игровыми комнатами.
     */
    public static final class Lobbies {
        private Lobbies() {}

        public static final String COLLECTION_NAME = "lobbies";

        public static final String LOBBY_ID            = "_id"; /* MongoDB ID */
        public static final String CREATION_DATE_TIME  = "creationDateTime";
        public static final String GAME_STATE          = "gameState";
        public static final String HOST_PLAYER_ID      = "hostPlayerId";
        public static final String MEMBERS_NAMES       = "membersNames";
        public static final String MEMBERS_CHARACTERS  = "membersCharacters";
    }

}

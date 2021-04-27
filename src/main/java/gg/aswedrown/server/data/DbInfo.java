package gg.aswedrown.server.data;

public final class DbInfo {

    private DbInfo() {}

    public static final String DATABASE_NAME = "aswedrown";

    /**
     * Данные, связанные с виртуальными соединениями
     * (временно связывают некоторые данные с IP-адресами).
     */
    public static final class VirtualConnections {
        private VirtualConnections() {}

        public static final String COLLECTION_NAME = "virtualConnections";

        public static final String ADDR_STR                       = "_id"; /* Mongo ID */
        public static final String LAST_PACKET_RECEIVED_DATE_TIME = "lastPacketReceivedDateTime";
        public static final String CURRENTLY_HOSTED_LOBBY_ID      = "currentlyHostedLobbyId";
        public static final String CURRENTLY_JOINED_LOBBY_ID      = "currentlyJoinedLobbyId";
        public static final String CURRENT_LOCAL_PLAYER_ID        = "currentLocalPlayerId";
    }

    /**
     * Данные, связанные с игровыми комнатами.
     */
    public static final class Lobbies {
        private Lobbies() {}

        public static final String COLLECTION_NAME = "lobbies";

        public static final String LOBBY_ID            = "_id"; /* Mongo ID */
        public static final String CREATION_DATE_TIME  = "creationDateTime";
        public static final String GAME_STATE          = "gameState";
        public static final String HOST_PLAYER_ID      = "hostPlayerId";
        public static final String MEMBERS             = "members";
    }

}

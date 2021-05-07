package gg.aswedrown.server.data;

public final class Constraints {

    private Constraints() {}

    public static final String PLAYER_NAME_PATTERN = "[a-zA-Z0-9_]{3,15}";

    public static final int MIN_INT32_ID     = 1;
    public static final int MAX_INT32_ID     = Integer.MAX_VALUE - 1;

    public static final int MIN_CHARACTER_ID = 1;
    public static final int MAX_CHARACTER_ID = 4; // всего 4 персонажа (должно быть равно maxLobbySize в конфиге)

}

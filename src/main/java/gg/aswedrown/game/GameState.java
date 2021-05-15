package gg.aswedrown.game;

public final class GameState {

    private GameState() {}

    /**
     * Этап аутентификации/рукопожатия.
     */
    public static final int AUTH        = 0;

    /**
     * Предыгровой этап.
     */
    public static final int LOBBY_STATE = 1;

    /**
     * Игровой этап.
     */
    public static final int PLAY_STATE  = 2;

}

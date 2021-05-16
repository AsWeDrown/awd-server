package gg.aswedrown.game;

import lombok.NonNull;

/**
 * Это исключение - индикатор фатальной ошибки в какой-то важной составляющей сервера.
 * Игровая комната должна сразу же быть расформирована и удалена в случае возникновения
 * ошибки этого типа, т.к. дальнейшее функционирование игры в этой комнате невозможно.
 */
public class FatalGameException extends Exception {

    private static final long serialVersionUID = 8629280744865326948L;

    public FatalGameException(@NonNull String message) {
        super(message);
    }

}

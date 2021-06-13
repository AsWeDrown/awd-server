package gg.aswedrown.game.event;

import gg.aswedrown.game.ActiveGameLobby;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GameEvent {

    @Getter @NonNull
    private final ActiveGameLobby lobby;

}

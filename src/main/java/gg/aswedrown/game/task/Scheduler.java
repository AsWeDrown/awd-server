package gg.aswedrown.game.task;

import lombok.NonNull;

public interface Scheduler {

    void schedule(@NonNull Task task);

    void updatePending();

}

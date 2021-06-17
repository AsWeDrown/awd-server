package gg.aswedrown.game.task;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class ServerThreadScheduler implements Scheduler {

    private final Object lock = new Object();

    private final Collection<Task> pendingTasks = new ArrayList<>();

    private Thread serverThread;

    @Override
    public void schedule(@NonNull Task task) {
        synchronized (lock) {
            pendingTasks.add(task);
        }
    }

    @Override
    public void updatePending() {
        if (serverThread == null)
            serverThread = Thread.currentThread();
        else if (serverThread != Thread.currentThread())
            throw new IllegalStateException(
                    "updatePending() cannot be called from multiple threads (use server thread instead)");

        Collection<Task> executedTasks = new ArrayList<>();

        synchronized (lock) {
            for (Task task : pendingTasks) {
                if (task.delayTicks == 0) {
                    try {
                        task.runnable.run();
                    } catch (Exception ex) {
                        log.error("Unhandled exception during pending task execution", ex);
                    } finally {
                        executedTasks.add(task);
                    }
                } else
                    task.delayTicks--;
            }

            pendingTasks.removeAll(executedTasks);
        }
    }

}

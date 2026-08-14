package com.bstar.qolmod.automation.task;

import com.bstar.qolmod.automation.AutomationStopReason;
import com.bstar.qolmod.automation.TaskContext;
import java.util.Objects;

/** Gives a child a fixed number of client ticks in which to finish. */
public final class TimeoutTask implements QOLTask {
    private final QOLTask child;
    private final long timeoutTicks;
    private long elapsedTicks;
    private boolean started;
    private boolean childFinished;

    public TimeoutTask(QOLTask child, long timeoutTicks) {
        this.child = Objects.requireNonNull(child, "child");
        if (timeoutTicks < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        this.timeoutTicks = timeoutTicks;
    }

    @Override
    public void start(TaskContext context) {
        if (started) {
            throw new IllegalStateException("TimeoutTask cannot be started twice");
        }
        started = true;
        child.start(context);
    }

    @Override
    public TaskResult tick(TaskContext context) {
        if (!started) {
            throw new IllegalStateException("TimeoutTask must be started before it is ticked");
        }
        if (childFinished) {
            throw new IllegalStateException("TimeoutTask cannot be ticked after completion");
        }
        if (elapsedTicks >= timeoutTicks) {
            return timeOut(context);
        }

        TaskResult result = Objects.requireNonNull(child.tick(context), "Timed task returned null");
        elapsedTicks++;
        if (result.outcome() == TaskOutcome.SUCCESS) {
            childFinished = true;
            return result;
        }
        if (result.outcome() == TaskOutcome.FAILURE) {
            return result;
        }
        return elapsedTicks >= timeoutTicks ? timeOut(context) : TaskResult.running();
    }

    @Override
    public void cancel(TaskContext context) {
        if (started && !childFinished) {
            child.cancel(context);
            childFinished = true;
        }
    }

    @Override
    public String activity() {
        return child.activity();
    }

    @Override
    public boolean isWaiting() {
        return child.isWaiting();
    }

    private TaskResult timeOut(TaskContext context) {
        try {
            child.cancel(context);
        } catch (RuntimeException exception) {
            childFinished = true;
            return TaskResult.failure(
                    "Timed out after " + timeoutTicks + " ticks; cleanup failed",
                    AutomationStopReason.ERROR,
                    exception
            );
        }
        childFinished = true;
        return TaskResult.failure(
                "Timed out after " + timeoutTicks + " ticks while " + child.activity().toLowerCase(),
                AutomationStopReason.TIMEOUT
        );
    }
}

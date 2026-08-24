package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.automation.TaskContext;
import com.bstar.qolmod.automation.task.QOLTask;
import com.bstar.qolmod.automation.task.TaskOutcome;
import com.bstar.qolmod.automation.task.TaskResult;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** AutoDuper-owned finite/infinite cycle loop; this is not a core scripting primitive. */
final class AutoDuperCycleLoopTask implements QOLTask {
    private final int targetCycles;
    private final IntFunction<? extends QOLTask> cycleFactory;
    private final IntConsumer cycleCompleted;
    private QOLTask activeCycle;
    private int completedCycles;
    private boolean started;
    private boolean complete;

    AutoDuperCycleLoopTask(
            int targetCycles,
            IntFunction<? extends QOLTask> cycleFactory,
            IntConsumer cycleCompleted
    ) {
        if (targetCycles < 0) {
            throw new IllegalArgumentException("Target cycles cannot be negative");
        }
        this.targetCycles = targetCycles;
        this.cycleFactory = Objects.requireNonNull(cycleFactory, "cycleFactory");
        this.cycleCompleted = Objects.requireNonNull(cycleCompleted, "cycleCompleted");
    }

    @Override
    public void start(TaskContext context) {
        if (started) {
            throw new IllegalStateException("AutoDuper cycle loop cannot be started twice");
        }
        started = true;
        startCycle(context);
    }

    @Override
    public TaskResult tick(TaskContext context) {
        requireRunning();
        TaskResult result = Objects.requireNonNull(activeCycle.tick(context), "AutoDuper cycle returned null");
        if (result.outcome() != TaskOutcome.SUCCESS) {
            return result;
        }

        completedCycles++;
        cycleCompleted.accept(completedCycles);
        if (targetCycles > 0 && completedCycles >= targetCycles) {
            complete = true;
            return TaskResult.success("Completed all " + targetCycles + " AutoDuper cycles");
        }

        startCycle(context);
        return TaskResult.running();
    }

    @Override
    public void cancel(TaskContext context) {
        if (started && !complete && activeCycle != null) {
            activeCycle.cancel(context);
        }
    }

    @Override
    public String activity() {
        return activeCycle == null ? "Starting cycle" : activeCycle.activity();
    }

    @Override
    public boolean isWaiting() {
        return started && !complete && activeCycle != null && activeCycle.isWaiting();
    }

    int completedCycles() {
        return completedCycles;
    }

    private void startCycle(TaskContext context) {
        int cycleNumber = completedCycles + 1;
        activeCycle = Objects.requireNonNull(cycleFactory.apply(cycleNumber), "Cycle factory returned null");
        activeCycle.start(context);
    }

    private void requireRunning() {
        if (!started) {
            throw new IllegalStateException("AutoDuper cycle loop must be started before it is ticked");
        }
        if (complete) {
            throw new IllegalStateException("AutoDuper cycle loop cannot be ticked after completion");
        }
    }
}

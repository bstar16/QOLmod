package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.automation.AutomationWorkflow;
import com.bstar.qolmod.automation.TaskContext;
import com.bstar.qolmod.automation.task.QOLTask;
import com.bstar.qolmod.automation.task.TaskResult;
import java.util.Objects;

/** The real AutoDuper workflow built from named, non-blocking automation tasks. */
public final class AutoDuperWorkflow implements AutomationWorkflow {
    public static final String ID = "auto-duper-workflow";

    private final AutoDuperConfig config;
    private final int targetCycles;
    private int completedCycles;
    private boolean taskCreated;

    public AutoDuperWorkflow(AutoDuperConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        targetCycles = config.cycles();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "Auto Duper";
    }

    @Override
    public String description() {
        return targetCycles == 0
                ? "Repeating AutoDuper cycles until cancelled"
                : "Running " + targetCycles + " AutoDuper cycle" + (targetCycles == 1 ? "" : "s");
    }

    @Override
    public QOLTask createTask(TaskContext context) {
        if (taskCreated) {
            throw new IllegalStateException("AutoDuperWorkflow cannot create more than one root task");
        }
        taskCreated = true;
        AutoDuperSession session = new AutoDuperSession(config);
        AutoDuperCycleLoopTask cycles = new AutoDuperCycleLoopTask(
                targetCycles,
                ignored -> AutoDuperTasks.oneCycle(session),
                completed -> completedCycles = completed
        );
        return new AutoDuperRunTask(session, cycles);
    }

    public int completedCycles() {
        return completedCycles;
    }

    public int targetCycles() {
        return targetCycles;
    }

    private final class AutoDuperRunTask implements QOLTask {
        private final AutoDuperSession session;
        private final AutoDuperCycleLoopTask cycles;
        private int announcedCycles;

        private AutoDuperRunTask(AutoDuperSession session, AutoDuperCycleLoopTask cycles) {
            this.session = session;
            this.cycles = cycles;
        }

        @Override
        public void start(TaskContext context) {
            session.begin(context);
            cycles.start(context);
        }

        @Override
        public TaskResult tick(TaskContext context) {
            var invalidEnvironment = session.invalidEnvironment(context);
            if (invalidEnvironment.isPresent()) {
                return TaskResult.failure(invalidEnvironment.orElseThrow());
            }

            TaskResult result = cycles.tick(context);
            if (completedCycles > announcedCycles) {
                announcedCycles = completedCycles;
                session.sendMessage(context, "Completed cycle " + completedCycles);
            }
            return result;
        }

        @Override
        public void cancel(TaskContext context) {
            cycles.cancel(context);
        }

        @Override
        public String activity() {
            return cycles.activity();
        }

        @Override
        public boolean isWaiting() {
            return cycles.isWaiting();
        }
    }
}

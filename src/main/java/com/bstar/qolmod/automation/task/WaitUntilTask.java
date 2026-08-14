package com.bstar.qolmod.automation.task;

import com.bstar.qolmod.automation.TaskContext;
import java.util.Objects;
import java.util.function.Predicate;

public final class WaitUntilTask implements QOLTask {
    private final String activity;
    private final Predicate<TaskContext> condition;

    public WaitUntilTask(String activity, Predicate<TaskContext> condition) {
        this.activity = activity == null || activity.isBlank() ? "Waiting for condition" : activity;
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    @Override
    public TaskResult tick(TaskContext context) {
        try {
            return condition.test(context) ? TaskResult.success() : TaskResult.running();
        } catch (RuntimeException exception) {
            return TaskResult.failure("Condition failed while " + activity.toLowerCase(), exception);
        }
    }

    @Override
    public String activity() {
        return activity;
    }

    @Override
    public boolean isWaiting() {
        return true;
    }
}

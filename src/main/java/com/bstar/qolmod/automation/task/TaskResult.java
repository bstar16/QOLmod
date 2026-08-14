package com.bstar.qolmod.automation.task;

import com.bstar.qolmod.automation.AutomationStopReason;
import java.util.Objects;
import java.util.Optional;

public record TaskResult(
        TaskOutcome outcome,
        Optional<String> detail,
        Optional<AutomationStopReason> stopReason,
        Optional<RuntimeException> cause
) {
    private static final TaskResult RUNNING = new TaskResult(
            TaskOutcome.RUNNING, Optional.empty(), Optional.empty(), Optional.empty());
    private static final TaskResult SUCCESS = new TaskResult(
            TaskOutcome.SUCCESS, Optional.empty(), Optional.empty(), Optional.empty());

    public TaskResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        detail = detail == null ? Optional.empty() : detail;
        stopReason = stopReason == null ? Optional.empty() : stopReason;
        cause = cause == null ? Optional.empty() : cause;
        if (outcome != TaskOutcome.FAILURE && (stopReason.isPresent() || cause.isPresent())) {
            throw new IllegalArgumentException("Only failed tasks may provide a stop reason or cause");
        }
    }

    public static TaskResult running() {
        return RUNNING;
    }

    public static TaskResult success() {
        return SUCCESS;
    }

    public static TaskResult success(String detail) {
        return new TaskResult(TaskOutcome.SUCCESS, Optional.ofNullable(detail), Optional.empty(), Optional.empty());
    }

    public static TaskResult failure(String detail) {
        return failure(detail, AutomationStopReason.ERROR, null);
    }

    public static TaskResult failure(String detail, RuntimeException cause) {
        return failure(detail, AutomationStopReason.ERROR, cause);
    }

    public static TaskResult failure(String detail, AutomationStopReason stopReason) {
        return failure(detail, stopReason, null);
    }

    public static TaskResult failure(
            String detail,
            AutomationStopReason stopReason,
            RuntimeException cause
    ) {
        return new TaskResult(
                TaskOutcome.FAILURE,
                Optional.ofNullable(detail),
                Optional.of(Objects.requireNonNull(stopReason, "stopReason")),
                Optional.ofNullable(cause)
        );
    }
}

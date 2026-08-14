package com.bstar.qolmod.automation;

import java.util.Objects;
import java.util.Optional;

public record AutomationStatus(
        AutomationState state,
        Optional<String> workflowId,
        Optional<String> workflowName,
        String activity,
        Optional<String> detail,
        long elapsedTicks,
        Optional<AutomationStopReason> stopReason
) {
    public AutomationStatus {
        state = Objects.requireNonNull(state, "state");
        workflowId = workflowId == null ? Optional.empty() : workflowId;
        workflowName = workflowName == null ? Optional.empty() : workflowName;
        activity = activity == null ? "" : activity;
        detail = detail == null ? Optional.empty() : detail;
        stopReason = stopReason == null ? Optional.empty() : stopReason;
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks cannot be negative");
        }
    }

    public static AutomationStatus idle() {
        return new AutomationStatus(
                AutomationState.IDLE,
                Optional.empty(),
                Optional.empty(),
                "Idle",
                Optional.empty(),
                0,
                Optional.empty()
        );
    }

    public double elapsedSeconds() {
        return elapsedTicks / 20.0;
    }
}

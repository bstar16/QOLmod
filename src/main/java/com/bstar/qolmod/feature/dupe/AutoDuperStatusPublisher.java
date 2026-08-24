package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.automation.AutomationState;
import com.bstar.qolmod.automation.AutomationStatus;
import com.bstar.qolmod.hud.status.StatusCardData;
import com.bstar.qolmod.hud.status.StatusProgress;
import com.bstar.qolmod.hud.status.StatusRegistry;
import com.bstar.qolmod.hud.status.StatusState;
import java.util.Objects;

/** Adapts Auto Duper-owned cycle information into the generic HUD presentation contract. */
public final class AutoDuperStatusPublisher {
    public static final String SOURCE_ID = "automation:auto_duper";
    private static final String TITLE = "Auto Duper";

    private final StatusRegistry registry;

    public AutoDuperStatusPublisher(StatusRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void publish(AutomationStatus automationStatus, AutoDuperWorkflow workflow) {
        Objects.requireNonNull(automationStatus, "automationStatus");
        Objects.requireNonNull(workflow, "workflow");
        StatusState state = presentationState(automationStatus.state());
        StatusProgress progress = progress(workflow, state);
        StatusCardData data = StatusCardData.of(
                SOURCE_ID,
                TITLE,
                automationStatus.activity(),
                state,
                progress
        );
        if (automationStatus.detail().isPresent()) {
            data = data.withDetail(automationStatus.detail().orElseThrow());
        }
        registry.publish(data);
    }

    public void publishStartFailure(String detail) {
        registry.publish(StatusCardData.of(
                SOURCE_ID,
                TITLE,
                "Could not start",
                StatusState.ERROR,
                StatusProgress.none()
        ).withDetail(detail));
    }

    private StatusProgress progress(AutoDuperWorkflow workflow, StatusState state) {
        int completed = workflow.completedCycles();
        int target = workflow.targetCycles();
        if (target == 0) {
            int cycle = state == StatusState.COMPLETED ? completed : completed + 1;
            return StatusProgress.countOnly(cycle, "Cycle");
        }
        int cycle = state == StatusState.COMPLETED ? completed : Math.min(completed + 1, target);
        return StatusProgress.finite(cycle, target, "Cycle");
    }

    private StatusState presentationState(AutomationState state) {
        return switch (state) {
            case RUNNING -> StatusState.ACTIVE;
            case WAITING -> StatusState.WAITING;
            case COMPLETED -> StatusState.COMPLETED;
            case ERROR -> StatusState.ERROR;
            case CANCELLED -> StatusState.CANCELLED;
            case IDLE -> throw new IllegalArgumentException("Idle automation has no contextual status card");
        };
    }
}

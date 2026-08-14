package com.bstar.qolmod.automation;

import java.util.Objects;

/** Opaque engine-created identity used to scope controlled inputs to one workflow run. */
public final class AutomationInputOwner {
    private final String workflowId;

    AutomationInputOwner(String workflowId) {
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId");
    }

    public String workflowId() {
        return workflowId;
    }

    @Override
    public String toString() {
        return "automation:" + workflowId;
    }
}

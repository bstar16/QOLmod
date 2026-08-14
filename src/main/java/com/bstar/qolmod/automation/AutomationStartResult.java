package com.bstar.qolmod.automation;

import java.util.Objects;

public record AutomationStartResult(boolean started, String message) {
    public AutomationStartResult {
        message = Objects.requireNonNullElse(message, "");
    }

    public static AutomationStartResult started(String message) {
        return new AutomationStartResult(true, message);
    }

    public static AutomationStartResult rejected(String message) {
        return new AutomationStartResult(false, message);
    }
}

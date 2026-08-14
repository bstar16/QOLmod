package com.bstar.qolmod.automation;

public enum AutomationStopReason {
    COMPLETED,
    USER_CANCELLED,
    MANUAL_OVERRIDE,
    PANIC,
    WORLD_LEFT,
    PLAYER_DIED,
    ERROR,
    TIMEOUT,
    CLIENT_SHUTDOWN
}

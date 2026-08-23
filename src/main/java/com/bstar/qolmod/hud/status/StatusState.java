package com.bstar.qolmod.hud.status;

/** Small, stable presentation contract shared by all contextual status sources. */
public enum StatusState {
    ACTIVE,
    WAITING,
    COMPLETED,
    ERROR,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETED || this == ERROR || this == CANCELLED;
    }
}

package com.bstar.qolmod.hud;

import java.util.Objects;

/** Mutable persisted settings for one HUD widget. */
public final class HudWidgetConfig {
    private final boolean defaultEnabled;
    private final HudPosition defaultPosition;
    private boolean enabled;
    private HudPosition position;

    public HudWidgetConfig(boolean enabled, HudPosition position) {
        this.defaultEnabled = enabled;
        this.defaultPosition = Objects.requireNonNull(position, "position");
        this.enabled = enabled;
        this.position = position;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public HudPosition position() {
        return position;
    }

    public void setPosition(HudPosition position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public HudPosition defaultPosition() {
        return defaultPosition;
    }

    public void resetPosition() {
        position = defaultPosition;
    }

    public void reset() {
        enabled = defaultEnabled;
        position = defaultPosition;
    }
}

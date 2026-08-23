package com.bstar.qolmod.hud;

import java.util.Objects;

/** Mutable persisted settings for one HUD widget. */
public final class HudWidgetConfig {
    private boolean enabled;
    private HudPosition position;

    public HudWidgetConfig(boolean enabled, HudPosition position) {
        this.enabled = enabled;
        this.position = Objects.requireNonNull(position, "position");
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
}

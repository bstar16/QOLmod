package com.bstar.qolmod.hud;

/** Root for HUD configuration, ready to grow with additional widgets. */
public final class HudConfig {
    private final HudWidgetConfig contextualStatus = new HudWidgetConfig(true, HudPosition.upperRightDefault());
    private final HudWidgetConfig notifications = new HudWidgetConfig(
            true,
            new HudPosition(HudAnchor.TOP_RIGHT, 10, 72, 1.0, 0.92)
    );

    public HudWidgetConfig contextualStatus() {
        return contextualStatus;
    }

    public HudWidgetConfig notifications() {
        return notifications;
    }
}

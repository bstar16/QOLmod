package com.bstar.qolmod.hud;

/** Root for HUD configuration, ready to grow with additional widgets. */
public final class HudConfig {
    private final HudWidgetConfig contextualStatus = new HudWidgetConfig(true, HudPosition.upperRightDefault());

    public HudWidgetConfig contextualStatus() {
        return contextualStatus;
    }
}

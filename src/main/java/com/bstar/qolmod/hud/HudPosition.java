package com.bstar.qolmod.hud;

import java.util.Objects;

/** Configurable placement shared by HUD widgets; dimensions remain widget-owned. */
public record HudPosition(HudAnchor anchor, int xOffset, int yOffset, double scale, double opacity) {
    public HudPosition {
        anchor = Objects.requireNonNull(anchor, "anchor");
        if (!Double.isFinite(scale) || scale < 0.5 || scale > 2.0) {
            throw new IllegalArgumentException("HUD scale must be between 0.5 and 2.0");
        }
        if (!Double.isFinite(opacity) || opacity < 0.0 || opacity > 1.0) {
            throw new IllegalArgumentException("HUD opacity must be between 0.0 and 1.0");
        }
    }

    public static HudPosition upperRightDefault() {
        return new HudPosition(HudAnchor.TOP_RIGHT, 10, 10, 1.0, 0.92);
    }
}

package com.bstar.qolmod.ui.theme;

/** Theme-owned parameters shared by GUI, HUD, and future liquid-glass surfaces. */
public record GlassStyle(
        int mainTint,
        int drawerTint,
        int hudTint,
        int innerHighlight,
        int innerShadow,
        int blurPaddingPixels,
        double blurStrength,
        GlassBorderStyle borderStyle,
        double highlightStrength
) {
    public GlassStyle {
        if (blurPaddingPixels < 0) {
            throw new IllegalArgumentException("blurPaddingPixels must be non-negative");
        }
        blurStrength = clamp(blurStrength, 0.0, 1.0);
        borderStyle = borderStyle == null ? GlassBorderStyle.BOLD : borderStyle;
        highlightStrength = clamp(highlightStrength, 0.0, 1.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}

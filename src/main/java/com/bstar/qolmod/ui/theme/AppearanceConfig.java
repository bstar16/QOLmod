package com.bstar.qolmod.ui.theme;

/** Persisted, user-facing appearance values with conservative material-safe bounds. */
public record AppearanceConfig(
        int accentRgb,
        double guiTintStrength,
        double hudTintStrength,
        double blurStrength,
        GlassBorderStyle borderStyle
) {
    public static final int DEFAULT_ACCENT_RGB = 0x2F6BFF;
    public static final double MIN_GUI_TINT = 0.30;
    public static final double MAX_GUI_TINT = 0.72;
    public static final double MIN_HUD_TINT = 0.42;
    public static final double MAX_HUD_TINT = 0.78;
    public static final double MIN_BLUR = 0.45;
    public static final double MAX_BLUR = 1.00;
    private static final double DEFAULT_GUI_TINT = 0.47;
    private static final double DEFAULT_HUD_TINT = 0.64;
    private static final double DEFAULT_BLUR = 0.88;
    private static final GlassBorderStyle DEFAULT_BORDER_STYLE = GlassBorderStyle.BOLD;

    public AppearanceConfig {
        accentRgb &= 0x00FFFFFF;
        guiTintStrength = sanitizePercent(guiTintStrength, MIN_GUI_TINT, MAX_GUI_TINT, DEFAULT_GUI_TINT);
        hudTintStrength = sanitizePercent(hudTintStrength, MIN_HUD_TINT, MAX_HUD_TINT, DEFAULT_HUD_TINT);
        blurStrength = sanitizePercent(blurStrength, MIN_BLUR, MAX_BLUR, DEFAULT_BLUR);
        borderStyle = borderStyle == null ? DEFAULT_BORDER_STYLE : borderStyle;
    }

    public static AppearanceConfig defaults() {
        return new AppearanceConfig(
                DEFAULT_ACCENT_RGB,
                DEFAULT_GUI_TINT,
                DEFAULT_HUD_TINT,
                DEFAULT_BLUR,
                DEFAULT_BORDER_STYLE
        );
    }

    public AppearanceConfig withAccentRgb(int value) {
        return new AppearanceConfig(
                value, guiTintStrength, hudTintStrength, blurStrength,
                borderStyle
        );
    }

    public AppearanceConfig withGuiTintStrength(double value) {
        return new AppearanceConfig(
                accentRgb, value, hudTintStrength, blurStrength,
                borderStyle
        );
    }

    public AppearanceConfig withHudTintStrength(double value) {
        return new AppearanceConfig(
                accentRgb, guiTintStrength, value, blurStrength,
                borderStyle
        );
    }

    public AppearanceConfig withBlurStrength(double value) {
        return new AppearanceConfig(
                accentRgb, guiTintStrength, hudTintStrength, value,
                borderStyle
        );
    }

    public AppearanceConfig withBorderStyle(GlassBorderStyle value) {
        return new AppearanceConfig(
                accentRgb, guiTintStrength, hudTintStrength, blurStrength,
                value
        );
    }

    private static double sanitizePercent(
            double value,
            double minimum,
            double maximum,
            double fallback
    ) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        double clamped = Math.max(minimum, Math.min(maximum, value));
        // The Settings page edits whole percentages. Canonical values keep exact preset matching
        // stable across widget synchronization and JSON round-trips.
        return Math.round(clamped * 100.0) / 100.0;
    }
}

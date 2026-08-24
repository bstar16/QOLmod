package com.bstar.qolmod.ui.theme;

import java.util.Objects;

/** Owns the active UI theme so screens never depend on concrete colour values. */
public final class ThemeManager {
    public static final double FIXED_HIGHLIGHT_STRENGTH = 0.34;
    private static final Theme DARK_COBALT = createTheme(
            AppearanceConfig.defaults(), "dark-cobalt", "Dark Cobalt"
    );

    private static Theme active = DARK_COBALT;

    private ThemeManager() {
    }

    public static Theme active() {
        return active;
    }

    public static void setActive(Theme theme) {
        active = Objects.requireNonNull(theme, "theme");
    }

    public static void applyAppearance(AppearanceConfig appearance) {
        active = themeFor(Objects.requireNonNull(appearance, "appearance"));
    }

    public static Theme themeFor(AppearanceConfig appearance) {
        Objects.requireNonNull(appearance, "appearance");
        if (appearance.equals(AppearanceConfig.defaults())) {
            return DARK_COBALT;
        }
        return createTheme(appearance, "configured", "Configured");
    }

    private static Theme createTheme(AppearanceConfig appearance, String id, String displayName) {
        int accent = 0xFF000000 | appearance.accentRgb();
        int guiAlpha = alpha(appearance.guiTintStrength());
        int drawerAlpha = (int) Math.round(guiAlpha * (112.0 / 120.0));
        return new Theme(
                id,
                displayName,
                new ColorPalette(
                        0x18070B12,
                        0x82111826,
                        0x941A2538,
                        accent,
                        accentHover(appearance.accentRgb()),
                        accent,
                        0xC051688F,
                        0x7851688F,
                        0xFFF5F7FC,
                        0xFFC4CEDD,
                        0xFF596276,
                        accent,
                        0xFF43C47A,
                        0xFFF2B84B,
                        0xFF38B6C8,
                        0xFFED5A68,
                        0x78000000
                ),
                new GlassStyle(
                        withAlpha(0x07101F, guiAlpha),
                        withAlpha(0x101B2C, drawerAlpha),
                        withAlpha(0x0B1424, alpha(appearance.hudTintStrength())),
                        0x887FA8FF,
                        0x9002050B,
                        12,
                        appearance.blurStrength(),
                        appearance.borderStyle(),
                        FIXED_HIGHLIGHT_STRENGTH
                )
        );
    }

    public static Theme darkCobalt() {
        return DARK_COBALT;
    }

    private static int alpha(double strength) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, strength)) * 255.0);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int accentHover(int rgb) {
        if ((rgb & 0x00FFFFFF) == AppearanceConfig.DEFAULT_ACCENT_RGB) {
            return 0xFF4C82FF;
        }
        int red = rgb >>> 16 & 0xFF;
        int green = rgb >>> 8 & 0xFF;
        int blue = rgb & 0xFF;
        double luminance = (red * 0.2126 + green * 0.7152 + blue * 0.0722) / 255.0;
        int target = luminance < 0.72 ? 0xFFFFFF : 0x000000;
        return 0xFF000000 | mixRgb(rgb, target, luminance < 0.72 ? 0.20 : 0.16);
    }

    private static int mixRgb(int from, int to, double amount) {
        int red = mixChannel(from >>> 16 & 0xFF, to >>> 16 & 0xFF, amount);
        int green = mixChannel(from >>> 8 & 0xFF, to >>> 8 & 0xFF, amount);
        int blue = mixChannel(from & 0xFF, to & 0xFF, amount);
        return red << 16 | green << 8 | blue;
    }

    private static int mixChannel(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
    }
}

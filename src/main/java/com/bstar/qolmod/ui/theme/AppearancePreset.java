package com.bstar.qolmod.ui.theme;

/** Curated material presets; CUSTOM is derived and never applies values of its own. */
public enum AppearancePreset {
    DEFAULT(AppearanceConfig.defaults()),
    DARKER(new AppearanceConfig(
            AppearanceConfig.DEFAULT_ACCENT_RGB,
            0.60, 0.72, 0.82, GlassBorderStyle.THICK
    )),
    CLEARER(new AppearanceConfig(
            AppearanceConfig.DEFAULT_ACCENT_RGB,
            0.36, 0.50, 0.62, GlassBorderStyle.THIN
    )),
    MINIMAL_GLASS(new AppearanceConfig(
            AppearanceConfig.DEFAULT_ACCENT_RGB,
            0.42, 0.56, AppearanceConfig.MIN_BLUR, GlassBorderStyle.THIN
    )),
    CUSTOM(null);

    private final AppearanceConfig appearance;

    AppearancePreset(AppearanceConfig appearance) {
        this.appearance = appearance;
    }

    public AppearanceConfig appearance() {
        if (appearance == null) {
            throw new IllegalStateException("CUSTOM has no preset appearance");
        }
        return appearance;
    }

    public static AppearancePreset identify(AppearanceConfig appearance) {
        for (AppearancePreset preset : values()) {
            if (preset != CUSTOM && preset.appearance().equals(appearance)) {
                return preset;
            }
        }
        return CUSTOM;
    }

    public String displayName() {
        return switch (this) {
            case MINIMAL_GLASS -> "MINIMAL GLASS";
            default -> name().replace('_', ' ');
        };
    }
}

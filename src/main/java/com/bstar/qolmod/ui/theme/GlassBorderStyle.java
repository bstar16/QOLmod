package com.bstar.qolmod.ui.theme;

import java.util.Locale;

/** User-facing solid glass border thicknesses in integer-aligned logical pixels. */
public enum GlassBorderStyle {
    THIN(1),
    BOLD(2),
    THICK(3);

    private final int thickness;

    GlassBorderStyle(int thickness) {
        this.thickness = thickness;
    }

    public int thickness() {
        return thickness;
    }

    public static GlassBorderStyle parse(String value, GlassBorderStyle fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    /** Maps the removed Phase 7 alpha control to the closest deliberate geometry choice. */
    public static GlassBorderStyle fromLegacyIntensity(double intensity) {
        if (!Double.isFinite(intensity)) {
            return BOLD;
        }
        if (intensity < 0.60) {
            return THIN;
        }
        if (intensity <= 0.94) {
            return BOLD;
        }
        return THICK;
    }
}

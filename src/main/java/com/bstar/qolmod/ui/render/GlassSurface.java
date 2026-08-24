package com.bstar.qolmod.ui.render;

/** Immutable, normalized parameters for one rectangular liquid-glass surface. */
public record GlassSurface(
        int x,
        int y,
        int width,
        int height,
        double opacity,
        int tint,
        int accent,
        double blurStrength
) {
    public GlassSurface {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Glass surface dimensions must be non-negative");
        }
        opacity = clamp(opacity, 0.0, 1.0);
        blurStrength = clamp(blurStrength, 0.0, 1.0);
    }

    public boolean isVisible() {
        return width > 0 && height > 0 && opacity > 0.0;
    }

    public boolean hasAccent() {
        return ((accent >>> 24) & 0xFF) > 0;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}

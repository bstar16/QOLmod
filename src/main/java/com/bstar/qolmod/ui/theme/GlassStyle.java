package com.bstar.qolmod.ui.theme;

/** Theme-owned parameters for the panel-local glass material. */
public record GlassStyle(
        int mainTint,
        int drawerTint,
        int edge,
        int innerHighlight,
        int innerShadow,
        int blurPaddingPixels
) {
    public GlassStyle {
        if (blurPaddingPixels < 0) {
            throw new IllegalArgumentException("blurPaddingPixels must be non-negative");
        }
    }
}

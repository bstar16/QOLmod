package com.bstar.qolmod.ui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassSurfaceTest {
    @Test
    void clampsOpacityAndBlurStrength() {
        GlassSurface surface = new GlassSurface(
                4, 8, 120, 48, 1.6, 0x8007101F, 0xFF2F6BFF, -0.4
        );

        assertEquals(1.0, surface.opacity());
        assertEquals(0.0, surface.blurStrength());
        assertTrue(surface.isVisible());
        assertTrue(surface.hasAccent());
    }

    @Test
    void nonFiniteOpacityDegradesToInvisible() {
        GlassSurface surface = new GlassSurface(
                0, 0, 10, 10, Double.NaN, 0xFFFFFFFF, 0, 1.0
        );

        assertEquals(0.0, surface.opacity());
        assertFalse(surface.isVisible());
        assertFalse(surface.hasAccent());
    }

    @Test
    void rejectsNegativeDimensionsButAllowsAnEmptyClippedSurface() {
        assertThrows(IllegalArgumentException.class,
                () -> new GlassSurface(0, 0, -1, 10, 1.0, 0, 0, 0.0));
        assertFalse(new GlassSurface(0, 0, 0, 10, 1.0, 0, 0, 0.0).isVisible());
    }

    @Test
    void alphaCompositionMultipliesExactlyOnce() {
        assertEquals(0x40112233, LiquidGlassSurfaceRenderer.multiplyAlpha(0x80112233, 0.5));
        assertEquals(0x00112233, LiquidGlassSurfaceRenderer.multiplyAlpha(0x80112233, -2.0));
        assertEquals(0x80112233, LiquidGlassSurfaceRenderer.multiplyAlpha(0x80112233, 4.0));
    }

    @Test
    void everyBorderLayerUsesOneSolidCustomAccentColor() {
        int customAccent = 0xFF8B5CF6;
        int expected = 0xE68B5CF6;

        for (var style : com.bstar.qolmod.ui.theme.GlassBorderStyle.values()) {
            for (int layer = 0; layer < style.thickness(); layer++) {
                assertEquals(expected,
                        LiquidGlassSurfaceRenderer.outerBorderColor(customAccent, 1.0));
            }
        }
    }
}

package com.bstar.qolmod.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class GlassStyleTest {
    @Test
    void darkCobaltDefaultsStayModerateAndUsable() {
        GlassStyle style = ThemeManager.darkCobalt().glass();

        assertEquals(12, style.blurPaddingPixels());
        assertEquals(0.88, style.blurStrength());
        assertEquals(GlassBorderStyle.BOLD, style.borderStyle());
        assertEquals(0.34, style.highlightStrength());
    }

    @Test
    void clampsTunableStrengthsAndRejectsInvalidPadding() {
        GlassStyle style = new GlassStyle(
                0, 0, 0, 0, 0, 4,
                3.0, null, Double.POSITIVE_INFINITY
        );

        assertEquals(1.0, style.blurStrength());
        assertEquals(GlassBorderStyle.BOLD, style.borderStyle());
        assertEquals(0.0, style.highlightStrength());
        assertThrows(IllegalArgumentException.class,
                () -> new GlassStyle(0, 0, 0, 0, 0, -1,
                        0.0, GlassBorderStyle.THIN, 0.0));
    }
}

package com.bstar.qolmod.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GlassBorderStyleTest {
    @Test
    void stylesUseDistinctIntegerThicknesses() {
        assertEquals(1, GlassBorderStyle.THIN.thickness());
        assertEquals(2, GlassBorderStyle.BOLD.thickness());
        assertEquals(3, GlassBorderStyle.THICK.thickness());
    }

    @Test
    void legacyDefaultMapsToBold() {
        assertEquals(GlassBorderStyle.BOLD, GlassBorderStyle.fromLegacyIntensity(0.90));
    }
}

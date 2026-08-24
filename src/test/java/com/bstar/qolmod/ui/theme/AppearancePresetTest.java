package com.bstar.qolmod.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AppearancePresetTest {
    @Test
    void requiredPresetsStayWithinSafeModelRanges() {
        for (AppearancePreset preset : new AppearancePreset[] {
                AppearancePreset.DEFAULT,
                AppearancePreset.DARKER,
                AppearancePreset.CLEARER,
                AppearancePreset.MINIMAL_GLASS
        }) {
            AppearanceConfig appearance = preset.appearance();
            assertTrue(appearance.guiTintStrength() >= AppearanceConfig.MIN_GUI_TINT);
            assertTrue(appearance.guiTintStrength() <= AppearanceConfig.MAX_GUI_TINT);
            assertTrue(appearance.hudTintStrength() >= AppearanceConfig.MIN_HUD_TINT);
            assertTrue(appearance.hudTintStrength() <= AppearanceConfig.MAX_HUD_TINT);
            assertEquals(preset, AppearancePreset.identify(appearance));
        }
    }

    @Test
    void resetAppearanceTargetIsTheExactDefault() {
        assertEquals(AppearanceConfig.defaults(), AppearancePreset.DEFAULT.appearance());
        assertEquals(AppearancePreset.DEFAULT,
                AppearancePreset.identify(AppearanceConfig.defaults()));
    }

    @Test
    void editingAfterPresetTransitionsToCustom() {
        AppearanceConfig edited = AppearancePreset.DARKER.appearance().withBlurStrength(0.83);

        assertEquals(AppearancePreset.CUSTOM, AppearancePreset.identify(edited));
        assertEquals(
                AppearancePreset.DARKER,
                AppearancePreset.identify(edited.withBlurStrength(0.82))
        );
        assertThrows(IllegalStateException.class, AppearancePreset.CUSTOM::appearance);
    }

    @Test
    void presetsOnlyContainAppearanceValues() {
        assertTrue(AppearancePreset.DARKER.appearance().guiTintStrength()
                > AppearancePreset.DEFAULT.appearance().guiTintStrength());
        assertTrue(AppearancePreset.CLEARER.appearance().blurStrength()
                < AppearancePreset.DEFAULT.appearance().blurStrength());
        assertEquals(GlassBorderStyle.BOLD, AppearancePreset.DEFAULT.appearance().borderStyle());
        assertEquals(GlassBorderStyle.THICK, AppearancePreset.DARKER.appearance().borderStyle());
        assertEquals(GlassBorderStyle.THIN, AppearancePreset.CLEARER.appearance().borderStyle());
        assertEquals(GlassBorderStyle.THIN, AppearancePreset.MINIMAL_GLASS.appearance().borderStyle());
    }
}

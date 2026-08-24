package com.bstar.qolmod.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class AppearanceConfigTest {
    @Test
    void defaultsReproduceApprovedPhaseSixMaterialValues() {
        AppearanceConfig defaults = AppearanceConfig.defaults();
        Theme theme = ThemeManager.themeFor(defaults);

        assertEquals(0x2F6BFF, defaults.accentRgb());
        assertEquals(0x7807101F, theme.glass().mainTint());
        assertEquals(0x70101B2C, theme.glass().drawerTint());
        assertEquals(0xA30B1424, theme.glass().hudTint());
        assertEquals(0.88, theme.glass().blurStrength());
        assertEquals(GlassBorderStyle.BOLD, theme.glass().borderStyle());
        assertEquals(ThemeManager.FIXED_HIGHLIGHT_STRENGTH, theme.glass().highlightStrength());
    }

    @Test
    void clampsManualValuesToSafeRangesAndSanitizesNonFiniteValues() {
        AppearanceConfig config = new AppearanceConfig(
                0xFF123456,
                -4.0,
                9.0,
                0.0,
                null
        );

        assertEquals(0x123456, config.accentRgb());
        assertEquals(AppearanceConfig.MIN_GUI_TINT, config.guiTintStrength());
        assertEquals(AppearanceConfig.MAX_HUD_TINT, config.hudTintStrength());
        assertEquals(AppearanceConfig.MIN_BLUR, config.blurStrength());
        assertEquals(AppearanceConfig.defaults().borderStyle(), config.borderStyle());
    }

    @Test
    void guiAndHudTintRemainIndependent() {
        AppearanceConfig defaults = AppearanceConfig.defaults();
        AppearanceConfig changed = defaults.withHudTintStrength(0.75);

        assertEquals(defaults.guiTintStrength(), changed.guiTintStrength());
        assertNotEquals(defaults.hudTintStrength(), changed.hudTintStrength());
        assertEquals(
                ThemeManager.themeFor(defaults).glass().mainTint(),
                ThemeManager.themeFor(changed).glass().mainTint()
        );
        assertNotEquals(
                ThemeManager.themeFor(defaults).glass().hudTint(),
                ThemeManager.themeFor(changed).glass().hudTint()
        );
    }
}

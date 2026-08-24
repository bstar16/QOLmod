package com.bstar.qolmod.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.bstar.qolmod.ui.render.GlassSurface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class ThemeManagerTest {
    @AfterEach
    void restoreDefaultTheme() {
        ThemeManager.applyAppearance(AppearanceConfig.defaults());
    }

    @Test
    void accentPropagatesWithoutRecoloringSemanticOutcomes() {
        Theme defaults = ThemeManager.themeFor(AppearanceConfig.defaults());
        Theme custom = ThemeManager.themeFor(AppearanceConfig.defaults().withAccentRgb(0xD95778));

        assertEquals(0xFFD95778, custom.colors().accent());
        assertEquals(custom.colors().accent(), custom.colors().outerBorder());
        assertEquals(custom.colors().accent(), custom.colors().active());
        assertNotEquals(custom.colors().accent(), custom.colors().accentHover());
        assertEquals(defaults.colors().success(), custom.colors().success());
        assertEquals(defaults.colors().waiting(), custom.colors().waiting());
        assertEquals(defaults.colors().error(), custom.colors().error());
    }

    @Test
    void widgetOpacityRemainsSeparateFromHudMaterialTint() {
        Theme custom = ThemeManager.themeFor(
                AppearanceConfig.defaults().withHudTintStrength(0.70)
        );
        GlassSurface surface = new GlassSurface(
                0, 0, 184, 54, 0.42, custom.glass().hudTint(), custom.colors().accent(), 0.0
        );

        assertEquals(0.42, surface.opacity());
        assertEquals(179, custom.glass().hudTint() >>> 24);
    }

}

package com.bstar.qolmod.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bstar.qolmod.ui.theme.AppearanceConfig;
import com.bstar.qolmod.ui.theme.AppearancePreset;
import com.bstar.qolmod.ui.theme.GlassBorderStyle;
import com.bstar.qolmod.ui.theme.ThemeManager;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

final class AppearanceConfigJsonTest {
    @Test
    void roundTripsEveryAppearanceValue() {
        AppearanceConfig expected = new AppearanceConfig(
                0x8B5CF6, 0.55, 0.70, 0.73, GlassBorderStyle.THICK
        );

        AppearanceConfig actual = AppearanceConfigJson.read(
                AppearanceConfigJson.write(expected),
                AppearanceConfig.defaults()
        );

        assertEquals(expected, actual);
    }

    @Test
    void persistsEverySemanticBorderStyle() {
        for (GlassBorderStyle style : GlassBorderStyle.values()) {
            AppearanceConfig expected = AppearanceConfig.defaults().withBorderStyle(style);
            AppearanceConfig actual = AppearanceConfigJson.read(
                    AppearanceConfigJson.write(expected), AppearanceConfig.defaults()
            );
            assertEquals(style, actual.borderStyle());
        }
    }

    @Test
    void missingAppearanceFromOlderConfigUsesPhaseSixDefaults() {
        assertEquals(
                AppearanceConfig.defaults(),
                AppearanceConfigJson.read(null, AppearanceConfig.defaults())
        );
        assertEquals(
                AppearanceConfig.defaults(),
                AppearanceConfigJson.read(new JsonObject(), AppearanceConfig.defaults())
        );
    }

    @Test
    void partialOlderAppearanceMigratesKnownValuesAndDefaultsTheRest() {
        JsonObject old = new JsonObject();
        old.addProperty("accent", "#22A9D6");
        old.addProperty("guiTintStrength", 0.52);

        AppearanceConfig loaded = AppearanceConfigJson.read(old, AppearanceConfig.defaults());

        assertEquals(0x22A9D6, loaded.accentRgb());
        assertEquals(0.52, loaded.guiTintStrength());
        assertEquals(AppearanceConfig.defaults().hudTintStrength(), loaded.hudTintStrength());
        assertEquals(AppearanceConfig.defaults().blurStrength(), loaded.blurStrength());
    }

    @Test
    void invalidManualValuesFallBackOrClampSafely() {
        JsonObject invalid = new JsonObject();
        invalid.addProperty("accent", "definitely-not-a-color");
        invalid.addProperty("guiTintStrength", -50.0);
        invalid.addProperty("hudTintStrength", 50.0);
        invalid.addProperty("refractionStrength", 900.0);
        invalid.addProperty("borderStyle", "not-a-style");

        AppearanceConfig loaded = AppearanceConfigJson.read(invalid, AppearanceConfig.defaults());

        assertEquals(AppearanceConfig.DEFAULT_ACCENT_RGB, loaded.accentRgb());
        assertEquals(AppearanceConfig.MIN_GUI_TINT, loaded.guiTintStrength());
        assertEquals(AppearanceConfig.MAX_HUD_TINT, loaded.hudTintStrength());
        assertEquals(AppearanceConfig.defaults().borderStyle(), loaded.borderStyle());
    }

    @Test
    void migratesLegacyBorderIntensityAndIgnoresRemovedHighlight() {
        assertLegacyBorder(0.35, GlassBorderStyle.THIN);
        assertLegacyBorder(0.90, GlassBorderStyle.BOLD);
        assertLegacyBorder(1.0, GlassBorderStyle.THICK);

        JsonObject legacy = new JsonObject();
        legacy.addProperty("highlightIntensity", 0.01);
        AppearanceConfig loaded = AppearanceConfigJson.read(legacy, AppearanceConfig.defaults());

        assertEquals(ThemeManager.FIXED_HIGHLIGHT_STRENGTH,
                ThemeManager.themeFor(loaded).glass().highlightStrength());
        JsonObject current = AppearanceConfigJson.write(loaded);
        assertEquals(false, current.has("highlightIntensity"));
        assertEquals(false, current.has("borderIntensity"));
        assertEquals(false, current.has("refractionStrength"));
    }

    @Test
    void legacyRefractionIsIgnoredAndDoesNotChangePresetRecognition() {
        for (AppearancePreset preset : AppearancePreset.values()) {
            if (preset == AppearancePreset.CUSTOM) {
                continue;
            }
            JsonObject legacy = AppearanceConfigJson.write(preset.appearance());
            legacy.addProperty("refractionStrength", preset.ordinal() / 3.0);

            AppearanceConfig loaded = AppearanceConfigJson.read(
                    legacy, AppearanceConfig.defaults()
            );

            assertEquals(preset, AppearancePreset.identify(loaded));
            assertEquals(false, AppearanceConfigJson.write(loaded).has("refractionStrength"));
        }
    }

    @Test
    void reloadPreservesDerivedPresetRecognition() {
        for (AppearancePreset preset : AppearancePreset.values()) {
            if (preset == AppearancePreset.CUSTOM) {
                continue;
            }
            AppearanceConfig reloaded = AppearanceConfigJson.read(
                    AppearanceConfigJson.write(preset.appearance()), AppearanceConfig.defaults()
            );
            assertEquals(preset, AppearancePreset.identify(reloaded));
        }
    }

    private static void assertLegacyBorder(double intensity, GlassBorderStyle expected) {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("borderIntensity", intensity);
        assertEquals(expected,
                AppearanceConfigJson.read(legacy, AppearanceConfig.defaults()).borderStyle());
    }
}

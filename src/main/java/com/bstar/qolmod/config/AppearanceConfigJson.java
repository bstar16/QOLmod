package com.bstar.qolmod.config;

import com.bstar.qolmod.ui.theme.AccentColor;
import com.bstar.qolmod.ui.theme.AppearanceConfig;
import com.bstar.qolmod.ui.theme.GlassBorderStyle;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.OptionalInt;

/** Pure JSON boundary for appearance migration, sanitization, and round-trip tests. */
final class AppearanceConfigJson {
    private AppearanceConfigJson() {
    }

    static AppearanceConfig read(JsonObject object, AppearanceConfig fallback) {
        if (object == null) {
            return fallback;
        }
        int accent = readAccent(object.get("accent"), fallback.accentRgb());
        return new AppearanceConfig(
                accent,
                readDouble(object, "guiTintStrength", fallback.guiTintStrength()),
                readDouble(object, "hudTintStrength", fallback.hudTintStrength()),
                readDouble(object, "blurStrength", fallback.blurStrength()),
                readBorderStyle(object, fallback.borderStyle())
        );
    }

    static JsonObject write(AppearanceConfig appearance) {
        JsonObject object = new JsonObject();
        object.addProperty("accent", AccentColor.formatHex(appearance.accentRgb()));
        object.addProperty("guiTintStrength", appearance.guiTintStrength());
        object.addProperty("hudTintStrength", appearance.hudTintStrength());
        object.addProperty("blurStrength", appearance.blurStrength());
        object.addProperty("borderStyle", appearance.borderStyle().name());
        return object;
    }

    private static GlassBorderStyle readBorderStyle(
            JsonObject object,
            GlassBorderStyle fallback
    ) {
        JsonElement style = object.get("borderStyle");
        if (style != null && style.isJsonPrimitive() && style.getAsJsonPrimitive().isString()) {
            return GlassBorderStyle.parse(style.getAsString(), fallback);
        }
        JsonElement legacy = object.get("borderIntensity");
        if (legacy != null && legacy.isJsonPrimitive() && legacy.getAsJsonPrimitive().isNumber()) {
            try {
                return GlassBorderStyle.fromLegacyIntensity(legacy.getAsDouble());
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int readAccent(JsonElement element, int fallback) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return fallback;
        }
        OptionalInt parsed = AccentColor.parseHex(element.getAsString());
        return parsed.orElse(fallback);
    }

    private static double readDouble(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}

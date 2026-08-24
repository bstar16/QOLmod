package com.bstar.qolmod.hud.editor;

import com.bstar.qolmod.hud.HudWidgetConfig;
import java.util.Objects;

/** Generic editor-facing identity, persistence key, and mutable config for a registered HUD widget. */
public record HudEditorMetadata(
        String id,
        String displayName,
        String configKey,
        HudWidgetConfig config
) {
    public HudEditorMetadata {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        configKey = requireText(configKey, "configKey");
        Objects.requireNonNull(config, "config");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}

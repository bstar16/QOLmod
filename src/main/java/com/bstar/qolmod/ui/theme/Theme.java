package com.bstar.qolmod.ui.theme;

import java.util.Objects;

public record Theme(String id, String displayName, ColorPalette colors, GlassStyle glass) {
    public Theme {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(colors, "colors");
        Objects.requireNonNull(glass, "glass");
    }
}

package com.bstar.qolmod.ui.theme;

import java.util.Objects;

/** Owns the active UI theme so screens never depend on concrete colour values. */
public final class ThemeManager {
    private static final Theme DARK_COBALT = new Theme(
            "dark-cobalt",
            "Dark Cobalt",
            new ColorPalette(
                    0x18070B12,
                    0x82111826,
                    0x941A2538,
                    0xFF2F6BFF,
                    0xFF4C82FF,
                    0xFF2F6BFF,
                    0xC051688F,
                    0x7851688F,
                    0xFFF5F7FC,
                    0xFFC4CEDD,
                    0xFF596276,
                    0xFF43C47A,
                    0xFFF2B84B,
                    0xFF38B6C8,
                    0xFFED5A68,
                    0x78000000
            ),
            new GlassStyle(
                    0x8007101F,
                    0x88101B2C,
                    12
            )
    );

    private static Theme active = DARK_COBALT;

    private ThemeManager() {
    }

    public static Theme active() {
        return active;
    }

    public static void setActive(Theme theme) {
        active = Objects.requireNonNull(theme, "theme");
    }

    public static Theme darkCobalt() {
        return DARK_COBALT;
    }
}

package com.bstar.qolmod.ui.theme;

import java.util.Objects;

/** Owns the active UI theme so screens never depend on concrete colour values. */
public final class ThemeManager {
    private static final Theme DARK_COBALT = new Theme(
            "dark-cobalt",
            "Dark Cobalt",
            new ColorPalette(
                    0x1803050A,
                    0x82171B23,
                    0x941D232E,
                    0xFF397CF6,
                    0xFF5A95FF,
                    0xFF397CF6,
                    0x506D84A8,
                    0xFFF1F5FC,
                    0xFFB5C0D1,
                    0xFF778399,
                    0xFF60A5FA,
                    0xFFF5B84C,
                    0xFF56D39B,
                    0xFFFF6B78,
                    0x78000000
            ),
            new GlassStyle(
                    0x80101826,
                    0x88121824,
                    0xB0397CF6,
                    0x487A9AC8,
                    0x58000000,
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

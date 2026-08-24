package com.bstar.qolmod.ui.theme;

import java.util.Locale;
import java.util.OptionalInt;

/** Parsing and formatting for the user-facing opaque RGB accent value. */
public final class AccentColor {
    private AccentColor() {
    }

    public static OptionalInt parseHex(String value) {
        if (value == null) {
            return OptionalInt.empty();
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    public static boolean isPartialHex(String value) {
        if (value == null || value.length() > 7) {
            return false;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        return normalized.length() <= 6 && normalized.matches("[0-9a-fA-F]*");
    }

    public static String formatHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0x00FFFFFF);
    }
}

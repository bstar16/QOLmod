package com.bstar.qolmod.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AccentColorTest {
    @Test
    void parsesAndNormalizesExactRgbHex() {
        assertEquals(0x2F6BFF, AccentColor.parseHex("#2f6bff").orElseThrow());
        assertEquals(0xA0B1C2, AccentColor.parseHex("A0B1C2").orElseThrow());
        assertEquals("#2F6BFF", AccentColor.formatHex(0xAA2F6BFF));
    }

    @Test
    void rejectsInvalidHexWithoutThrowing() {
        assertTrue(AccentColor.parseHex("#12345").isEmpty());
        assertTrue(AccentColor.parseHex("#GG1122").isEmpty());
        assertTrue(AccentColor.parseHex(null).isEmpty());
    }

    @Test
    void acceptsOnlyUsefulPartialKeyboardInput() {
        assertTrue(AccentColor.isPartialHex(""));
        assertTrue(AccentColor.isPartialHex("#"));
        assertTrue(AccentColor.isPartialHex("#2f6"));
        assertFalse(AccentColor.isPartialHex("##2F"));
        assertFalse(AccentColor.isPartialHex("#1234567"));
    }
}

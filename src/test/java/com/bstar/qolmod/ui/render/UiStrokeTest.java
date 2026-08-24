package com.bstar.qolmod.ui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class UiStrokeTest {
    @Test
    void borderThicknessFitsCompleteIntegerAlignedRings() {
        assertEquals(0, UiStroke.effectiveBorderThickness(0, 20, 3));
        assertEquals(0, UiStroke.effectiveBorderThickness(20, 20, 0));
        assertEquals(1, UiStroke.effectiveBorderThickness(1, 8, 3));
        assertEquals(2, UiStroke.effectiveBorderThickness(4, 9, 3));
        assertEquals(3, UiStroke.effectiveBorderThickness(184, 54, 3));
    }
}

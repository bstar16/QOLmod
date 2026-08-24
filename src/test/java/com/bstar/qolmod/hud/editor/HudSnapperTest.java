package com.bstar.qolmod.hud.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudSnapperTest {
    @Test
    void snapsToScreenEdgesWithinThreshold() {
        HudSnapResult result = HudSnapper.snap(8, 154, 100, 40, 300, 200, 4, 6);

        assertEquals(4, result.x());
        assertEquals(156, result.y());
        assertEquals(2, result.guides().size());
    }

    @Test
    void snapsWidgetCenterToScreenAxes() {
        HudSnapResult result = HudSnapper.snap(102, 78, 100, 40, 300, 200, 4, 6);

        assertEquals(100, result.x());
        assertEquals(80, result.y());
        assertTrue(result.guides().stream().anyMatch(guide -> guide.coordinate() == 150));
        assertTrue(result.guides().stream().anyMatch(guide -> guide.coordinate() == 100));
    }

    @Test
    void releasesSnapOutsideThreshold() {
        HudSnapResult result = HudSnapper.snap(13, 69, 100, 40, 300, 200, 4, 6);

        assertEquals(13, result.x());
        assertEquals(69, result.y());
        assertTrue(result.guides().isEmpty());
    }
}

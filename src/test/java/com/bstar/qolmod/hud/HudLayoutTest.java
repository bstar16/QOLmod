package com.bstar.qolmod.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudLayoutTest {
    @Test
    void sharedTopRightLanePlacesNotificationsBelowStatusStack() {
        HudLayout layout = new HudLayout(320, 240);
        HudPosition position = HudPosition.upperRightDefault();

        HudPlacement statuses = layout.place(position, 184, 114, 0);
        HudPlacement notifications = layout.place(position, 184, 89, 8);

        assertEquals(statuses.x(), notifications.x());
        assertTrue(notifications.y() >= statuses.y() + 114 + 8);
    }

    @Test
    void unoccupiedLaneDoesNotApplyInterWidgetGap() {
        HudLayout layout = new HudLayout(320, 240);

        HudPlacement notifications = layout.place(HudPosition.upperRightDefault(), 184, 42, 8);

        assertEquals(10, notifications.y());
    }

    @Test
    void bottomAnchorsAllocateUpwardWithoutOverlap() {
        HudLayout layout = new HudLayout(320, 240);
        HudPosition position = new HudPosition(HudAnchor.BOTTOM_RIGHT, 10, 10, 1.0, 1.0);

        HudPlacement first = layout.place(position, 184, 54, 0);
        HudPlacement second = layout.place(position, 184, 42, 8);

        assertTrue(second.y() + 42 + 8 <= first.y());
    }

    @Test
    void sharedLaneUsesScaledLogicalHeightAtSmallAndLargeScales() {
        for (double scale : new double[]{0.75, 1.5}) {
            HudLayout layout = new HudLayout(640, 480);
            HudPosition position = new HudPosition(HudAnchor.TOP_RIGHT, 10, 10, scale, 1.0);

            HudPlacement first = layout.place(position, 184, 71, 0);
            HudPlacement second = layout.place(position, 184, 42, 8);

            int scaledFirstHeight = (int) Math.ceil(71 * scale);
            assertTrue(second.y() >= first.y() + scaledFirstHeight + 8);
        }
    }
}

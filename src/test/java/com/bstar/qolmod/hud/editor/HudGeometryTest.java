package com.bstar.qolmod.hud.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bstar.qolmod.hud.HudAnchor;
import com.bstar.qolmod.hud.HudPosition;
import com.bstar.qolmod.hud.HudWidgetConfig;
import org.junit.jupiter.api.Test;

class HudGeometryTest {
    private static final HudSize SIZE = new HudSize(100, 40);

    @Test
    void resolvesAllCornerAnchors() {
        assertEquals(new HudRect(10, 12, 100, 40), rect(HudAnchor.TOP_LEFT));
        assertEquals(new HudRect(190, 12, 100, 40), rect(HudAnchor.TOP_RIGHT));
        assertEquals(new HudRect(10, 148, 100, 40), rect(HudAnchor.BOTTOM_LEFT));
        assertEquals(new HudRect(190, 148, 100, 40), rect(HudAnchor.BOTTOM_RIGHT));
    }

    @Test
    void changingAnchorPreservesVisualPosition() {
        HudPosition original = new HudPosition(HudAnchor.TOP_LEFT, 37, 29, 1.0, 0.8);
        HudRect before = HudGeometry.screenRect(original, SIZE, 300, 200);

        HudPosition changed = HudGeometry.changeAnchorPreservingScreenPosition(
                original, HudAnchor.BOTTOM_RIGHT, SIZE, 300, 200
        );

        assertEquals(before, HudGeometry.screenRect(changed, SIZE, 300, 200));
        assertEquals(163, changed.xOffset());
        assertEquals(131, changed.yOffset());
    }

    @Test
    void selectingEachAnchorMovesToItsDefaultCornerMargin() {
        HudPosition original = new HudPosition(HudAnchor.TOP_LEFT, 37, 29, 1.0, 0.8);

        assertEquals(new HudRect(10, 10, 100, 40), anchoredRect(original, HudAnchor.TOP_LEFT));
        assertEquals(new HudRect(190, 10, 100, 40), anchoredRect(original, HudAnchor.TOP_RIGHT));
        assertEquals(new HudRect(10, 150, 100, 40), anchoredRect(original, HudAnchor.BOTTOM_LEFT));
        assertEquals(new HudRect(190, 150, 100, 40), anchoredRect(original, HudAnchor.BOTTOM_RIGHT));
    }

    @Test
    void anchorSelectionUsesScaledBoundsAndPreservesPresentation() {
        HudPosition original = new HudPosition(HudAnchor.BOTTOM_LEFT, 80, 50, 1.5, 0.67);

        HudPosition anchored = HudGeometry.positionAtAnchor(
                original, HudAnchor.BOTTOM_RIGHT, SIZE, 300, 200, 10
        );

        assertEquals(new HudRect(140, 130, 150, 60), HudGeometry.screenRect(anchored, SIZE, 300, 200));
        assertEquals(1.5, anchored.scale());
        assertEquals(0.67, anchored.opacity());
    }

    @Test
    void changingSelectedAnchorIsVisiblyMeaningful() {
        HudPosition original = new HudPosition(HudAnchor.TOP_RIGHT, 10, 10, 1.0, 0.9);
        HudPosition topLeft = HudGeometry.positionAtAnchor(
                original, HudAnchor.TOP_LEFT, SIZE, 300, 200, 10
        );
        HudPosition bottomRight = HudGeometry.positionAtAnchor(
                original, HudAnchor.BOTTOM_RIGHT, SIZE, 300, 200, 10
        );

        assertNotEquals(
                HudGeometry.screenRect(topLeft, SIZE, 300, 200),
                HudGeometry.screenRect(bottomRight, SIZE, 300, 200)
        );
    }

    @Test
    void draggingAfterAnchorSelectionKeepsSelectedAnchorReference() {
        HudPosition selected = HudGeometry.positionAtAnchor(
                HudPosition.upperRightDefault(), HudAnchor.BOTTOM_RIGHT, SIZE, 300, 200, 10
        );

        HudPosition dragged = HudGeometry.fromScreenPosition(selected, 75, 55, SIZE, 300, 200);

        assertEquals(HudAnchor.BOTTOM_RIGHT, dragged.anchor());
        assertEquals(new HudRect(75, 55, 100, 40), HudGeometry.screenRect(dragged, SIZE, 300, 200));
    }

    @Test
    void clampingKeepsScaledWidgetInsideSafeBounds() {
        HudPosition stranded = new HudPosition(HudAnchor.TOP_LEFT, -80, 500, 1.5, 0.9);
        HudPosition clamped = HudGeometry.clampPosition(stranded, SIZE, 300, 200, 4);
        HudRect rect = HudGeometry.screenRect(clamped, SIZE, 300, 200);

        assertEquals(new HudRect(4, 136, 150, 60), rect);
    }

    @Test
    void resolutionChangeCanBeReclampedWithoutLosingAnchor() {
        HudPosition largeScreen = new HudPosition(HudAnchor.TOP_RIGHT, 20, 340, 1.0, 0.9);
        HudPosition clamped = HudGeometry.clampPosition(largeScreen, SIZE, 220, 120, 4);
        HudRect rect = HudGeometry.screenRect(clamped, SIZE, 220, 120);

        assertEquals(HudAnchor.TOP_RIGHT, clamped.anchor());
        assertTrue(rect.x() >= 4);
        assertTrue(rect.y() >= 4);
        assertTrue(rect.right() <= 216);
        assertTrue(rect.bottom() <= 116);
    }

    @Test
    void resetMethodsRestoreExpectedDefaults() {
        HudPosition defaults = new HudPosition(HudAnchor.TOP_RIGHT, 10, 72, 1.0, 0.92);
        HudWidgetConfig config = new HudWidgetConfig(true, defaults);
        config.setEnabled(false);
        config.setPosition(new HudPosition(HudAnchor.BOTTOM_LEFT, 25, 30, 1.4, 0.5));

        config.resetPosition();
        assertEquals(defaults, config.position());
        assertEquals(false, config.enabled());

        config.reset();
        assertEquals(defaults, config.position());
        assertTrue(config.enabled());
    }

    private HudRect rect(HudAnchor anchor) {
        return HudGeometry.screenRect(new HudPosition(anchor, 10, 12, 1.0, 0.9), SIZE, 300, 200);
    }

    private HudRect anchoredRect(HudPosition original, HudAnchor anchor) {
        HudPosition anchored = HudGeometry.positionAtAnchor(original, anchor, SIZE, 300, 200, 10);
        return HudGeometry.screenRect(anchored, SIZE, 300, 200);
    }
}

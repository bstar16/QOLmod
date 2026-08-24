package com.bstar.qolmod.hud.editor;

import java.util.ArrayList;
import java.util.List;

/** Restrained nearest-target snapping for screen edges and axes. */
public final class HudSnapper {
    private HudSnapper() {
    }

    public static HudSnapResult snap(
            int x,
            int y,
            int width,
            int height,
            int screenWidth,
            int screenHeight,
            int margin,
            int threshold
    ) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Snap threshold cannot be negative");
        }
        AxisSnap horizontal = nearest(
                x,
                threshold,
                new Target(margin, margin),
                new Target((screenWidth - width) / 2, screenWidth / 2),
                new Target(screenWidth - margin - width, screenWidth - margin)
        );
        AxisSnap vertical = nearest(
                y,
                threshold,
                new Target(margin, margin),
                new Target((screenHeight - height) / 2, screenHeight / 2),
                new Target(screenHeight - margin - height, screenHeight - margin)
        );
        List<HudSnapGuide> guides = new ArrayList<>(2);
        if (horizontal.snapped()) {
            guides.add(new HudSnapGuide(HudSnapGuide.Axis.VERTICAL, horizontal.guide()));
        }
        if (vertical.snapped()) {
            guides.add(new HudSnapGuide(HudSnapGuide.Axis.HORIZONTAL, vertical.guide()));
        }
        return new HudSnapResult(horizontal.position(), vertical.position(), guides);
    }

    private static AxisSnap nearest(int value, int threshold, Target... targets) {
        Target closest = null;
        int distance = Integer.MAX_VALUE;
        for (Target target : targets) {
            int candidateDistance = Math.abs(value - target.position());
            if (candidateDistance < distance) {
                distance = candidateDistance;
                closest = target;
            }
        }
        return closest != null && distance <= threshold
                ? new AxisSnap(closest.position(), closest.guide(), true)
                : new AxisSnap(value, 0, false);
    }

    private record Target(int position, int guide) {
    }

    private record AxisSnap(int position, int guide, boolean snapped) {
    }
}

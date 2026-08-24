package com.bstar.qolmod.hud;

import java.util.EnumMap;
import java.util.Map;

/** Small per-frame anchor allocator that prevents QOLmod widget stacks from overlapping. */
public final class HudLayout {
    private final int screenWidth;
    private final int screenHeight;
    private final Map<HudAnchor, Integer> cursors = new EnumMap<>(HudAnchor.class);

    public HudLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public HudPlacement place(HudPosition position, int logicalWidth, int logicalHeight, int gapBefore) {
        double scale = position.scale();
        int width = (int) Math.ceil(logicalWidth * scale);
        int height = (int) Math.ceil(logicalHeight * scale);
        int x = switch (position.anchor()) {
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - position.xOffset() - width;
            case TOP_LEFT, BOTTOM_LEFT -> position.xOffset();
        };

        boolean occupied = cursors.containsKey(position.anchor());
        int y;
        if (position.anchor() == HudAnchor.TOP_LEFT || position.anchor() == HudAnchor.TOP_RIGHT) {
            int requestedTop = position.yOffset();
            y = occupied ? Math.max(requestedTop, cursors.get(position.anchor()) + gapBefore) : requestedTop;
            cursors.put(position.anchor(), y + height);
        } else {
            int requestedBottom = screenHeight - position.yOffset();
            int bottom = occupied ? Math.min(requestedBottom, cursors.get(position.anchor()) - gapBefore) : requestedBottom;
            y = bottom - height;
            cursors.put(position.anchor(), y);
        }
        return new HudPlacement(x, y);
    }
}

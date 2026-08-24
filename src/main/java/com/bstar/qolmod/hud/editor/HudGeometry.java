package com.bstar.qolmod.hud.editor;

import com.bstar.qolmod.hud.HudAnchor;
import com.bstar.qolmod.hud.HudPosition;
import java.util.Objects;

/** Pure anchor/offset conversion and safe-screen clamping shared by runtime HUD and editor. */
public final class HudGeometry {
    private HudGeometry() {
    }

    public static HudRect screenRect(HudPosition position, HudSize logicalSize, int screenWidth, int screenHeight) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(logicalSize, "logicalSize");
        int width = scaled(logicalSize.width(), position.scale());
        int height = scaled(logicalSize.height(), position.scale());
        int x = isRight(position.anchor())
                ? screenWidth - position.xOffset() - width
                : position.xOffset();
        int y = isBottom(position.anchor())
                ? screenHeight - position.yOffset() - height
                : position.yOffset();
        return new HudRect(x, y, width, height);
    }

    public static HudRect clampedScreenRect(
            HudPosition position,
            HudSize logicalSize,
            int screenWidth,
            int screenHeight,
            int margin
    ) {
        HudRect rect = screenRect(position, logicalSize, screenWidth, screenHeight);
        return clampRect(rect.x(), rect.y(), rect.width(), rect.height(), screenWidth, screenHeight, margin);
    }

    public static HudPosition clampPosition(
            HudPosition position,
            HudSize logicalSize,
            int screenWidth,
            int screenHeight,
            int margin
    ) {
        HudRect rect = clampedScreenRect(position, logicalSize, screenWidth, screenHeight, margin);
        return fromScreenPosition(position, rect.x(), rect.y(), logicalSize, screenWidth, screenHeight);
    }

    public static HudPosition changeAnchorPreservingScreenPosition(
            HudPosition position,
            HudAnchor anchor,
            HudSize logicalSize,
            int screenWidth,
            int screenHeight
    ) {
        Objects.requireNonNull(anchor, "anchor");
        HudRect rect = screenRect(position, logicalSize, screenWidth, screenHeight);
        HudPosition anchored = new HudPosition(
                anchor,
                position.xOffset(),
                position.yOffset(),
                position.scale(),
                position.opacity()
        );
        return fromScreenPosition(anchored, rect.x(), rect.y(), logicalSize, screenWidth, screenHeight);
    }

    /** Places a widget in the selected corner with equal screen-edge margins. */
    public static HudPosition positionAtAnchor(
            HudPosition position,
            HudAnchor anchor,
            HudSize logicalSize,
            int screenWidth,
            int screenHeight,
            int margin
    ) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(anchor, "anchor");
        if (margin < 0) {
            throw new IllegalArgumentException("margin must be non-negative");
        }
        HudPosition anchored = new HudPosition(
                anchor,
                margin,
                margin,
                position.scale(),
                position.opacity()
        );
        return clampPosition(anchored, logicalSize, screenWidth, screenHeight, margin);
    }

    public static HudPosition fromScreenPosition(
            HudPosition template,
            int x,
            int y,
            HudSize logicalSize,
            int screenWidth,
            int screenHeight
    ) {
        Objects.requireNonNull(template, "template");
        int width = scaled(logicalSize.width(), template.scale());
        int height = scaled(logicalSize.height(), template.scale());
        int xOffset = isRight(template.anchor()) ? screenWidth - x - width : x;
        int yOffset = isBottom(template.anchor()) ? screenHeight - y - height : y;
        return new HudPosition(
                template.anchor(),
                xOffset,
                yOffset,
                template.scale(),
                template.opacity()
        );
    }

    public static HudRect clampRect(
            int x,
            int y,
            int width,
            int height,
            int screenWidth,
            int screenHeight,
            int margin
    ) {
        if (width <= 0 || height <= 0 || screenWidth <= 0 || screenHeight <= 0 || margin < 0) {
            throw new IllegalArgumentException("Invalid HUD screen bounds");
        }
        int maxX = Math.max(margin, screenWidth - margin - width);
        int maxY = Math.max(margin, screenHeight - margin - height);
        return new HudRect(
                Math.max(margin, Math.min(maxX, x)),
                Math.max(margin, Math.min(maxY, y)),
                width,
                height
        );
    }

    private static int scaled(int logical, double scale) {
        return Math.max(1, (int) Math.ceil(logical * scale));
    }

    private static boolean isRight(HudAnchor anchor) {
        return anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT;
    }

    private static boolean isBottom(HudAnchor anchor) {
        return anchor == HudAnchor.BOTTOM_LEFT || anchor == HudAnchor.BOTTOM_RIGHT;
    }
}

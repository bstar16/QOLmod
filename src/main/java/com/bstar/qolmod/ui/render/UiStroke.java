package com.bstar.qolmod.ui.render;

import net.minecraft.client.gui.DrawContext;

/** Shared one-pixel, integer-aligned strokes for QOLmod surfaces and dividers. */
public final class UiStroke {
    private UiStroke() {
    }

    public static void horizontal(DrawContext context, int left, int right, int y, int color) {
        if (right > left) {
            context.fill(left, y, right, y + 1, color);
        }
    }

    public static void vertical(DrawContext context, int x, int top, int bottom, int color) {
        if (bottom > top) {
            context.fill(x, top, x + 1, bottom, color);
        }
    }

    /** Draws connected shell dividers without blending an intersection pixel twice. */
    public static void structuralGrid(
            DrawContext context,
            int left,
            int right,
            int top,
            int bottom,
            int verticalX,
            int color
    ) {
        horizontal(context, left, right, top, color);
        if (bottom > top) {
            horizontal(context, left, right, bottom, color);
            if (verticalX >= left && verticalX < right) {
                vertical(context, verticalX, top + 1, bottom, color);
            }
        }
    }

    public static void border(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        horizontal(context, x, x + width, y, color);
        if (height > 1) {
            horizontal(context, x, x + width, y + height - 1, color);
        }
        if (height > 2) {
            vertical(context, x, y + 1, y + height - 1, color);
            if (width > 1) {
                vertical(context, x + width - 1, y + 1, y + height - 1, color);
            }
        }
    }
}

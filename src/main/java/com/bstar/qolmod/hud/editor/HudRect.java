package com.bstar.qolmod.hud.editor;

/** Scaled screen-space bounds for one HUD widget. */
public record HudRect(int x, int y, int width, int height) {
    public HudRect {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("HUD bounds must be positive");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double pointX, double pointY) {
        return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }
}

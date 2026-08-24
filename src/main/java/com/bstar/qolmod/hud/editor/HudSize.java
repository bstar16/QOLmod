package com.bstar.qolmod.hud.editor;

/** Logical, pre-scale dimensions of a HUD widget preview. */
public record HudSize(int width, int height) {
    public HudSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("HUD dimensions must be positive");
        }
    }
}

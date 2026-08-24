package com.bstar.qolmod.hud.editor;

/** Editor-only guide at a snapped screen coordinate. */
public record HudSnapGuide(Axis axis, int coordinate) {
    public enum Axis {
        VERTICAL,
        HORIZONTAL
    }
}

package com.bstar.qolmod.hud.render;

import net.minecraft.client.gui.DrawContext;

/** Reusable panel-local material for compact HUD surfaces. */
public interface HudSurface {
    void draw(DrawContext context, int x, int y, int width, int height, double opacity, int stateColor);
}

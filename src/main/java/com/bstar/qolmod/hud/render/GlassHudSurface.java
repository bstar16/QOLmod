package com.bstar.qolmod.hud.render;

import com.bstar.qolmod.ui.render.GlassSurface;
import com.bstar.qolmod.ui.render.LiquidGlassSurfaceRenderer;
import com.bstar.qolmod.ui.theme.GlassStyle;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

/** Lightweight local glass treatment suitable for several small cards in one HUD pass. */
public final class GlassHudSurface implements HudSurface {
    private final LiquidGlassSurfaceRenderer renderer = new LiquidGlassSurfaceRenderer();

    @Override
    public void draw(DrawContext context, int x, int y, int width, int height, double opacity, int stateColor) {
        GlassStyle style = ThemeManager.active().glass();
        renderer.draw(context, new GlassSurface(
                x,
                y,
                width,
                height,
                opacity,
                style.hudTint(),
                stateColor,
                0.0
        ));
    }
}

package com.bstar.qolmod.hud.render;

import com.bstar.qolmod.ui.render.UiStroke;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

/** Lightweight local glass treatment suitable for several small cards in one HUD pass. */
public final class GlassHudSurface implements HudSurface {
    @Override
    public void draw(DrawContext context, int x, int y, int width, int height, double opacity, int stateColor) {
        ColorPalette colors = ThemeManager.active().colors();
        context.fill(x + 2, y + 3, x + width + 2, y + height + 3, multiplyAlpha(colors.shadow(), opacity * 0.55));
        context.fill(x, y, x + width, y + height, multiplyAlpha(colors.surface(), opacity));
        UiStroke.border(context, x, y, width, height, multiplyAlpha(colors.outerBorder(), opacity * 0.82));
        UiStroke.border(context, x + 2, y + 2, width - 4, height - 4,
                multiplyAlpha(colors.subtleDivider(), opacity * 0.62));
        context.fill(x, y + 1, x + 3, y + height - 1, multiplyAlpha(stateColor, opacity));
    }

    private int multiplyAlpha(int color, double opacity) {
        int alpha = (color >>> 24) & 0xFF;
        int adjusted = (int) Math.round(alpha * Math.max(0.0, Math.min(1.0, opacity)));
        return (adjusted << 24) | (color & 0x00FFFFFF);
    }
}

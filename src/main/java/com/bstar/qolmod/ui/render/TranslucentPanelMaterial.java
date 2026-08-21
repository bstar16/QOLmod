package com.bstar.qolmod.ui.render;

import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

/** Stage 1 dark translucent fallback material. */
public final class TranslucentPanelMaterial implements PanelMaterial {
    @Override
    public void drawMainPanel(DrawContext context, int x, int y, int width, int height) {
        ColorPalette colors = ThemeManager.active().colors();
        context.fill(x, y, x + width, y + height, colors.surface());
        drawOuterBorder(context, x, y, width, height, colors.border());
        drawInnerBorder(context, x + 2, y + 2, width - 4, height - 4, colors.innerBorder());
    }

    @Override
    public void drawDrawer(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            boolean attached,
            int clipLeft,
            int clipRight
    ) {
        ColorPalette colors = ThemeManager.active().colors();
        context.fill(x, y, x + width, y + height, attached ? colors.surface() : colors.elevatedSurface());
        drawOuterBorder(context, x, y, width, height, colors.border());
        drawInnerBorder(context, x + 2, y + 2, width - 4, height - 4, colors.innerBorder());
    }

    private void drawOuterBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 2, color);
        context.fill(x, y + height - 2, x + width, y + height, color);
        context.fill(x, y + 2, x + 2, y + height - 2, color);
        context.fill(x + width - 2, y + 2, x + width, y + height - 2, color);
    }

    private void drawInnerBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + 1, x + 1, y + height, color);
    }
}

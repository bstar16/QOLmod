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
        UiStroke.border(context, x, y, width, height, colors.outerBorder());
        UiStroke.border(context, x + 2, y + 2, width - 4, height - 4, colors.subtleDivider());
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
        UiStroke.border(context, x, y, width, height, colors.outerBorder());
        UiStroke.border(context, x + 2, y + 2, width - 4, height - 4, colors.subtleDivider());
    }
}

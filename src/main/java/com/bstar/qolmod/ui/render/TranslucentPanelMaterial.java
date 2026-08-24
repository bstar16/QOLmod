package com.bstar.qolmod.ui.render;

import com.bstar.qolmod.ui.theme.GlassStyle;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

/** Dark translucent fallback that retains the shared liquid-glass edge treatment. */
public final class TranslucentPanelMaterial implements PanelMaterial {
    private final LiquidGlassSurfaceRenderer renderer = new LiquidGlassSurfaceRenderer();

    @Override
    public void drawMainPanel(DrawContext context, int x, int y, int width, int height) {
        GlassStyle style = ThemeManager.active().glass();
        renderer.draw(context, new GlassSurface(
                x, y, width, height, 1.0, style.mainTint(), 0, 0.0
        ));
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
        GlassStyle style = ThemeManager.active().glass();
        int tint = attached ? style.drawerTint() : style.hudTint();
        int safeLeft = Math.max(x, clipLeft);
        int safeRight = Math.min(x + width, clipRight);
        if (safeRight <= safeLeft) {
            return;
        }
        context.enableScissor(safeLeft, y, safeRight, y + height);
        renderer.draw(context, new GlassSurface(
                x, y, width, height, 1.0, tint, 0, 0.0
        ));
        context.disableScissor();
    }
}

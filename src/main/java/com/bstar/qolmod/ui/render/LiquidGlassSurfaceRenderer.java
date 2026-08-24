package com.bstar.qolmod.ui.render;

import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.GlassStyle;
import com.bstar.qolmod.ui.theme.GlassBorderStyle;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;

/**
 * Shared material language for rectangular QOLmod surfaces. Layout owners provide geometry and an
 * optional semantic accent; an optional backdrop supplies the advanced framebuffer treatment.
 */
public final class LiquidGlassSurfaceRenderer {
    static final double OUTER_BORDER_OPACITY = 0.90;

    @FunctionalInterface
    public interface Backdrop {
        void draw(DrawContext context, GlassSurface surface);
    }

    public void draw(DrawContext context, GlassSurface surface) {
        draw(context, surface, null);
    }

    public void draw(DrawContext context, GlassSurface surface, Backdrop backdrop) {
        if (!surface.isVisible()) {
            return;
        }

        ColorPalette colors = ThemeManager.active().colors();
        GlassStyle style = ThemeManager.active().glass();
        int x = surface.x();
        int y = surface.y();
        int width = surface.width();
        int height = surface.height();
        double opacity = surface.opacity();
        GlassBorderStyle borderStyle = style.borderStyle();
        int borderLayers = UiStroke.effectiveBorderThickness(
                width, height, borderStyle.thickness()
        );

        if (backdrop != null && surface.blurStrength() > 0.0) {
            backdrop.draw(context, surface);
        }
        context.fill(x, y, x + width, y + height, multiplyAlpha(surface.tint(), opacity));

        // A dark lower/right inner edge and faint upper/left response suggest thickness without
        // changing the approved rectangular silhouette.
        int innerShadow = multiplyAlpha(style.innerShadow(), opacity * 0.48);
        int innerInset = Math.max(1, borderLayers);
        if (width > innerInset * 2 && height > innerInset * 2) {
            int innerRight = x + width - innerInset;
            int innerBottom = y + height - innerInset;
            UiStroke.horizontal(context, x + innerInset, innerRight,
                    innerBottom - 1, innerShadow);
            UiStroke.vertical(context, innerRight - 1, y + innerInset,
                    innerBottom - 1, innerShadow);

            int highlight = multiplyAlpha(
                    style.innerHighlight(), opacity * style.highlightStrength()
            );
            UiStroke.horizontal(context, x + innerInset, innerRight,
                    y + innerInset, highlight);
            UiStroke.vertical(context, x + innerInset, y + innerInset + 1,
                    innerBottom, highlight);
        }

        drawOuterBorder(context, x, y, width, height, colors.outerBorder(), opacity,
                borderLayers);
        if (surface.hasAccent() && width > borderLayers + 2 && height > borderLayers * 2) {
            int accentX = x + borderLayers;
            context.fill(accentX, y + borderLayers, accentX + 2, y + height - borderLayers,
                    multiplyAlpha(surface.accent(), opacity * 0.88));
        }
    }

    private static void drawOuterBorder(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int color,
            double opacity,
            int layers
    ) {
        // Thickness is geometry only: every ring reuses this exact accent color and alpha.
        int borderColor = outerBorderColor(color, opacity);
        for (int layer = 0; layer < layers; layer++) {
            UiStroke.border(
                    context,
                    x + layer,
                    y + layer,
                    width - layer * 2,
                    height - layer * 2,
                    borderColor
            );
        }
    }

    static int outerBorderColor(int accent, double surfaceOpacity) {
        return multiplyAlpha(accent, surfaceOpacity * OUTER_BORDER_OPACITY);
    }

    public static int multiplyAlpha(int color, double opacity) {
        double normalized = Double.isFinite(opacity) ? Math.max(0.0, Math.min(1.0, opacity)) : 0.0;
        int alpha = (color >>> 24) & 0xFF;
        int adjusted = (int) Math.round(alpha * normalized);
        return (adjusted << 24) | (color & 0x00FFFFFF);
    }
}

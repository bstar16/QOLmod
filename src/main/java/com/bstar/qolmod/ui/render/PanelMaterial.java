package com.bstar.qolmod.ui.render;

import net.minecraft.client.gui.DrawContext;

/**
 * Replaceable panel-local material boundary; implementations must leave framebuffer pixels outside
 * the supplied surface bounds untouched. Stage 3 can supply glass without changing screen layout.
 */
public interface PanelMaterial extends AutoCloseable {
    /** Captures/prepares the smallest rectangle containing all visible QOLmod glass. */
    default void prepareFrame(DrawContext context, int x, int y, int width, int height) {
    }

    void drawMainPanel(DrawContext context, int x, int y, int width, int height);

    void drawDrawer(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            boolean attached,
            int clipLeft,
            int clipRight
    );

    @Override
    default void close() {
    }
}

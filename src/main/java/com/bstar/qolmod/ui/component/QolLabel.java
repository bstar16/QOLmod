package com.bstar.qolmod.ui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Lightweight presentation-only label used by screen layouts. */
public record QolLabel(Text text, int color) {
    public void draw(DrawContext context, TextRenderer renderer, int x, int y) {
        context.drawText(renderer, text, x, y, color, false);
    }
}

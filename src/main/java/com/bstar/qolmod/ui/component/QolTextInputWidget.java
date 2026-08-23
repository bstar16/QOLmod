package com.bstar.qolmod.ui.component;

import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class QolTextInputWidget extends TextFieldWidget {
    private static final int HORIZONTAL_PADDING = 4;
    private static final int VANILLA_TEXT_HEIGHT = 8;

    public QolTextInputWidget(TextRenderer renderer, int x, int y, int width, int height, Text narration) {
        super(renderer, x, y, width, height, narration);
        setDrawsBackground(false);
        setTextShadow(false);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ColorPalette colors = ThemeManager.active().colors();
        setEditableColor(colors.primaryText());
        setUneditableColor(colors.mutedText());
        context.fill(getX(), getY(), getRight(), getBottom(), colors.elevatedSurface());
        context.drawStrokedRectangle(getX(), getY(), getWidth(), getHeight(),
                isFocused() ? colors.accent() : colors.subtleDivider());

        int verticalOffset = Math.max(0, (getHeight() - VANILLA_TEXT_HEIGHT) / 2);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(HORIZONTAL_PADDING, verticalOffset);
        try {
            super.renderWidget(context, mouseX - HORIZONTAL_PADDING, mouseY - verticalOffset, delta);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        super.onClick(offsetClick(click), doubled);
    }

    @Override
    protected void onDrag(Click click, double deltaX, double deltaY) {
        super.onDrag(offsetClick(click), deltaX, deltaY);
    }

    @Override
    public int getInnerWidth() {
        return Math.max(0, super.getInnerWidth() - HORIZONTAL_PADDING * 2);
    }

    @Override
    public int getCharacterX(int index) {
        return super.getCharacterX(index) + HORIZONTAL_PADDING;
    }

    private Click offsetClick(Click click) {
        int verticalOffset = Math.max(0, (getHeight() - VANILLA_TEXT_HEIGHT) / 2);
        return new Click(
                click.x() - HORIZONTAL_PADDING,
                click.y() - verticalOffset,
                click.buttonInfo()
        );
    }
}

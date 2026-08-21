package com.bstar.qolmod.ui.component;

import com.bstar.qolmod.ui.animation.AnimatedValue;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class QolButtonWidget extends ClickableWidget {
    public enum Style {
        STANDARD,
        GHOST,
        NAVIGATION,
        ICON
    }

    private final Runnable action;
    private final Style style;
    private final BooleanSupplier selected;
    private final AnimatedValue hover = new AnimatedValue(0.0, 140, AnimatedValue.Easing.CUBIC_OUT);

    public QolButtonWidget(int x, int y, int width, int height, Text message, Style style, Runnable action) {
        this(x, y, width, height, message, style, () -> false, action);
    }

    public QolButtonWidget(
            int x,
            int y,
            int width,
            int height,
            Text message,
            Style style,
            BooleanSupplier selected,
            Runnable action
    ) {
        super(x, y, width, height, message);
        this.style = Objects.requireNonNull(style, "style");
        this.selected = Objects.requireNonNull(selected, "selected");
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        action.run();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!active || !visible || !isFocused()) {
            return false;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER
                || input.key() == GLFW.GLFW_KEY_SPACE) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            action.run();
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ColorPalette colors = ThemeManager.active().colors();
        hover.setTarget(isHovered() ? 1.0 : 0.0);
        boolean chosen = selected.getAsBoolean();
        int background = switch (style) {
            case NAVIGATION -> chosen ? withAlpha(colors.accent(), 30) : withAlpha(colors.elevatedSurface(), (int) (hover.value() * 75));
            case GHOST, ICON -> withAlpha(colors.elevatedSurface(), (int) (hover.value() * 120));
            case STANDARD -> isHovered() ? withAlpha(colors.accent(), 58) : colors.elevatedSurface();
        };
        context.fill(getX(), getY(), getRight(), getBottom(), background);
        if (chosen && style == Style.NAVIGATION) {
            context.fill(getX(), getY() + 3, getX() + 2, getBottom() - 3, colors.accent());
        } else if (style == Style.STANDARD) {
            drawBorder(context, isFocused() ? colors.accent() : colors.innerBorder());
        }
        int textColor = active ? (chosen ? colors.primaryText() : colors.secondaryText()) : colors.mutedText();
        int textX = style == Style.NAVIGATION ? getX() + 9 : getX() + (getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(getMessage())) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, getMessage(), textX, textY, textColor, false);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    private void drawBorder(DrawContext context, int color) {
        context.drawStrokedRectangle(getX(), getY(), getWidth(), getHeight(), color);
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}

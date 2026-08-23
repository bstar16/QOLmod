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

/** Compact icon-and-label navigation item used by the main QOLmod sidebar. */
public final class QolNavigationWidget extends ClickableWidget {
    public enum Icon {
        FEATURES,
        HUD,
        KEYBINDS,
        SETTINGS,
        ABOUT
    }

    private final Icon icon;
    private final BooleanSupplier selected;
    private final Runnable action;
    private final AnimatedValue hover = new AnimatedValue(0.0, 130, AnimatedValue.Easing.CUBIC_OUT);

    public QolNavigationWidget(
            int x,
            int y,
            int width,
            int height,
            Text label,
            Icon icon,
            BooleanSupplier selected,
            Runnable action
    ) {
        super(x, y, width, height, label);
        this.icon = Objects.requireNonNull(icon, "icon");
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
        boolean chosen = selected.getAsBoolean();
        hover.setTarget(isHovered() || isFocused() ? 1.0 : 0.0);
        double emphasis = Math.max(chosen ? 1.0 : 0.0, hover.value() * 0.58);
        int backgroundAlpha = chosen ? 34 : (int) Math.round(hover.value() * 58.0);
        if (backgroundAlpha > 0) {
            context.fill(getX(), getY(), getRight(), getBottom(),
                    withAlpha(colors.elevatedSurface(), backgroundAlpha));
        }
        if (chosen) {
            context.fill(getX(), getY() + 3, getX() + 2, getBottom() - 3, colors.accent());
        }

        int iconColor = mix(colors.secondaryText(), colors.accentHover(), emphasis);
        int textColor = mix(colors.secondaryText(), colors.primaryText(), emphasis);
        int iconX = getX() + 9;
        int iconY = getY() + (getHeight() - 12) / 2;
        drawIcon(context, iconX, iconY, iconColor);
        int textY = getY() + (getHeight() - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, getMessage(), getX() + 28, textY,
                textColor, false);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    private void drawIcon(DrawContext context, int x, int y, int color) {
        switch (icon) {
            case FEATURES -> {
                context.fill(x, y + 1, x + 5, y + 6, color);
                context.fill(x + 7, y + 1, x + 12, y + 6, color);
                context.fill(x, y + 8, x + 5, y + 12, color);
                context.fill(x + 7, y + 8, x + 12, y + 12, color);
            }
            case HUD -> {
                outline(context, x, y + 1, 12, 10, color);
                context.fill(x + 2, y + 3, x + 4, y + 9, color);
                context.fill(x + 5, y + 5, x + 7, y + 9, color);
                context.fill(x + 8, y + 2, x + 10, y + 9, color);
            }
            case KEYBINDS -> {
                outline(context, x, y + 2, 12, 8, color);
                for (int keyX = x + 2; keyX <= x + 8; keyX += 3) {
                    context.fill(keyX, y + 4, keyX + 2, y + 6, color);
                }
                context.fill(x + 3, y + 7, x + 9, y + 9, color);
            }
            case SETTINGS -> {
                outline(context, x + 3, y + 3, 6, 6, color);
                context.fill(x + 5, y, x + 7, y + 3, color);
                context.fill(x + 5, y + 9, x + 7, y + 12, color);
                context.fill(x, y + 5, x + 3, y + 7, color);
                context.fill(x + 9, y + 5, x + 12, y + 7, color);
            }
            case ABOUT -> {
                context.fill(x + 3, y, x + 9, y + 1, color);
                context.fill(x + 3, y + 11, x + 9, y + 12, color);
                context.fill(x + 1, y + 2, x + 2, y + 10, color);
                context.fill(x + 10, y + 2, x + 11, y + 10, color);
                context.fill(x + 2, y + 1, x + 3, y + 2, color);
                context.fill(x + 9, y + 1, x + 10, y + 2, color);
                context.fill(x + 2, y + 10, x + 3, y + 11, color);
                context.fill(x + 9, y + 10, x + 10, y + 11, color);
                context.fill(x + 5, y + 2, x + 7, y + 4, color);
                context.fill(x + 5, y + 5, x + 7, y + 9, color);
            }
        }
    }

    private static void outline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int mix(int from, int to, double progress) {
        double amount = Math.max(0.0, Math.min(1.0, progress));
        int alpha = channel(from, 24, to, amount);
        int red = channel(from, 16, to, amount);
        int green = channel(from, 8, to, amount);
        int blue = channel(from, 0, to, amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int channel(int from, int shift, int to, double progress) {
        int start = from >>> shift & 0xFF;
        int end = to >>> shift & 0xFF;
        return (int) Math.round(start + (end - start) * progress);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}

package com.bstar.qolmod.ui.component;

import com.bstar.qolmod.ui.animation.AnimatedValue;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class QolToggleWidget extends ClickableWidget {
    private final BooleanSupplier value;
    private final Consumer<Boolean> changed;
    private final AnimatedValue position;

    public QolToggleWidget(int x, int y, BooleanSupplier value, Consumer<Boolean> changed, Text narration) {
        super(x, y, 30, 14, narration);
        this.value = Objects.requireNonNull(value, "value");
        this.changed = Objects.requireNonNull(changed, "changed");
        position = new AnimatedValue(value.getAsBoolean() ? 1.0 : 0.0, 160, AnimatedValue.Easing.SMOOTH_STEP);
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        toggle();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!active || !visible || !isFocused()) {
            return false;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER
                || input.key() == GLFW.GLFW_KEY_SPACE) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            toggle();
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ColorPalette colors = ThemeManager.active().colors();
        boolean enabled = value.getAsBoolean();
        position.setTarget(enabled ? 1.0 : 0.0);
        int track = enabled
                ? withAlpha(colors.accent(), isHovered() ? 190 : 155)
                : withAlpha(colors.mutedText(), 128);
        context.fill(getX(), getY(), getRight(), getBottom(), track);
        if (isFocused()) {
            context.drawStrokedRectangle(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, colors.accentHover());
        }
        int knobX = getX() + 2 + (int) Math.round(position.value() * 16.0);
        context.fill(knobX, getY() + 2, knobX + 10, getBottom() - 2, enabled ? colors.primaryText() : colors.mutedText());
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    private void toggle() {
        changed.accept(!value.getAsBoolean());
    }

    private int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}

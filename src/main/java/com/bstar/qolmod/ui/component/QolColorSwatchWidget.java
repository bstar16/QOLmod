package com.bstar.qolmod.ui.component;

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

/** Compact keyboard-accessible accent swatch for the appearance settings page. */
public final class QolColorSwatchWidget extends ClickableWidget {
    private final int rgb;
    private final BooleanSupplier selected;
    private final Runnable action;

    public QolColorSwatchWidget(
            int x,
            int y,
            int rgb,
            BooleanSupplier selected,
            Runnable action,
            Text narration
    ) {
        super(x, y, 18, 16, narration);
        this.rgb = rgb & 0x00FFFFFF;
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
        int outline = selected.getAsBoolean() || isFocused()
                ? colors.primaryText()
                : colors.subtleDivider();
        context.drawStrokedRectangle(getX(), getY(), getWidth(), getHeight(), outline);
        context.fill(getX() + 2, getY() + 2, getRight() - 2, getBottom() - 2, 0xFF000000 | rgb);
        if (isHovered()) {
            context.drawStrokedRectangle(
                    getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, colors.accentHover()
            );
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}

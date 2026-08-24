package com.bstar.qolmod.ui.component;

import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/** Compact slider bound to an arbitrary in-memory double value. */
public final class QolValueSliderWidget extends SliderWidget {
    private final DoubleSupplier current;
    private final DoubleConsumer changed;
    private final DoubleFunction<String> formatter;
    private final double minimum;
    private final double maximum;

    public QolValueSliderWidget(
            int x,
            int y,
            int width,
            double minimum,
            double maximum,
            DoubleSupplier current,
            DoubleConsumer changed,
            DoubleFunction<String> formatter,
            Text narration
    ) {
        super(x, y, width, 14, narration, normalize(current.getAsDouble(), minimum, maximum));
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || maximum <= minimum) {
            throw new IllegalArgumentException("Slider bounds must be finite and increasing");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.current = Objects.requireNonNull(current, "current");
        this.changed = Objects.requireNonNull(changed, "changed");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    protected void updateMessage() {
        // The owning compact inspector renders its label and numeric value.
    }

    @Override
    protected void applyValue() {
        changed.accept(roundHundredths(minimum + value * (maximum - minimum)));
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        syncFromValue();
        ColorPalette colors = ThemeManager.active().colors();
        int centerY = getY() + getHeight() / 2;
        context.fill(getX(), centerY - 2, getRight(), centerY + 2, withAlpha(colors.mutedText(), 144));
        int knobCenter = getX() + 4 + (int) Math.round(value * Math.max(0, getWidth() - 8));
        context.fill(getX(), centerY - 2, knobCenter, centerY + 2, colors.accent());
        int knob = isHovered() || isFocused() ? colors.accentHover() : colors.primaryText();
        context.fill(knobCenter - 3, getY() + 2, knobCenter + 3, getBottom() - 2, knob);
    }

    public String formattedValue() {
        return formatter.apply(current.getAsDouble());
    }

    private void syncFromValue() {
        value = Math.max(0.0, Math.min(1.0, normalize(current.getAsDouble(), minimum, maximum)));
    }

    private static double normalize(double value, double minimum, double maximum) {
        return maximum <= minimum ? 0.0 : (value - minimum) / (maximum - minimum);
    }

    private static double roundHundredths(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}

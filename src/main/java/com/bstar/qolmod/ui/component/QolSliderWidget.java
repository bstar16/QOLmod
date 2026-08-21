package com.bstar.qolmod.ui.component;

import com.bstar.qolmod.setting.DoubleSetting;
import com.bstar.qolmod.setting.IntSetting;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class QolSliderWidget extends SliderWidget {
    private final IntSetting intSetting;
    private final DoubleSetting doubleSetting;
    private final Runnable changed;
    private final double sliderMin;
    private final double sliderMax;

    public QolSliderWidget(int x, int y, int width, IntSetting setting, Runnable changed) {
        this(x, y, width, setting, setting.max(), changed);
    }

    public QolSliderWidget(int x, int y, int width, IntSetting setting, int usefulSliderMax, Runnable changed) {
        super(x, y, width, 14, Text.literal(setting.displayName()),
                normalize(
                        Math.min(setting.get(), Math.max(setting.min(), usefulSliderMax)),
                        setting.min(),
                        Math.max(setting.min(), usefulSliderMax)
                ));
        intSetting = setting;
        doubleSetting = null;
        sliderMin = setting.min();
        sliderMax = Math.max(setting.min(), Math.min(setting.max(), usefulSliderMax));
        this.changed = changed;
    }

    public QolSliderWidget(int x, int y, int width, DoubleSetting setting, Runnable changed) {
        super(x, y, width, 14, Text.literal(setting.displayName()), normalize(setting.get(), setting.min(), setting.max()));
        intSetting = null;
        doubleSetting = setting;
        sliderMin = setting.min();
        sliderMax = setting.max();
        this.changed = changed;
    }

    @Override
    protected void updateMessage() {
        // The compact setting row renders its value in the adjacent exact input.
    }

    @Override
    protected void applyValue() {
        if (intSetting != null) {
            intSetting.set((int) Math.round(lerp(value, sliderMin, sliderMax)));
        } else {
            doubleSetting.set(roundHundredths(lerp(value, sliderMin, sliderMax)));
        }
        if (changed != null) {
            changed.run();
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ColorPalette colors = ThemeManager.active().colors();
        int centerY = getY() + getHeight() / 2;
        context.fill(getX(), centerY - 2, getRight(), centerY + 2, withAlpha(colors.mutedText(), 144));
        int knobCenter = getX() + 4 + (int) Math.round(value * Math.max(0, getWidth() - 8));
        context.fill(getX(), centerY - 2, knobCenter, centerY + 2, colors.accent());
        int knob = isHovered() || isFocused() ? colors.accentHover() : colors.primaryText();
        context.fill(knobCenter - 3, getY() + 2, knobCenter + 3, getBottom() - 2, knob);
    }

    public void syncFromSetting() {
        if (intSetting != null) {
            value = normalize(intSetting.get(), sliderMin, sliderMax);
        } else {
            value = normalize(doubleSetting.get(), sliderMin, sliderMax);
        }
        value = Math.max(0.0, Math.min(1.0, value));
    }

    private static double normalize(double value, double min, double max) {
        return max <= min ? 0.0 : (value - min) / (max - min);
    }

    private static double lerp(double value, double min, double max) {
        return min + value * (max - min);
    }

    private static double roundHundredths(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}

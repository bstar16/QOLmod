package com.bstar.qolmod.gui;

import com.bstar.qolmod.feature.setting.DoubleSetting;
import com.bstar.qolmod.feature.setting.IntSetting;
import java.util.Locale;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

final class NumericSettingSlider extends SliderWidget {
    private final IntSetting intSetting;
    private final DoubleSetting doubleSetting;

    NumericSettingSlider(int x, int y, int width, int height, IntSetting setting) {
        super(x, y, width, height, Text.empty(), toSliderValue(setting.get(), setting.min(), setting.max()));
        this.intSetting = setting;
        this.doubleSetting = null;
        updateMessage();
    }

    NumericSettingSlider(int x, int y, int width, int height, DoubleSetting setting) {
        super(x, y, width, height, Text.empty(), toSliderValue(setting.get(), setting.min(), setting.max()));
        this.intSetting = null;
        this.doubleSetting = setting;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        if (intSetting != null) {
            setMessage(Text.literal(intSetting.displayName() + ": " + intSetting.get()));
        } else {
            setMessage(Text.literal(doubleSetting.displayName() + ": " + String.format(Locale.ROOT, "%.2f", doubleSetting.get())));
        }
    }

    @Override
    protected void applyValue() {
        if (intSetting != null) {
            intSetting.set((int) Math.round(lerp(value, intSetting.min(), intSetting.max())));
        } else {
            doubleSetting.set(roundToHundredths(lerp(value, doubleSetting.min(), doubleSetting.max())));
        }
    }

    private static double toSliderValue(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }

        return (value - min) / (max - min);
    }

    private static double lerp(double value, double min, double max) {
        return min + value * (max - min);
    }

    private static double roundToHundredths(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

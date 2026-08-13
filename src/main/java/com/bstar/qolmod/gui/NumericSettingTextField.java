package com.bstar.qolmod.gui;

import com.bstar.qolmod.feature.setting.DoubleSetting;
import com.bstar.qolmod.feature.setting.IntSetting;
import java.util.Locale;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

final class NumericSettingTextField extends TextFieldWidget {
    private static final int MAX_DECIMAL_PLACES = 2;

    private final IntSetting intSetting;
    private final DoubleSetting doubleSetting;

    NumericSettingTextField(TextRenderer textRenderer, int x, int y, int width, int height, DoubleSetting setting) {
        super(textRenderer, x, y, width, height, Text.literal(setting.displayName()));
        this.intSetting = null;
        this.doubleSetting = setting;
        setMaxLength(8);
        setTextPredicate(this::isValidInput);
        setText(format(setting.get()));
        setChangedListener(this::updateSetting);
    }

    NumericSettingTextField(TextRenderer textRenderer, int x, int y, int width, int height, IntSetting setting) {
        super(textRenderer, x, y, width, height, Text.literal(setting.displayName()));
        this.intSetting = setting;
        this.doubleSetting = null;
        setMaxLength(8);
        setTextPredicate(this::isValidInput);
        setText(String.valueOf(setting.get()));
        setChangedListener(this::updateSetting);
    }

    private boolean isValidInput(String text) {
        if (text.isEmpty()) {
            return true;
        }

        if (intSetting != null) {
            return isValidIntInput(text);
        }

        return isValidDoubleInput(text);
    }

    private boolean isValidIntInput(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isDigit(text.charAt(index))) {
                return false;
            }
        }

        try {
            int value = Integer.parseInt(text);
            return value >= intSetting.min() && value <= intSetting.max();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isValidDoubleInput(String text) {
        if (".".equals(text)) {
            return true;
        }

        int decimalIndex = text.indexOf('.');
        if (decimalIndex != text.lastIndexOf('.')) {
            return false;
        }

        if (decimalIndex >= 0 && text.length() - decimalIndex - 1 > MAX_DECIMAL_PLACES) {
            return false;
        }

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (!Character.isDigit(character) && character != '.') {
                return false;
            }
        }

        try {
            double value = Double.parseDouble(text);
            return value >= doubleSetting.min() && value <= doubleSetting.max();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void updateSetting(String text) {
        if (text.isEmpty()) {
            return;
        }

        if (intSetting != null) {
            updateIntSetting(text);
        } else {
            updateDoubleSetting(text);
        }
    }

    private void updateIntSetting(String text) {
        try {
            intSetting.set(Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateDoubleSetting(String text) {
        if (".".equals(text)) {
            return;
        }

        try {
            doubleSetting.set(roundToHundredths(Double.parseDouble(text)));
        } catch (NumberFormatException ignored) {
        }
    }

    private static double roundToHundredths(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String format(double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        return formatted.replaceAll("\\.?0+$", "");
    }
}

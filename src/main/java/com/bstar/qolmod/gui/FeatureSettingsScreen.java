package com.bstar.qolmod.gui;

import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.DoubleSetting;
import com.bstar.qolmod.setting.IntSetting;
import com.bstar.qolmod.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FeatureSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 36;
    private static final Set<String> TEXT_FIELD_SETTING_IDS = Set.of("cycles", "chest-apply-delay");

    private final Screen parent;
    private final QOLFeature feature;
    private final ConfigManager configManager;
    private final List<SettingRow> rows = new ArrayList<>();
    private int scrollOffset;

    public FeatureSettingsScreen(Screen parent, QOLFeature feature, ConfigManager configManager) {
        super(Text.literal(feature.name()));
        this.parent = parent;
        this.feature = feature;
        this.configManager = configManager;
    }

    @Override
    protected void init() {
        rows.clear();
        int listWidth = Math.min(560, width - 40);
        int left = (width - listWidth) / 2;
        int labelWidth = Math.max(140, listWidth - 198);

        addDrawableChild(new TextWidget(left + 8, 38, labelWidth, 12, Text.literal("Setting").formatted(Formatting.GRAY), textRenderer));
        addDrawableChild(new TextWidget(left + listWidth - 174, 38, 80, 12, Text.literal("Value").formatted(Formatting.GRAY), textRenderer));

        for (Setting<?> setting : feature.settings()) {
            TextWidget nameLabel = new TextWidget(left + 8, 0, labelWidth, 10, Text.literal(setting.displayName()), textRenderer);
            TextWidget descriptionLabel = new TextWidget(left + 8, 0, labelWidth, 10, Text.literal(setting.description()).formatted(Formatting.GRAY), textRenderer);
            nameLabel.setMaxWidth(labelWidth);
            descriptionLabel.setMaxWidth(labelWidth);

            ClickableWidget widget = createSettingWidget(setting, left + listWidth - 178, 0, 174, 20);
            widget.setTooltip(Tooltip.of(Text.literal(setting.description())));
            addDrawableChild(nameLabel);
            addDrawableChild(descriptionLabel);
            addDrawableChild(widget);
            rows.add(new SettingRow(setting, nameLabel, descriptionLabel, widget));
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(width / 2 - 75, height - 28, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderFlatBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(feature.name()), width / 2, 14, 0xFFFFFF);

        int listWidth = Math.min(560, width - 40);
        int left = (width - listWidth) / 2;
        int top = 54;
        int bottom = height - 38;
        int visibleHeight = bottom - top;
        int maxScroll = Math.max(0, rows.size() * ROW_HEIGHT - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(left - 4, top - 4, left + listWidth + 4, bottom);
        for (int index = 0; index < rows.size(); index++) {
            SettingRow row = rows.get(index);
            int y = top + index * ROW_HEIGHT - scrollOffset;
            boolean visible = y > top - ROW_HEIGHT && y < bottom;

            row.nameLabel.visible = visible;
            row.descriptionLabel.visible = visible;
            row.widget.visible = visible;
            row.nameLabel.setY(y + 6);
            row.descriptionLabel.setY(y + 18);
            row.widget.setY(y + 6);

            if (visible) {
                context.fill(left - 2, y, left + listWidth + 2, y + 32, 0x55222222);
            }
        }
        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) (verticalAmount * ROW_HEIGHT);
        return true;
    }

    @Override
    public void close() {
        configManager.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        configManager.save();
    }

    private ClickableWidget createSettingWidget(Setting<?> setting, int x, int y, int width, int height) {
        if (setting instanceof BooleanSetting booleanSetting) {
            return ButtonWidget.builder(booleanText(booleanSetting), button -> {
                booleanSetting.set(!booleanSetting.get());
                button.setMessage(booleanText(booleanSetting));
                configManager.save();
            }).dimensions(x, y, width, height).build();
        }

        if (setting instanceof IntSetting intSetting) {
            if (TEXT_FIELD_SETTING_IDS.contains(intSetting.id())) {
                return new NumericSettingTextField(textRenderer, x, y, width, height, intSetting);
            }

            return new NumericSettingSlider(x, y, width, height, intSetting);
        }

        if (setting instanceof DoubleSetting doubleSetting) {
            if (TEXT_FIELD_SETTING_IDS.contains(doubleSetting.id())) {
                return new NumericSettingTextField(textRenderer, x, y, width, height, doubleSetting);
            }

            return new NumericSettingSlider(x, y, width, height, doubleSetting);
        }

        return ButtonWidget.builder(Text.literal(String.valueOf(setting.get())), button -> {
        }).dimensions(x, y, width, height).build();
    }

    private Text booleanText(BooleanSetting setting) {
        return Text.literal(setting.get() ? "On" : "Off");
    }

    private void renderFlatBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xD0101010);
    }

    private record SettingRow(Setting<?> setting, TextWidget nameLabel, TextWidget descriptionLabel, ClickableWidget widget) {
    }
}

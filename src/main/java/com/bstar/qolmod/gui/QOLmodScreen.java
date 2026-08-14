package com.bstar.qolmod.gui;

import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.QOLFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class QOLmodScreen extends Screen {
    private static final int ROW_HEIGHT = 28;

    private final Screen parent;
    private final FeatureManager featureManager;
    private final ConfigManager configManager;
    private final List<FeatureRow> rows = new ArrayList<>();
    private int scrollOffset;

    public QOLmodScreen(Screen parent, FeatureManager featureManager, ConfigManager configManager) {
        super(Text.literal("QOLmod"));
        this.parent = parent;
        this.featureManager = featureManager;
        this.configManager = configManager;
    }

    @Override
    protected void init() {
        rows.clear();
        int listWidth = Math.min(520, width - 40);
        int left = (width - listWidth) / 2;
        int labelWidth = Math.max(120, listWidth - 138);

        addDrawableChild(new TextWidget(left + 8, 38, labelWidth, 12, Text.literal("Feature").formatted(Formatting.GRAY), textRenderer));
        addDrawableChild(new TextWidget(left + listWidth - 114, 38, 70, 12, Text.literal("State").formatted(Formatting.GRAY), textRenderer));
        addDrawableChild(new TextWidget(left + listWidth - 34, 38, 40, 12, Text.literal("Config").formatted(Formatting.GRAY), textRenderer));

        for (QOLFeature feature : featureManager.all()) {
            TextWidget nameLabel = new TextWidget(left + 8, 0, labelWidth, 10, Text.literal(feature.name()), textRenderer);
            TextWidget descriptionLabel = new TextWidget(left + 8, 0, labelWidth, 10, Text.literal(feature.description()).formatted(Formatting.GRAY), textRenderer);
            nameLabel.setMaxWidth(labelWidth);
            descriptionLabel.setMaxWidth(labelWidth);

            ButtonWidget toggleButton = ButtonWidget.builder(toggleText(feature), button -> {
                featureManager.setEnabled(feature, !feature.isEnabled());
                button.setMessage(toggleText(feature));
                configManager.save();
            }).dimensions(left + listWidth - 118, 0, 84, 20).build();

            ButtonWidget configureButton = ButtonWidget.builder(Text.literal("⚙"), button ->
                    client.setScreen(new FeatureSettingsScreen(this, feature, configManager))
            ).dimensions(left + listWidth - 28, 0, 24, 20).tooltip(Tooltip.of(Text.literal("Configure " + feature.name()))).build();
            configureButton.active = feature.hasSettings();

            addDrawableChild(nameLabel);
            addDrawableChild(descriptionLabel);
            addDrawableChild(toggleButton);
            addDrawableChild(configureButton);
            rows.add(new FeatureRow(feature, nameLabel, descriptionLabel, toggleButton, configureButton));
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(width / 2 - 154, height - 28, 150, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> configManager.save())
                .dimensions(width / 2 + 4, height - 28, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderFlatBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);

        int listWidth = Math.min(520, width - 40);
        int left = (width - listWidth) / 2;
        int top = 54;
        int bottom = height - 38;
        int visibleHeight = bottom - top;
        int maxScroll = Math.max(0, rows.size() * ROW_HEIGHT - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(left - 4, top - 4, left + listWidth + 4, bottom);
        for (int index = 0; index < rows.size(); index++) {
            FeatureRow row = rows.get(index);
            int y = top + index * ROW_HEIGHT - scrollOffset;
            boolean visible = y > top - ROW_HEIGHT && y < bottom;

            row.nameLabel.visible = visible;
            row.descriptionLabel.visible = visible;
            row.toggleButton.visible = visible;
            row.configureButton.visible = visible;
            row.nameLabel.setY(y + 4);
            row.descriptionLabel.setY(y + 15);
            row.toggleButton.setY(y + 2);
            row.configureButton.setY(y + 2);
            row.toggleButton.setMessage(toggleText(row.feature));

            if (visible) {
                int rowColor = row.feature.isEnabled() ? 0x55346C3D : 0x55222222;
                context.fill(left - 2, y, left + listWidth + 2, y + 24, rowColor);
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

    private Text toggleText(QOLFeature feature) {
        return Text.literal(feature.isEnabled() ? "Enabled" : "Disabled");
    }

    private void renderFlatBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xD0101010);
    }

    private record FeatureRow(
            QOLFeature feature,
            TextWidget nameLabel,
            TextWidget descriptionLabel,
            ButtonWidget toggleButton,
            ButtonWidget configureButton
    ) {
    }
}

package com.bstar.qolmod.gui;

import com.bstar.qolmod.feature.impl.StorageLabelsFeature;
import com.bstar.qolmod.feature.labels.StorageLabel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class StorageLabelEditScreen extends Screen {
    private static final int MAX_LINES = 8;
    private static final int LINE_HEIGHT = 24;
    private static final List<ColorChoice> COLORS = List.of(
            new ColorChoice("White", 0xFFFFFF),
            new ColorChoice("Yellow", 0xFFFF55),
            new ColorChoice("Aqua", 0x55FFFF),
            new ColorChoice("Green", 0x55FF55),
            new ColorChoice("Red", 0xFF5555),
            new ColorChoice("Purple", 0xAA00AA),
            new ColorChoice("Gold", 0xFFAA00),
            new ColorChoice("Gray", 0xAAAAAA)
    );

    private final Screen parent;
    private final StorageLabelsFeature feature;
    private final StorageLabel label;
    private final boolean focusColor;
    private final List<TextFieldWidget> lineFields = new ArrayList<>();
    private List<String> draftLines;
    private TextFieldWidget aliasField;
    private TextFieldWidget colorField;
    private String iconItemId;
    private StorageLabel.IconPosition iconPosition;
    private int selectedColor;

    public StorageLabelEditScreen(Screen parent, StorageLabelsFeature feature, StorageLabel label, boolean focusColor) {
        super(Text.literal("Edit Storage Label"));
        this.parent = parent;
        this.feature = feature;
        this.label = label;
        this.focusColor = focusColor;
        this.iconItemId = label.iconItemId();
        this.iconPosition = label.iconPosition();
        this.selectedColor = label.textColor();
        this.draftLines = new ArrayList<>(label.lines());
    }

    @Override
    protected void init() {
        lineFields.clear();
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        int y = 50;

        aliasField = new TextFieldWidget(textRenderer, left, y, panelWidth, 20, Text.literal("Alias"));
        aliasField.setMaxLength(80);
        aliasField.setText(label.alias());
        aliasField.setTextPredicate(value -> !value.contains("\n") && !value.contains("\r"));
        addDrawableChild(aliasField);
        y += 34;

        for (String line : draftLines) {
            addLineField(left, y, panelWidth, line);
            y += LINE_HEIGHT;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Line"), button -> {
            if (lineFields.size() < MAX_LINES) {
                rebuildWithLines(readLinesWithExtra(""));
            }
        }).dimensions(left, y + 4, 96, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Line"), button -> {
            if (lineFields.size() > 1) {
                List<String> linesToKeep = readLines();
                linesToKeep.remove(linesToKeep.size() - 1);
                rebuildWithLines(linesToKeep);
            }
        }).dimensions(left + 104, y + 4, 104, 20).build());

        y += 38;
        addDrawableChild(ButtonWidget.builder(Text.literal("Icon Picker"), button ->
                client.setScreen(new StorageIconPickerScreen(this, feature, label))
        ).dimensions(left, y, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Icon"), button -> iconItemId = null)
                .dimensions(left + 112, y, 92, 20).build());
        addDrawableChild(ButtonWidget.builder(iconPositionText(), button -> {
            iconPosition = iconPosition.toggle();
            button.setMessage(iconPositionText());
        }).dimensions(left + 212, y, 150, 20).build());

        y += 34;
        int colorButtonWidth = 64;
        for (int index = 0; index < COLORS.size(); index++) {
            ColorChoice choice = COLORS.get(index);
            int buttonX = left + (index % 4) * (colorButtonWidth + 6);
            int buttonY = y + (index / 4) * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(choice.name()), button -> {
                selectedColor = choice.rgb();
                colorField.setText(formatHex(selectedColor));
            }).dimensions(buttonX, buttonY, colorButtonWidth, 20).build());
        }

        colorField = new TextFieldWidget(textRenderer, left + panelWidth - 112, y, 112, 20, Text.literal("Hex color"));
        colorField.setMaxLength(7);
        colorField.setText(formatHex(selectedColor));
        colorField.setChangedListener(this::updateColorFromHex);
        addDrawableChild(colorField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
                .dimensions(width / 2 - 156, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 - 50, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> saveAndClose())
                .dimensions(width / 2 + 56, height - 28, 100, 20).build());

        if (focusColor) {
            setInitialFocus(colorField);
        } else if (!lineFields.isEmpty()) {
            setInitialFocus(aliasField);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal("Alias").formatted(Formatting.GRAY), left, 34, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Visible Text").formatted(Formatting.GRAY), left, 68, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Color").formatted(Formatting.GRAY), left, 84 + lineFields.size() * LINE_HEIGHT + 72, 0xFFFFFF);
        String iconText = iconItemId == null ? "No icon" : "Icon: " + iconItemId;
        context.drawTextWithShadow(textRenderer, Text.literal(iconText).formatted(Formatting.GRAY), left + 370, 84 + lineFields.size() * LINE_HEIGHT + 8, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void addLineField(int left, int y, int width, String text) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, left, y, width, 20, Text.literal("Label line"));
        field.setMaxLength(120);
        field.setText(text);
        lineFields.add(field);
        addDrawableChild(field);
    }

    private void rebuildWithLines(List<String> lines) {
        String alias = aliasField == null ? label.alias() : aliasField.getText();
        draftLines = lines.stream().limit(MAX_LINES).toList();
        clearChildren();
        init();
        aliasField.setText(alias);
    }

    private List<String> readLinesWithExtra(String extra) {
        List<String> lines = readLines();
        lines.add(extra);
        return lines;
    }

    private List<String> readLines() {
        return new ArrayList<>(lineFields.stream().map(TextFieldWidget::getText).toList());
    }

    private String joinLines() {
        return String.join("\n", readLines()).trim();
    }

    private void updateColorFromHex(String value) {
        try {
            String normalized = value.trim();
            if (normalized.startsWith("#")) {
                normalized = normalized.substring(1);
            }
            if (normalized.length() == 6) {
                selectedColor = Integer.parseInt(normalized, 16) & 0xFFFFFF;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void saveAndClose() {
        feature.updateLabel(label, readLines(), aliasField.getText(), selectedColor, iconItemId, iconPosition);
        close();
    }

    void setIconItemId(String iconItemId) {
        this.iconItemId = iconItemId;
    }

    private String formatHex(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private Text iconPositionText() {
        return Text.literal("Icon Position: " + (iconPosition == StorageLabel.IconPosition.ABOVE ? "Above" : "Below"));
    }

    private record ColorChoice(String name, int rgb) {
    }
}

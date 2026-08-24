package com.bstar.qolmod.ui;

import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.hud.HudAnchor;
import com.bstar.qolmod.hud.HudManager;
import com.bstar.qolmod.hud.HudPosition;
import com.bstar.qolmod.hud.HudWidgetConfig;
import com.bstar.qolmod.hud.editor.EditableHudWidget;
import com.bstar.qolmod.hud.editor.HudGeometry;
import com.bstar.qolmod.hud.editor.HudRect;
import com.bstar.qolmod.hud.editor.HudSize;
import com.bstar.qolmod.hud.editor.HudSnapGuide;
import com.bstar.qolmod.hud.editor.HudSnapResult;
import com.bstar.qolmod.hud.editor.HudSnapper;
import com.bstar.qolmod.hud.render.GlassHudSurface;
import com.bstar.qolmod.hud.render.HudSurface;
import com.bstar.qolmod.input.KeybindManager;
import com.bstar.qolmod.ui.component.QolButtonWidget;
import com.bstar.qolmod.ui.component.QolToggleWidget;
import com.bstar.qolmod.ui.component.QolValueSliderWidget;
import com.bstar.qolmod.ui.render.UiStroke;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** In-world editor for registered contextual HUD widgets. */
public final class HudEditorScreen extends Screen implements HudSuppressingScreen {
    private static final int SAFE_MARGIN = 4;
    private static final int SNAP_DISTANCE = 6;
    private static final int ANCHOR_MARGIN = 10;
    private static final int INSPECTOR_MARGIN = 12;
    private static final int INSPECTOR_Y = 12;
    private static final int INSPECTOR_WIDTH = 214;
    private static final int INSPECTOR_HEIGHT = 224;
    private static final double EDITOR_MIN_OPACITY = 0.35;
    private static final double EDITOR_MAX_OPACITY = 1.0;
    private static final double EDITOR_MIN_SCALE = 0.6;
    private static final double EDITOR_MAX_SCALE = 1.5;

    private final Screen qolmodParent;
    private final FeatureManager featureManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final KeybindManager keybindManager;
    private final List<EditableHudWidget> widgets;
    private final HudSurface inspectorSurface = new GlassHudSurface();
    private EditableHudWidget selected;
    private EditableHudWidget dragged;
    private double dragPointerOffsetX;
    private double dragPointerOffsetY;
    private List<HudSnapGuide> snapGuides = List.of();
    private QolToggleWidget enabledToggle;
    private final List<AnchorButton> anchorButtons = new ArrayList<>();
    private QolValueSliderWidget scaleSlider;
    private QolValueSliderWidget opacitySlider;
    private QolButtonWidget resetPositionButton;
    private QolButtonWidget resetWidgetButton;
    private QolButtonWidget nextWidgetButton;
    private QolButtonWidget doneButton;
    private boolean dirty;

    public HudEditorScreen(
            Screen qolmodParent,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager,
            KeybindManager keybindManager
    ) {
        super(Text.literal("QOLMOD HUD EDITOR"));
        this.qolmodParent = qolmodParent;
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.hudManager = Objects.requireNonNull(hudManager, "hudManager");
        this.keybindManager = Objects.requireNonNull(keybindManager, "keybindManager");
        widgets = List.copyOf(hudManager.editableWidgets());
        selected = widgets.isEmpty() ? null : widgets.getFirst();
    }

    @Override
    protected void init() {
        ensureAllInBounds();
        anchorButtons.clear();
        enabledToggle = addDrawableChild(new QolToggleWidget(
                0,
                0,
                () -> selected != null && selected.config().enabled(),
                enabled -> {
                    if (selected != null) {
                        selected.config().setEnabled(enabled);
                        dirty = true;
                    }
                },
                Text.literal("Enable selected HUD widget")
        ));
        for (HudAnchor anchor : HudAnchor.values()) {
            QolButtonWidget button = addDrawableChild(new QolButtonWidget(
                    0,
                    0,
                    28,
                    20,
                    Text.literal(shortAnchorLabel(anchor)),
                    QolButtonWidget.Style.NAVIGATION,
                    () -> selected != null && selected.config().position().anchor() == anchor,
                    () -> selectAnchor(anchor)
            ));
            button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                    "Move to " + anchorLabel(anchor)
            )));
            anchorButtons.add(new AnchorButton(anchor, button));
        }
        scaleSlider = addDrawableChild(new QolValueSliderWidget(
                0, 0, 90, EDITOR_MIN_SCALE, EDITOR_MAX_SCALE,
                () -> selectedPosition().scale(),
                this::setScale,
                value -> String.format(Locale.ROOT, "%.2fx", value),
                Text.literal("Selected HUD scale")
        ));
        opacitySlider = addDrawableChild(new QolValueSliderWidget(
                0, 0, 90, EDITOR_MIN_OPACITY, EDITOR_MAX_OPACITY,
                () -> Math.max(EDITOR_MIN_OPACITY, selectedPosition().opacity()),
                this::setOpacity,
                value -> Math.round(value * 100.0) + "%",
                Text.literal("Selected HUD opacity")
        ));
        resetPositionButton = addDrawableChild(new QolButtonWidget(
                0, 0, 88, 20, Text.literal("Reset Position"), QolButtonWidget.Style.STANDARD, this::resetPosition
        ));
        resetWidgetButton = addDrawableChild(new QolButtonWidget(
                0, 0, 88, 20, Text.literal("Reset Widget"), QolButtonWidget.Style.GHOST, this::resetWidget
        ));
        nextWidgetButton = addDrawableChild(new QolButtonWidget(
                0, 0, 22, 18, Text.literal(">"), QolButtonWidget.Style.ICON, this::selectNextWidget
        ));
        doneButton = addDrawableChild(new QolButtonWidget(
                0, 0, 48, 18, Text.literal("Done"), QolButtonWidget.Style.GHOST, this::close
        ));
        positionControls();
        hudManager.editorInitialized();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client != null) {
            client.inGameHud.renderDeferredSubtitles();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ensureAllInBounds();
        positionControls();
        renderPreviews(context, mouseX, mouseY);
        renderGuides(context);
        renderInspector(context);
        updateControls();
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPreviews(DrawContext context, int mouseX, int mouseY) {
        for (EditableHudWidget widget : widgets) {
            HudSize size = widget.previewSize(client);
            HudPosition position = widget.config().position();
            HudRect bounds = HudGeometry.screenRect(position, size, width, height);
            double opacity = widget.config().enabled()
                    ? Math.max(EDITOR_MIN_OPACITY, position.opacity())
                    : EDITOR_MIN_OPACITY;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(bounds.x(), bounds.y());
            context.getMatrices().scale((float) position.scale(), (float) position.scale());
            try {
                widget.renderPreview(context, client, opacity);
            } finally {
                context.getMatrices().popMatrix();
            }
            renderWidgetBounds(context, widget, bounds, bounds.contains(mouseX, mouseY));
        }
    }

    private void renderWidgetBounds(
            DrawContext context,
            EditableHudWidget widget,
            HudRect bounds,
            boolean hovered
    ) {
        ColorPalette colors = ThemeManager.active().colors();
        boolean chosen = widget == selected;
        int outline = chosen ? colors.accentHover() : withAlpha(colors.secondaryText(), hovered ? 180 : 88);
        context.drawStrokedRectangle(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, outline);
        if (!chosen && !hovered) {
            return;
        }

        String label = widget.editorMetadata().displayName().toUpperCase(Locale.ROOT)
                + (widget.config().enabled() ? "" : "  •  DISABLED");
        int labelWidth = textRenderer.getWidth(label) + 8;
        int labelY = bounds.y() >= 16 ? bounds.y() - 13 : bounds.y() + 3;
        context.fill(bounds.x(), labelY, bounds.x() + labelWidth, labelY + 11,
                withAlpha(colors.surface(), 224));
        context.drawText(textRenderer, label, bounds.x() + 4, labelY + 2,
                widget.config().enabled() ? colors.primaryText() : colors.mutedText(), false);

        if (chosen) {
            int handle = colors.accent();
            corner(context, bounds.x() - 2, bounds.y() - 2, handle);
            corner(context, bounds.right() - 1, bounds.y() - 2, handle);
            corner(context, bounds.x() - 2, bounds.bottom() - 1, handle);
            corner(context, bounds.right() - 1, bounds.bottom() - 1, handle);
        }
    }

    private void corner(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 3, y + 3, color);
    }

    private void renderGuides(DrawContext context) {
        int color = withAlpha(ThemeManager.active().colors().accentHover(), 150);
        for (HudSnapGuide guide : snapGuides) {
            if (guide.axis() == HudSnapGuide.Axis.VERTICAL) {
                context.fill(guide.coordinate(), 0, guide.coordinate() + 1, height, color);
            } else {
                context.fill(0, guide.coordinate(), width, guide.coordinate() + 1, color);
            }
        }
    }

    private void renderInspector(DrawContext context) {
        ColorPalette colors = ThemeManager.active().colors();
        int inspectorX = inspectorX();
        inspectorSurface.draw(
                context,
                inspectorX,
                INSPECTOR_Y,
                INSPECTOR_WIDTH,
                INSPECTOR_HEIGHT,
                0.96,
                colors.accent()
        );
        int left = inspectorX + 12;
        int right = inspectorX + INSPECTOR_WIDTH - 12;
        context.drawText(textRenderer, "HUD EDITOR", left, INSPECTOR_Y + 10, colors.accentHover(), false);
        context.drawText(textRenderer, selected == null ? "Select a preview" : selected.editorMetadata().displayName(),
                left, INSPECTOR_Y + 27, colors.primaryText(), true);
        UiStroke.horizontal(context, left, right, INSPECTOR_Y + 43, colors.structuralDivider());

        if (selected == null) {
            context.drawText(textRenderer, "Click a HUD preview to edit it.", left, INSPECTOR_Y + 57,
                    colors.secondaryText(), false);
            return;
        }

        HudPosition position = selected.config().position();
        inspectorRow(context, "Enabled", INSPECTOR_Y + 53, null, colors);
        inspectorRow(context, "Anchor", INSPECTOR_Y + 79, null, colors);
        inspectorRow(context, "Scale", INSPECTOR_Y + 108, scaleSlider.formattedValue(), colors);
        inspectorRow(context, "Opacity", INSPECTOR_Y + 136, opacitySlider.formattedValue(), colors);
        inspectorRow(context, "Offsets", INSPECTOR_Y + 164,
                "X " + position.xOffset() + "   Y " + position.yOffset(), colors);
        context.drawText(textRenderer, "Drag previews directly • edge/center snap", left,
                INSPECTOR_Y + 210, colors.mutedText(), false);
    }

    private void inspectorRow(
            DrawContext context,
            String label,
            int y,
            String value,
            ColorPalette colors
    ) {
        int inspectorX = inspectorX();
        context.drawText(textRenderer, label, inspectorX + 12, y, colors.secondaryText(), false);
        if (value != null) {
            context.drawText(textRenderer, value,
                    inspectorX + INSPECTOR_WIDTH - 12 - textRenderer.getWidth(value), y,
                    colors.primaryText(), false);
        }
    }

    private void positionControls() {
        int inspectorX = inspectorX();
        int right = inspectorX + INSPECTOR_WIDTH - 12;
        enabledToggle.setX(right - enabledToggle.getWidth());
        enabledToggle.setY(INSPECTOR_Y + 50);
        int anchorX = inspectorX + 78;
        for (AnchorButton entry : anchorButtons) {
            entry.button.setX(anchorX);
            entry.button.setY(INSPECTOR_Y + 72);
            anchorX += entry.button.getWidth() + 3;
        }
        scaleSlider.setX(inspectorX + 70);
        scaleSlider.setY(INSPECTOR_Y + 101);
        opacitySlider.setX(inspectorX + 70);
        opacitySlider.setY(INSPECTOR_Y + 129);
        resetPositionButton.setX(inspectorX + 12);
        resetPositionButton.setY(INSPECTOR_Y + 184);
        resetWidgetButton.setX(inspectorX + 106);
        resetWidgetButton.setY(INSPECTOR_Y + 184);
        nextWidgetButton.setX(inspectorX + INSPECTOR_WIDTH - 36);
        nextWidgetButton.setY(INSPECTOR_Y + 24);
        doneButton.setX(inspectorX + INSPECTOR_WIDTH - 60);
        doneButton.setY(INSPECTOR_Y + 6);
    }

    private void updateControls() {
        boolean active = selected != null;
        enabledToggle.active = active;
        for (AnchorButton entry : anchorButtons) {
            entry.button.active = active;
        }
        scaleSlider.active = active;
        opacitySlider.active = active;
        resetPositionButton.active = active;
        resetWidgetButton.active = active;
        nextWidgetButton.active = widgets.size() > 1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (insideInspector(click.x(), click.y())) {
            return false;
        }
        EditableHudWidget hit = widgetAt(click.x(), click.y());
        if (hit != null) {
            selected = hit;
            dragged = hit;
            HudRect bounds = bounds(hit);
            dragPointerOffsetX = click.x() - bounds.x();
            dragPointerOffsetY = click.y() - bounds.y();
            return true;
        }
        if (!insideInspector(click.x(), click.y())) {
            selected = null;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragged == null) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        HudRect current = bounds(dragged);
        int desiredX = (int) Math.round(click.x() - dragPointerOffsetX);
        int desiredY = (int) Math.round(click.y() - dragPointerOffsetY);
        HudSnapResult snapped = HudSnapper.snap(
                desiredX, desiredY, current.width(), current.height(),
                width, height, SAFE_MARGIN, SNAP_DISTANCE
        );
        HudRect clamped = HudGeometry.clampRect(
                snapped.x(), snapped.y(), current.width(), current.height(),
                width, height, SAFE_MARGIN
        );
        HudWidgetConfig config = dragged.config();
        config.setPosition(HudGeometry.fromScreenPosition(
                config.position(), clamped.x(), clamped.y(), dragged.previewSize(client), width, height
        ));
        dirty = true;
        snapGuides = snapped.guides();
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragged != null) {
            dragged = null;
            snapGuides = List.of();
            saveIfDirty();
            return true;
        }
        boolean handled = super.mouseReleased(click);
        saveIfDirty();
        return handled;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        saveIfDirty();
        if (client != null) {
            client.setScreen(new QOLmodScreen(
                    qolmodParent,
                    featureManager,
                    configManager,
                    hudManager,
                    keybindManager,
                    QOLmodScreen.Page.HUD
            ));
        }
    }

    @Override
    public void removed() {
        saveIfDirty();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private EditableHudWidget widgetAt(double x, double y) {
        List<EditableHudWidget> reversed = new ArrayList<>(widgets);
        Collections.reverse(reversed);
        for (EditableHudWidget widget : reversed) {
            if (bounds(widget).contains(x, y)) {
                return widget;
            }
        }
        return null;
    }

    private HudRect bounds(EditableHudWidget widget) {
        return HudGeometry.screenRect(widget.config().position(), widget.previewSize(client), width, height);
    }

    private void selectAnchor(HudAnchor anchor) {
        if (selected == null) {
            return;
        }
        HudPosition position = selected.config().position();
        HudSize size = selected.previewSize(client);
        HudPosition anchored = HudGeometry.positionAtAnchor(
                position, anchor, size, width, height, ANCHOR_MARGIN
        );
        selected.config().setPosition(anchored);
        dirty = true;
        saveIfDirty();
    }

    private void setScale(double scale) {
        if (selected == null) {
            return;
        }
        HudPosition old = selected.config().position();
        selected.config().setPosition(new HudPosition(
                old.anchor(), old.xOffset(), old.yOffset(), scale, old.opacity()
        ));
        dirty = true;
        clamp(selected);
    }

    private void setOpacity(double opacity) {
        if (selected == null) {
            return;
        }
        HudPosition old = selected.config().position();
        selected.config().setPosition(new HudPosition(
                old.anchor(), old.xOffset(), old.yOffset(), old.scale(), opacity
        ));
        dirty = true;
    }

    private void resetPosition() {
        if (selected != null) {
            selected.config().resetPosition();
            dirty = true;
            clamp(selected);
            saveIfDirty();
        }
    }

    private void resetWidget() {
        if (selected != null) {
            selected.config().reset();
            dirty = true;
            clamp(selected);
            saveIfDirty();
        }
    }

    private void selectNextWidget() {
        if (widgets.isEmpty()) {
            selected = null;
            return;
        }
        int index = selected == null ? -1 : widgets.indexOf(selected);
        selected = widgets.get((index + 1) % widgets.size());
    }

    private void ensureAllInBounds() {
        if (client == null || width <= 0 || height <= 0) {
            return;
        }
        for (EditableHudWidget widget : widgets) {
            clamp(widget);
        }
    }

    private void clamp(EditableHudWidget widget) {
        HudPosition current = widget.config().position();
        HudPosition clamped = HudGeometry.clampPosition(
                widget.config().position(), widget.previewSize(client), width, height, SAFE_MARGIN
        );
        if (!current.equals(clamped)) {
            widget.config().setPosition(clamped);
            dirty = true;
        }
    }

    private void saveIfDirty() {
        if (!dirty) {
            return;
        }
        configManager.save();
        dirty = false;
    }

    private HudPosition selectedPosition() {
        return selected == null ? HudPosition.upperRightDefault() : selected.config().position();
    }

    private boolean insideInspector(double x, double y) {
        int inspectorX = inspectorX();
        return x >= inspectorX && x < inspectorX + INSPECTOR_WIDTH
                && y >= INSPECTOR_Y && y < INSPECTOR_Y + INSPECTOR_HEIGHT;
    }

    private String anchorLabel(HudAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> "Top Left";
            case TOP_RIGHT -> "Top Right";
            case BOTTOM_LEFT -> "Bottom Left";
            case BOTTOM_RIGHT -> "Bottom Right";
        };
    }

    private String shortAnchorLabel(HudAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> "TL";
            case TOP_RIGHT -> "TR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_RIGHT -> "BR";
        };
    }

    private int inspectorX() {
        int left = INSPECTOR_MARGIN;
        if (selected == null || width <= INSPECTOR_WIDTH + INSPECTOR_MARGIN * 2) {
            return left;
        }
        HudRect widget = bounds(selected);
        HudRect leftInspector = new HudRect(
                left, INSPECTOR_Y, INSPECTOR_WIDTH, INSPECTOR_HEIGHT
        );
        boolean overlaps = widget.x() < leftInspector.right() && widget.right() > leftInspector.x()
                && widget.y() < leftInspector.bottom() && widget.bottom() > leftInspector.y();
        return overlaps ? width - INSPECTOR_MARGIN - INSPECTOR_WIDTH : left;
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private record AnchorButton(HudAnchor anchor, QolButtonWidget button) {
    }
}

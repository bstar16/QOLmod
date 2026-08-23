package com.bstar.qolmod.ui;

import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.FeatureState;
import com.bstar.qolmod.feature.FeatureStatus;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.DoubleSetting;
import com.bstar.qolmod.setting.IntSetting;
import com.bstar.qolmod.setting.Setting;
import com.bstar.qolmod.setting.StringSetting;
import com.bstar.qolmod.ui.animation.AnimatedValue;
import com.bstar.qolmod.ui.component.QolButtonWidget;
import com.bstar.qolmod.ui.component.QolLabel;
import com.bstar.qolmod.ui.component.QolNavigationWidget;
import com.bstar.qolmod.ui.component.QolSliderWidget;
import com.bstar.qolmod.ui.component.QolTextInputWidget;
import com.bstar.qolmod.ui.component.QolToggleWidget;
import com.bstar.qolmod.ui.render.FramebufferGlassPanelMaterial;
import com.bstar.qolmod.ui.render.PanelMaterial;
import com.bstar.qolmod.ui.render.UiStroke;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Floating QOLmod interface with panel-local glass and in-shell contextual settings. */
public final class QOLmodScreen extends Screen {
    private static final int HEADER_HEIGHT = 45;
    private static final int FOOTER_HEIGHT = 27;
    private static final int SETTINGS_HEADER_HEIGHT = 38;
    private static final int SIDEBAR_WIDTH = 112;
    private static final int FEATURE_ROW_HEIGHT = 52;
    private static final int MAX_PANEL_WIDTH = 560;
    private static final long DRAWER_DURATION_MS = 210;
    private static final List<String> SPLASHES = List.of(
            "Built around actual gameplay annoyances.",
            "Quality of life, not feature bloat.",
            "Automation should describe intent, not fight the player.",
            "Small improvements. Better gameplay.",
            "Probably should have committed that file.",
            "It worked on the first try. Probably.",
            "Powered by questionable amounts of testing.",
            "One more feature, then we're done.",
            "Yes, this could have been a command.",
            "No, we don't need another module."
    );

    private final Screen parent;
    private final FeatureManager featureManager;
    private final ConfigManager configManager;
    private final PanelMaterial material;
    private final String splash;
    private final AnimatedValue drawerAnimation = new AnimatedValue(0.0, DRAWER_DURATION_MS, AnimatedValue.Easing.CUBIC_OUT);
    private final List<FeatureWidgets> featureWidgets = new ArrayList<>();
    private final List<QolNavigationWidget> navigationWidgets = new ArrayList<>();
    private final List<SettingControl> settingControls = new ArrayList<>();
    private QolButtonWidget settingsBackButton;
    private QolButtonWidget closeButton;
    private Page page = Page.FEATURES;
    private QOLFeature drawerFeature;
    private boolean drawerOpen;
    private int drawerScroll;

    public QOLmodScreen(Screen parent, FeatureManager featureManager, ConfigManager configManager) {
        this(parent, featureManager, configManager, new FramebufferGlassPanelMaterial());
    }

    QOLmodScreen(Screen parent, FeatureManager featureManager, ConfigManager configManager, PanelMaterial material) {
        super(Text.literal("QUALITY OF LIFE MOD"));
        this.parent = parent;
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.material = Objects.requireNonNull(material, "material");
        splash = SPLASHES.get(ThreadLocalRandom.current().nextInt(SPLASHES.size()));
    }

    @Override
    protected void init() {
        featureWidgets.clear();
        navigationWidgets.clear();
        settingControls.clear();
        addNavigationWidgets();
        addFeatureWidgets();
        addShellButtons();
        if (drawerFeature != null) {
            addSettingControls(drawerFeature);
        }
    }

    private void addNavigationWidgets() {
        for (Page candidate : Page.values()) {
            QolNavigationWidget button = new QolNavigationWidget(
                    0,
                    0,
                    SIDEBAR_WIDTH - 16,
                    24,
                    Text.literal(candidate.label),
                    candidate.icon,
                    () -> page == candidate,
                    () -> selectPage(candidate)
            );
            navigationWidgets.add(addDrawableChild(button));
        }
    }

    private void addShellButtons() {
        settingsBackButton = addDrawableChild(new QolButtonWidget(
                0, 0, 22, 20, Text.literal("<"), QolButtonWidget.Style.ICON, this::closeDrawer
        ));
        settingsBackButton.setTooltip(Tooltip.of(Text.literal("Back to features")));
        settingsBackButton.visible = false;
        settingsBackButton.active = false;

        closeButton = addDrawableChild(new QolButtonWidget(
                0, 0, 20, 18, Text.literal("×"), QolButtonWidget.Style.ICON, this::close
        ));
        closeButton.setTooltip(Tooltip.of(Text.literal("Close QOLmod")));
    }

    private void addFeatureWidgets() {
        for (QOLFeature feature : featureManager.all()) {
            QolToggleWidget toggle = new QolToggleWidget(
                    0,
                    0,
                    feature::isEnabled,
                    enabled -> {
                        featureManager.setEnabled(feature, enabled);
                        configManager.save();
                    },
                    Text.literal("Toggle " + feature.name())
            );
            toggle.setTooltip(Tooltip.of(Text.literal(feature.description())));
            QolButtonWidget configure = new QolButtonWidget(
                    0,
                    0,
                    22,
                    20,
                    Text.literal(">"),
                    QolButtonWidget.Style.ICON,
                    () -> openDrawer(feature)
            );
            configure.active = feature.hasSettings();
            configure.setTooltip(Tooltip.of(Text.literal(feature.hasSettings()
                    ? "Configure " + feature.name()
                    : feature.name() + " has no settings")));
            featureWidgets.add(new FeatureWidgets(feature, addDrawableChild(toggle), addDrawableChild(configure)));
        }
    }

    private void addSettingControls(QOLFeature feature) {
        for (Setting<?> setting : feature.settings()) {
            SettingGroup group = groupFor(setting);
            if (setting instanceof BooleanSetting booleanSetting) {
                QolToggleWidget toggle = addDrawableChild(new QolToggleWidget(
                        0,
                        0,
                        booleanSetting::get,
                        value -> {
                            booleanSetting.set(value);
                            configManager.save();
                        },
                        Text.literal(setting.displayName())
                ));
                toggle.setTooltip(Tooltip.of(Text.literal(setting.description())));
                settingControls.add(SettingControl.toggle(setting, group, toggle));
            } else if (setting instanceof IntSetting intSetting) {
                int usefulSliderMax = setting.id().equals("cycles") ? Math.min(intSetting.max(), 100) : intSetting.max();
                addNumericControl(setting, group,
                        new QolSliderWidget(0, 0, 80, intSetting, usefulSliderMax, () -> {}), intSetting);
            } else if (setting instanceof DoubleSetting doubleSetting) {
                addNumericControl(setting, group, new QolSliderWidget(0, 0, 80, doubleSetting, () -> {}), doubleSetting);
            } else if (setting instanceof StringSetting stringSetting) {
                QolTextInputWidget input = new QolTextInputWidget(textRenderer, 0, 0, 100, 18, Text.literal(setting.displayName()));
                input.setMaxLength(256);
                input.setText(stringSetting.get());
                input.setChangedListener(stringSetting::set);
                input.setTooltip(Tooltip.of(Text.literal(setting.description())));
                settingControls.add(SettingControl.input(setting, group, addDrawableChild(input)));
            }
        }
    }

    private void addNumericControl(Setting<?> setting, SettingGroup group, QolSliderWidget slider, Object numericSetting) {
        QolTextInputWidget input = new QolTextInputWidget(textRenderer, 0, 0, 47, 18, Text.literal(setting.displayName()));
        input.setMaxLength(12);
        input.setText(formatNumber(setting.get()));
        if (numericSetting instanceof IntSetting intSetting) {
            input.setTextPredicate(value -> validInt(value, intSetting));
            input.setChangedListener(value -> parseInt(value, intSetting, slider));
        } else if (numericSetting instanceof DoubleSetting doubleSetting) {
            input.setTextPredicate(value -> validDouble(value, doubleSetting));
            input.setChangedListener(value -> parseDouble(value, doubleSetting, slider));
        }
        Tooltip tooltip = Tooltip.of(Text.literal(setting.description()));
        slider.setTooltip(tooltip);
        input.setTooltip(tooltip);
        settingControls.add(SettingControl.numeric(
                setting,
                group,
                addDrawableChild(slider),
                addDrawableChild(input)
        ));
    }

    private void selectPage(Page selected) {
        page = selected;
        if (selected != Page.FEATURES) {
            closeDrawer();
        }
    }

    private void openDrawer(QOLFeature feature) {
        if (!feature.hasSettings()) {
            return;
        }
        if (drawerFeature != feature) {
            removeSettingControls();
            drawerFeature = feature;
            drawerScroll = 0;
            addSettingControls(feature);
        }
        drawerOpen = true;
        drawerAnimation.setTarget(1.0);
        setFeatureControlsVisible(false);
    }

    private void closeDrawer() {
        drawerOpen = false;
        drawerAnimation.setTarget(0.0);
        setDrawerControlsInteractive(false, true);
    }

    private void removeSettingControls() {
        for (SettingControl control : settingControls) {
            for (ClickableWidget widget : control.widgets()) {
                remove(widget);
            }
        }
        settingControls.clear();
    }

    /**
     * Screen.renderWithTooltip invokes this before render(). The vanilla implementation applies
     * menu blur and darkening across the whole in-game framebuffer, so QOLmod deliberately leaves
     * that framebuffer untouched and performs all background treatment in its panel material.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client != null) {
            client.inGameHud.renderDeferredSubtitles();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        double drawerProgress = drawerAnimation.value();
        Layout layout = layout();
        material.prepareFrame(context, layout.mainX, layout.panelY, layout.mainWidth, layout.panelHeight);
        material.drawMainPanel(context, layout.mainX, layout.panelY, layout.mainWidth, layout.panelHeight);
        renderMain(context, layout, mouseX, mouseY, drawerProgress);
        if (drawerFeature != null && drawerProgress > 0.002) {
            renderDrawer(context, layout, mouseX, mouseY, drawerProgress);
        } else if (!drawerOpen && drawerFeature != null && drawerAnimation.isAtTarget()) {
            setDrawerControlsInteractive(false, false);
        }

        positionWidgets(layout, drawerProgress);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMain(DrawContext context, Layout layout, int mouseX, int mouseY, double drawerProgress) {
        ColorPalette colors = ThemeManager.active().colors();
        int x = layout.mainX;
        int y = layout.panelY;
        int right = x + layout.mainWidth;
        boolean settingsContentActive = settingsContentActive(drawerProgress);

        UiStroke.horizontal(context, x + 16, Math.min(right - 16, x + 82), y + 2, colors.accentHover());
        new QolLabel(Text.literal("QUALITY OF LIFE MOD"), colors.primaryText()).draw(context, textRenderer, x + 16, y + 10);
        String metadata = layout.mainWidth < 390 ? "v0.1.0  •  1.21.11" : "v0.1.0  •  Fabric 1.21.11";
        context.drawText(textRenderer, metadata, x + 16, y + 27, colors.mutedText(), false);
        int footerY = y + layout.panelHeight - FOOTER_HEIGHT;
        UiStroke.structuralGrid(context, x + 1, right - 1, y + HEADER_HEIGHT - 1, footerY,
                x + SIDEBAR_WIDTH, colors.structuralDivider());

        if (page == Page.FEATURES && !settingsContentActive) {
            renderFeatures(context, layout, mouseX, mouseY);
        } else if (page != Page.FEATURES) {
            renderPlaceholder(context, layout, page);
        }

        String credit = "Coded By Codex  •  Designed By bstar";
        context.drawText(textRenderer, credit, right - 14 - textRenderer.getWidth(credit), footerY + 10, colors.mutedText(), false);

        if (page == Page.FEATURES && !settingsContentActive) {
            int contentLeft = x + SIDEBAR_WIDTH + 16;
            int preferredSplashY = y + HEADER_HEIGHT + 16 + featureWidgets.size() * featureRowHeight(layout) + 12;
            if (preferredSplashY <= footerY - 14) {
                context.drawCenteredTextWithShadow(
                        textRenderer,
                        trimToWidth(splash, layout.mainWidth - SIDEBAR_WIDTH - 32),
                        contentLeft + (layout.mainWidth - SIDEBAR_WIDTH - 28) / 2,
                        preferredSplashY,
                        colors.secondaryText()
                );
            }
        }
    }

    private void renderFeatures(DrawContext context, Layout layout, int mouseX, int mouseY) {
        ColorPalette colors = ThemeManager.active().colors();
        int left = layout.mainX + SIDEBAR_WIDTH + 14;
        int right = layout.mainX + layout.mainWidth - 14;
        int top = layout.panelY + HEADER_HEIGHT + 12;
        context.drawText(textRenderer, "FEATURES", left, top, colors.mutedText(), false);
        int rowY = top + 15;
        int rowHeight = featureRowHeight(layout);
        for (FeatureWidgets widgets : featureWidgets) {
            QOLFeature feature = widgets.feature;
            boolean hovered = mouseX >= left && mouseX < right && mouseY >= rowY && mouseY < rowY + rowHeight - 4;
            context.fill(left, rowY, right, rowY + rowHeight - 4,
                    withAlpha(colors.elevatedSurface(), hovered ? 90 : 57));
            UiStroke.horizontal(context, left, right, rowY + rowHeight - 5, colors.subtleDivider());
            context.drawText(textRenderer, feature.name(), left + 9, rowY + 9, colors.primaryText(), false);

            String state = stateLabel(feature);
            if (right - left < 200) {
                state = switch (state) {
                    case "WAITING" -> "WAIT";
                    case "ACTIVE" -> "RUN";
                    default -> state;
                };
            }
            int stateColor = stateColor(feature, colors);
            int stateX = right - 62 - textRenderer.getWidth(state);
            context.drawText(textRenderer, state, stateX, rowY + 9, stateColor, false);

            String contextLine = contextualStatus(feature);
            if (contextLine == null && hovered) {
                contextLine = feature.description();
            }
            if (contextLine != null) {
                context.drawText(textRenderer, trimToWidth(contextLine, right - left - 82), left + 9, rowY + 27,
                        feature.status().state() == FeatureState.ERROR ? colors.error() : colors.secondaryText(), false);
            }
            rowY += rowHeight;
        }
    }

    private void renderPlaceholder(DrawContext context, Layout layout, Page selected) {
        ColorPalette colors = ThemeManager.active().colors();
        int contentLeft = layout.mainX + SIDEBAR_WIDTH + 14;
        int contentWidth = layout.mainWidth - SIDEBAR_WIDTH - 28;
        int centerX = contentLeft + contentWidth / 2;
        int centerY = layout.panelY + layout.panelHeight / 2 - 8;
        context.drawCenteredTextWithShadow(textRenderer, selected.label.toUpperCase(Locale.ROOT), centerX, centerY - 10, colors.primaryText());
        context.drawCenteredTextWithShadow(textRenderer, selected.placeholder, centerX, centerY + 8,
                colors.secondaryText());
    }

    private void renderDrawer(DrawContext context, Layout layout, int mouseX, int mouseY, double progress) {
        int reveal = Math.max(1, (int) Math.round(layout.drawerWidth * progress));
        int revealLeft = layout.drawerX + layout.drawerWidth - reveal;
        int revealRight = layout.drawerX + layout.drawerWidth;
        context.enableScissor(revealLeft, layout.drawerY, revealRight, layout.drawerY + layout.drawerHeight);
        material.drawDrawer(
                context,
                layout.drawerX,
                layout.drawerY,
                layout.drawerWidth,
                layout.drawerHeight,
                false,
                revealLeft,
                revealRight
        );

        ColorPalette colors = ThemeManager.active().colors();
        int contentOffset = (int) Math.round((1.0 - progress) * 18.0);
        int left = layout.drawerX + 42 + contentOffset;
        int right = layout.drawerX + layout.drawerWidth - 14 + contentOffset;
        int titleWidth = Math.max(1, right - textRenderer.getWidth("SETTINGS") - 8 - left);
        context.drawText(textRenderer, trimToWidth(drawerFeature.name(), titleWidth), left,
                layout.drawerY + 14, colors.primaryText(), false);
        context.drawText(textRenderer, "SETTINGS", right - textRenderer.getWidth("SETTINGS"), layout.drawerY + 15, colors.mutedText(), false);
        UiStroke.horizontal(context, layout.drawerX + 12, layout.drawerX + layout.drawerWidth - 12,
                layout.drawerY + SETTINGS_HEADER_HEIGHT - 1, colors.structuralDivider());

        renderSettingGroups(context, layout, contentOffset);
        context.disableScissor();
    }

    private void renderSettingGroups(DrawContext context, Layout layout, int contentOffset) {
        int contentLeft = layout.drawerX + 14 + contentOffset;
        int contentWidth = layout.drawerWidth - 28;
        boolean twoColumns = contentWidth >= 302;
        int gap = twoColumns ? 12 : 0;
        int columnWidth = twoColumns ? (contentWidth - gap) / 2 : contentWidth;
        int top = layout.drawerY + SETTINGS_HEADER_HEIGHT + 9 - drawerScroll;
        int bottom = layout.drawerY + layout.drawerHeight - 8;
        context.enableScissor(layout.drawerX + 4, layout.drawerY + SETTINGS_HEADER_HEIGHT + 2,
                layout.drawerX + layout.drawerWidth - 4, layout.drawerY + layout.drawerHeight - 4);

        if (twoColumns) {
            int leftY = drawSettingGroup(context, SettingGroup.GENERAL, contentLeft, top, columnWidth, bottom);
            drawSettingGroup(context, SettingGroup.BEHAVIOUR, contentLeft, leftY + 6, columnWidth, bottom);
            drawSettingGroup(context, SettingGroup.TIMINGS, contentLeft + columnWidth + gap, top, columnWidth, bottom);
        } else {
            int y = top;
            for (SettingGroup group : SettingGroup.values()) {
                if (hasGroup(group)) {
                    y = drawSettingGroup(context, group, contentLeft, y, columnWidth, bottom) + 6;
                }
            }
        }
        context.disableScissor();
    }

    private int drawSettingGroup(DrawContext context, SettingGroup group, int x, int y, int width, int bottom) {
        if (!hasGroup(group)) {
            return y;
        }
        ColorPalette colors = ThemeManager.active().colors();
        context.drawText(textRenderer, group.label, x, y, colors.accentHover(), false);
        y += 14;
        for (SettingControl control : settingControls) {
            if (control.group != group) {
                continue;
            }
            context.drawText(textRenderer, control.setting.displayName(), x, y + 2, colors.secondaryText(), false);
            if (control.numeric()) {
                String unit = unitFor(control.setting);
                int unitX = x + width - textRenderer.getWidth(unit);
                context.drawText(textRenderer, unit, unitX, y + 19, colors.mutedText(), false);
            }
            y += control.rowHeight();
        }
        return y;
    }

    private void positionWidgets(Layout layout, double drawerProgress) {
        int navX = layout.mainX + 8;
        int navY = layout.panelY + HEADER_HEIGHT + 8;
        int navStep = Math.max(19, Math.min(27,
                (layout.panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT - 10) / Page.values().length));
        for (QolNavigationWidget navigation : navigationWidgets) {
            navigation.setX(navX);
            navigation.setY(navY);
            navigation.setWidth(SIDEBAR_WIDTH - 16);
            navigation.setHeight(Math.max(17, navStep - 3));
            navY += navStep;
        }

        closeButton.setX(layout.mainX + layout.mainWidth - 28);
        closeButton.setY(layout.panelY + 9);

        settingsBackButton.setX(layout.drawerX + 10);
        settingsBackButton.setY(layout.drawerY + 9);
        settingsBackButton.visible = drawerFeature != null && drawerOpen && drawerProgress > 0.97;
        settingsBackButton.active = drawerOpen && drawerProgress > 0.985;

        boolean mainControlsVisible = page == Page.FEATURES && !settingsContentActive(drawerProgress);
        int rowY = layout.panelY + HEADER_HEIGHT + 27;
        int rowHeight = featureRowHeight(layout);
        int right = layout.mainX + layout.mainWidth - 14;
        for (FeatureWidgets widgets : featureWidgets) {
            widgets.toggle.setX(right - 55);
            widgets.toggle.setY(rowY + 5);
            widgets.configure.setX(right - 24);
            widgets.configure.setY(rowY + 2);
            rowY += rowHeight;
        }
        setFeatureControlsVisible(mainControlsVisible);

        positionSettingControls(layout, drawerProgress);
    }

    private boolean settingsContentActive(double drawerProgress) {
        return drawerFeature != null && (drawerOpen || drawerProgress > 0.002);
    }

    private void setFeatureControlsVisible(boolean visible) {
        for (FeatureWidgets widgets : featureWidgets) {
            widgets.toggle.visible = visible;
            widgets.toggle.active = visible;
            widgets.configure.visible = visible;
            widgets.configure.active = visible && widgets.feature.hasSettings();
        }
    }

    private void positionSettingControls(Layout layout, double progress) {
        boolean visible = drawerFeature != null && drawerOpen && progress > 0.97;
        boolean interactive = drawerOpen && progress > 0.985;
        int offset = (int) Math.round((1.0 - progress) * 18.0);
        int contentLeft = layout.drawerX + 14 + offset;
        int contentWidth = layout.drawerWidth - 28;
        boolean twoColumns = contentWidth >= 302;
        int gap = twoColumns ? 12 : 0;
        int columnWidth = twoColumns ? (contentWidth - gap) / 2 : contentWidth;
        int top = layout.drawerY + SETTINGS_HEADER_HEIGHT + 9 - drawerScroll;
        Map<SettingGroup, Integer> groupY = new EnumMap<>(SettingGroup.class);
        if (twoColumns) {
            groupY.put(SettingGroup.GENERAL, top);
            groupY.put(SettingGroup.TIMINGS, top);
            int generalEnd = projectedGroupEnd(SettingGroup.GENERAL, top);
            groupY.put(SettingGroup.BEHAVIOUR, generalEnd + 6);
        } else {
            int y = top;
            for (SettingGroup group : SettingGroup.values()) {
                groupY.put(group, y);
                y = projectedGroupEnd(group, y) + (hasGroup(group) ? 6 : 0);
            }
        }

        Map<SettingGroup, Integer> cursor = new EnumMap<>(SettingGroup.class);
        for (SettingGroup group : SettingGroup.values()) {
            cursor.put(group, groupY.getOrDefault(group, top) + (hasGroup(group) ? 14 : 0));
        }
        for (SettingControl control : settingControls) {
            int columnX = contentLeft;
            if (twoColumns && control.group == SettingGroup.TIMINGS) {
                columnX += columnWidth + gap;
            }
            int y = cursor.get(control.group);
            int viewportTop = layout.drawerY + SETTINGS_HEADER_HEIGHT + 2;
            int viewportBottom = layout.drawerY + layout.drawerHeight - 4;
            boolean rowVisible = visible && y + control.rowHeight() > viewportTop && y < viewportBottom;
            positionControl(control, columnX, y, columnWidth, rowVisible, interactive && rowVisible);
            cursor.put(control.group, y + control.rowHeight());
        }
    }

    private void positionControl(SettingControl control, int x, int y, int width, boolean visible, boolean interactive) {
        if (control.toggle != null) {
            control.toggle.setX(x + width - control.toggle.getWidth());
            control.toggle.setY(y);
        } else if (control.slider != null && control.input != null) {
            String unit = unitFor(control.setting);
            int unitWidth = unit.isEmpty() ? 0 : textRenderer.getWidth(unit) + 4;
            int inputWidth = 47;
            int sliderWidth = Math.max(34, width - inputWidth - unitWidth - 7);
            control.slider.setX(x);
            control.slider.setY(y + 16);
            control.slider.setWidth(sliderWidth);
            control.input.setX(x + sliderWidth + 5);
            control.input.setY(y + 14);
            control.input.setWidth(inputWidth);
            if (!control.input.isFocused()) {
                String formatted = formatNumber(control.setting.get());
                if (!control.input.getText().equals(formatted)) {
                    control.input.setText(formatted);
                }
            }
        } else if (control.input != null) {
            control.input.setX(x);
            control.input.setY(y + 14);
            control.input.setWidth(width);
        }
        for (ClickableWidget widget : control.widgets()) {
            widget.visible = visible;
            widget.active = interactive;
        }
    }

    private int projectedGroupEnd(SettingGroup group, int start) {
        if (!hasGroup(group)) {
            return start;
        }
        int y = start + 14;
        for (SettingControl control : settingControls) {
            if (control.group == group) {
                y += control.rowHeight();
            }
        }
        return y;
    }

    private boolean hasGroup(SettingGroup group) {
        return settingControls.stream().anyMatch(control -> control.group == group);
    }

    private void setDrawerControlsInteractive(boolean interactive, boolean visible) {
        if (settingsBackButton != null) {
            settingsBackButton.active = interactive;
            settingsBackButton.visible = visible;
        }
        for (SettingControl control : settingControls) {
            for (ClickableWidget widget : control.widgets()) {
                widget.active = interactive;
                widget.visible = visible;
            }
        }
    }

    private Layout layout() {
        int margin = Math.max(10, Math.min(18, width / 40));
        int mainWidth = Math.max(286, Math.min(MAX_PANEL_WIDTH, width - margin * 2));
        int panelHeight = Math.max(214, Math.min(360, height - margin * 2));
        panelHeight = Math.min(panelHeight, height - 4);
        int mainX = (width - mainWidth) / 2;
        int panelY = Math.max(2, (height - panelHeight) / 2);
        int drawerX = mainX + SIDEBAR_WIDTH + 1;
        int drawerY = panelY + HEADER_HEIGHT;
        int drawerWidth = Math.max(1, mainWidth - SIDEBAR_WIDTH - 2);
        int drawerHeight = Math.max(1, panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT);
        return new Layout(mainX, panelY, mainWidth, panelHeight, drawerX, drawerY, drawerWidth, drawerHeight);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (page != Page.FEATURES || settingsContentActive(drawerAnimation.value())) {
            return false;
        }

        FeatureWidgets row = featureRowAt(click.x(), click.y());
        if (row == null) {
            return false;
        }
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            featureManager.setEnabled(row.feature, !row.feature.isEnabled());
            configManager.save();
            return true;
        }
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && row.feature.hasSettings()) {
            openDrawer(row.feature);
            return true;
        }
        return false;
    }

    private FeatureWidgets featureRowAt(double mouseX, double mouseY) {
        Layout layout = layout();
        int left = layout.mainX + SIDEBAR_WIDTH + 14;
        int right = layout.mainX + layout.mainWidth - 14;
        if (mouseX < left || mouseX >= right) {
            return null;
        }

        int rowY = layout.panelY + HEADER_HEIGHT + 27;
        int rowHeight = featureRowHeight(layout);
        for (FeatureWidgets widgets : featureWidgets) {
            if (mouseY >= rowY && mouseY < rowY + rowHeight - 4) {
                return widgets;
            }
            rowY += rowHeight;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double progress = drawerAnimation.value();
        Layout layout = layout();
        if (drawerFeature != null && progress > 0.98
                && mouseX >= layout.drawerX && mouseX < layout.drawerX + layout.drawerWidth
                && mouseY >= layout.drawerY && mouseY < layout.drawerY + layout.drawerHeight) {
            int maxScroll = maxDrawerScroll(layout);
            drawerScroll = Math.max(0, Math.min(maxScroll, drawerScroll - (int) Math.round(verticalAmount * 22.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int maxDrawerScroll(Layout layout) {
        int contentWidth = layout.drawerWidth - 28;
        boolean twoColumns = contentWidth >= 302;
        int contentHeight;
        if (twoColumns) {
            int left = projectedGroupEnd(SettingGroup.BEHAVIOUR,
                    projectedGroupEnd(SettingGroup.GENERAL, 0) + 6);
            int right = projectedGroupEnd(SettingGroup.TIMINGS, 0);
            contentHeight = Math.max(left, right);
        } else {
            int y = 0;
            for (SettingGroup group : SettingGroup.values()) {
                y = projectedGroupEnd(group, y) + (hasGroup(group) ? 6 : 0);
            }
            contentHeight = y;
        }
        int available = layout.drawerHeight - SETTINGS_HEADER_HEIGHT - 12;
        return Math.max(0, contentHeight - available);
    }

    private int featureRowHeight(Layout layout) {
        int rowTop = layout.panelY + HEADER_HEIGHT + 27;
        int footerTop = layout.panelY + layout.panelHeight - FOOTER_HEIGHT;
        int available = Math.max(1, footerTop - rowTop - 2);
        return Math.max(38, Math.min(FEATURE_ROW_HEIGHT, available / Math.max(1, featureWidgets.size())));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && drawerOpen) {
            closeDrawer();
            return true;
        }
        return super.keyPressed(input);
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
        material.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private String stateLabel(QOLFeature feature) {
        return switch (feature.status().state()) {
            case ERROR -> "ERROR";
            case COMPLETED -> "DONE";
            case WAITING -> feature.isEnabled() ? "WAITING" : "OFF";
            case RUNNING, PAUSED -> "auto-duper".equals(feature.id()) ? "ACTIVE" : (feature.isEnabled() ? "ON" : "OFF");
            case IDLE -> feature.isEnabled() ? "ON" : "OFF";
        };
    }

    private int stateColor(QOLFeature feature, ColorPalette colors) {
        return switch (feature.status().state()) {
            case ERROR -> colors.error();
            case COMPLETED -> colors.completed();
            case WAITING, PAUSED -> feature.isEnabled() ? colors.waiting() : colors.mutedText();
            case RUNNING -> feature.isEnabled() ? colors.active() : colors.mutedText();
            case IDLE -> feature.isEnabled() ? colors.active() : colors.mutedText();
        };
    }

    private String contextualStatus(QOLFeature feature) {
        FeatureStatus status = feature.status();
        if (status.state() == FeatureState.IDLE) {
            return null;
        }
        if (status.state() == FeatureState.RUNNING && !"auto-duper".equals(feature.id())) {
            return null;
        }
        String text = status.activity();
        if (status.state() == FeatureState.ERROR && status.detail().isPresent()) {
            text = status.detail().get();
        } else if (status.progress().isPresent()) {
            FeatureStatus.Progress progress = status.progress().get();
            long cycle = progress.max() == 0 ? progress.current() : Math.min(progress.max(), progress.current() + 1);
            text += "  •  Cycle " + cycle + " / " + progress.max();
        } else if (status.detail().isPresent() && !status.detail().get().isBlank()) {
            text += "  •  " + status.detail().get();
        }
        return text;
    }

    private String trimToWidth(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }
        String suffix = "…";
        int length = value.length();
        while (length > 0 && textRenderer.getWidth(value.substring(0, length) + suffix) > maxWidth) {
            length--;
        }
        return value.substring(0, length) + suffix;
    }

    private SettingGroup groupFor(Setting<?> setting) {
        if (setting.id().contains("delay")) {
            return SettingGroup.TIMINGS;
        }
        if (setting.id().contains("only") || setting.id().contains("through") || setting.id().equals("background")) {
            return SettingGroup.BEHAVIOUR;
        }
        return SettingGroup.GENERAL;
    }

    private String unitFor(Setting<?> setting) {
        if (setting.id().contains("delay")) {
            return "s";
        }
        if (setting.id().equals("cycles")) {
            return "cycles";
        }
        if (setting.id().contains("distance") || setting.id().equals("y-offset")) {
            return "blocks";
        }
        return "";
    }

    private String formatNumber(Object value) {
        if (value instanceof Double number) {
            String result = String.format(Locale.ROOT, "%.2f", number);
            return result.replaceAll("\\.?0+$", "");
        }
        return String.valueOf(value);
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private boolean validInt(String value, IntSetting setting) {
        if (value.isEmpty()) {
            return true;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= setting.min() && parsed <= setting.max();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean validDouble(String value, DoubleSetting setting) {
        if (value.isEmpty() || value.equals(".")) {
            return true;
        }
        if (!value.matches("\\d*(\\.\\d{0,2})?")) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(value);
            return parsed >= setting.min() && parsed <= setting.max();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void parseInt(String value, IntSetting setting, QolSliderWidget slider) {
        if (value.isEmpty()) {
            return;
        }
        try {
            setting.set(Integer.parseInt(value));
            slider.syncFromSetting();
        } catch (NumberFormatException ignored) {
        }
    }

    private void parseDouble(String value, DoubleSetting setting, QolSliderWidget slider) {
        if (value.isEmpty() || value.equals(".")) {
            return;
        }
        try {
            setting.set(Math.round(Double.parseDouble(value) * 100.0) / 100.0);
            slider.syncFromSetting();
        } catch (NumberFormatException ignored) {
        }
    }

    private enum Page {
        FEATURES(QolNavigationWidget.Icon.FEATURES, "Features", "Gameplay improvements live here."),
        HUD(QolNavigationWidget.Icon.HUD, "HUD", "HUD tools are planned for a later stage."),
        KEYBINDS(QolNavigationWidget.Icon.KEYBINDS, "Keybinds", "Use Minecraft Controls for current bindings."),
        SETTINGS(QolNavigationWidget.Icon.SETTINGS, "Settings", "Theme customisation arrives in Stage 2."),
        ABOUT(QolNavigationWidget.Icon.ABOUT, "About", "Small improvements. Better gameplay.");

        private final QolNavigationWidget.Icon icon;
        private final String label;
        private final String placeholder;

        Page(QolNavigationWidget.Icon icon, String label, String placeholder) {
            this.icon = icon;
            this.label = label;
            this.placeholder = placeholder;
        }
    }

    private enum SettingGroup {
        GENERAL("GENERAL"),
        BEHAVIOUR("BEHAVIOUR"),
        TIMINGS("TIMINGS");

        private final String label;

        SettingGroup(String label) {
            this.label = label;
        }
    }

    private record Layout(
            int mainX,
            int panelY,
            int mainWidth,
            int panelHeight,
            int drawerX,
            int drawerY,
            int drawerWidth,
            int drawerHeight
    ) {
    }

    private record FeatureWidgets(QOLFeature feature, QolToggleWidget toggle, QolButtonWidget configure) {
    }

    private static final class SettingControl {
        private final Setting<?> setting;
        private final SettingGroup group;
        private final QolToggleWidget toggle;
        private final QolSliderWidget slider;
        private final QolTextInputWidget input;

        private SettingControl(
                Setting<?> setting,
                SettingGroup group,
                QolToggleWidget toggle,
                QolSliderWidget slider,
                QolTextInputWidget input
        ) {
            this.setting = setting;
            this.group = group;
            this.toggle = toggle;
            this.slider = slider;
            this.input = input;
        }

        static SettingControl toggle(Setting<?> setting, SettingGroup group, QolToggleWidget toggle) {
            return new SettingControl(setting, group, toggle, null, null);
        }

        static SettingControl numeric(
                Setting<?> setting,
                SettingGroup group,
                QolSliderWidget slider,
                QolTextInputWidget input
        ) {
            return new SettingControl(setting, group, null, slider, input);
        }

        static SettingControl input(Setting<?> setting, SettingGroup group, QolTextInputWidget input) {
            return new SettingControl(setting, group, null, null, input);
        }

        boolean numeric() {
            return slider != null;
        }

        int rowHeight() {
            return numeric() ? 38 : 27;
        }

        List<ClickableWidget> widgets() {
            List<ClickableWidget> widgets = new ArrayList<>(2);
            if (toggle != null) {
                widgets.add(toggle);
            }
            if (slider != null) {
                widgets.add(slider);
            }
            if (input != null) {
                widgets.add(input);
            }
            return widgets;
        }
    }
}

package com.bstar.qolmod.ui;

import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.FeatureState;
import com.bstar.qolmod.feature.FeatureStatus;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.hud.HudManager;
import com.bstar.qolmod.hud.render.GlassHudSurface;
import com.bstar.qolmod.hud.render.HudSurface;
import com.bstar.qolmod.input.KeybindManager;
import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.DoubleSetting;
import com.bstar.qolmod.setting.IntSetting;
import com.bstar.qolmod.setting.Setting;
import com.bstar.qolmod.setting.StringSetting;
import com.bstar.qolmod.ui.animation.AnimatedValue;
import com.bstar.qolmod.ui.component.QolButtonWidget;
import com.bstar.qolmod.ui.component.QolColorSwatchWidget;
import com.bstar.qolmod.ui.component.QolLabel;
import com.bstar.qolmod.ui.component.QolNavigationWidget;
import com.bstar.qolmod.ui.component.QolSliderWidget;
import com.bstar.qolmod.ui.component.QolTextInputWidget;
import com.bstar.qolmod.ui.component.QolToggleWidget;
import com.bstar.qolmod.ui.component.QolValueSliderWidget;
import com.bstar.qolmod.ui.render.FramebufferGlassPanelMaterial;
import com.bstar.qolmod.ui.render.PanelMaterial;
import com.bstar.qolmod.ui.render.UiStroke;
import com.bstar.qolmod.ui.theme.AccentColor;
import com.bstar.qolmod.ui.theme.AppearanceConfig;
import com.bstar.qolmod.ui.theme.AppearancePreset;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.GlassBorderStyle;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Floating QOLmod interface with panel-local glass and in-shell contextual settings. */
public final class QOLmodScreen extends Screen implements HudSuppressingScreen {
    private static final int HEADER_HEIGHT = 45;
    private static final int FOOTER_HEIGHT = 27;
    private static final int SETTINGS_HEADER_HEIGHT = 38;
    private static final int SIDEBAR_WIDTH = 112;
    private static final int FEATURE_ROW_HEIGHT = 52;
    private static final int MAX_PANEL_WIDTH = 560;
    private static final long DRAWER_DURATION_MS = 210;
    private static final int APPEARANCE_SECTION_HEIGHT = 207;
    private static final int GLASS_SECTION_HEIGHT = 170;
    private static final List<AccentOption> ACCENT_OPTIONS = List.of(
            new AccentOption("Cobalt", 0x2F6BFF),
            new AccentOption("Violet", 0x8B5CF6),
            new AccentOption("Cyan", 0x22A9D6),
            new AccentOption("Emerald", 0x2BAE74),
            new AccentOption("Amber", 0xE29A2D),
            new AccentOption("Rose", 0xD95778)
    );
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
    private final HudManager hudManager;
    private final KeybindManager keybindManager;
    private final PanelMaterial material;
    private final HudSurface appearanceHudPreview = new GlassHudSurface();
    private final String splash;
    private final AnimatedValue drawerAnimation = new AnimatedValue(0.0, DRAWER_DURATION_MS, AnimatedValue.Easing.CUBIC_OUT);
    private final List<FeatureWidgets> featureWidgets = new ArrayList<>();
    private final List<QolNavigationWidget> navigationWidgets = new ArrayList<>();
    private final List<SettingControl> settingControls = new ArrayList<>();
    private final List<QolColorSwatchWidget> accentSwatches = new ArrayList<>();
    private final List<AppearanceSliderControl> appearanceSliderControls = new ArrayList<>();
    private final List<BorderStyleButton> borderStyleButtons = new ArrayList<>();
    private final List<PresetButton> presetButtons = new ArrayList<>();
    private final List<KeybindButton> keybindButtons = new ArrayList<>();
    private QolButtonWidget settingsBackButton;
    private QolButtonWidget closeButton;
    private QolToggleWidget statusHudToggle;
    private QolToggleWidget notificationHudToggle;
    private QolButtonWidget editHudButton;
    private QolTextInputWidget accentHexInput;
    private QolButtonWidget resetAppearanceButton;
    private Page page = Page.FEATURES;
    private QOLFeature drawerFeature;
    private boolean drawerOpen;
    private int drawerScroll;
    private int appearanceScroll;
    private boolean appearanceDirty;
    private boolean syncingAppearanceInputs;
    private KeybindManager.Binding bindingCapture;

    public QOLmodScreen(
            Screen parent,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager,
            KeybindManager keybindManager
    ) {
        this(parent, featureManager, configManager, hudManager, keybindManager, Page.FEATURES);
    }

    QOLmodScreen(
            Screen parent,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager,
            KeybindManager keybindManager,
            Page initialPage
    ) {
        this(parent, featureManager, configManager, hudManager, keybindManager,
                new FramebufferGlassPanelMaterial(), initialPage);
    }

    QOLmodScreen(
            Screen parent,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager,
            KeybindManager keybindManager,
            PanelMaterial material
    ) {
        this(parent, featureManager, configManager, hudManager, keybindManager,
                material, Page.FEATURES);
    }

    private QOLmodScreen(
            Screen parent,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager,
            KeybindManager keybindManager,
            PanelMaterial material,
            Page initialPage
    ) {
        super(Text.literal("QUALITY OF LIFE MOD"));
        this.parent = parent;
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.hudManager = Objects.requireNonNull(hudManager, "hudManager");
        this.keybindManager = Objects.requireNonNull(keybindManager, "keybindManager");
        this.material = Objects.requireNonNull(material, "material");
        page = Objects.requireNonNull(initialPage, "initialPage");
        splash = SPLASHES.get(ThreadLocalRandom.current().nextInt(SPLASHES.size()));
    }

    @Override
    protected void init() {
        featureWidgets.clear();
        navigationWidgets.clear();
        settingControls.clear();
        accentSwatches.clear();
        appearanceSliderControls.clear();
        borderStyleButtons.clear();
        presetButtons.clear();
        keybindButtons.clear();
        addNavigationWidgets();
        addFeatureWidgets();
        addHudWidgets();
        addAppearanceWidgets();
        addKeybindWidgets();
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

    private void addHudWidgets() {
        statusHudToggle = addDrawableChild(new QolToggleWidget(
                0,
                0,
                () -> hudManager.config().contextualStatus().enabled(),
                enabled -> {
                    hudManager.config().contextualStatus().setEnabled(enabled);
                    configManager.save();
                },
                Text.literal("Toggle Contextual Status HUD")
        ));
        statusHudToggle.setTooltip(Tooltip.of(Text.literal(
                "Show contextual cards while gameplay tasks are active"
        )));

        notificationHudToggle = addDrawableChild(new QolToggleWidget(
                0,
                0,
                () -> hudManager.config().notifications().enabled(),
                enabled -> {
                    hudManager.config().notifications().setEnabled(enabled);
                    configManager.save();
                },
                Text.literal("Toggle Notifications")
        ));
        notificationHudToggle.setTooltip(Tooltip.of(Text.literal(
                "Show brief notifications for meaningful gameplay events"
        )));

        editHudButton = addDrawableChild(new QolButtonWidget(
                0,
                0,
                104,
                22,
                Text.literal("Edit HUD"),
                QolButtonWidget.Style.STANDARD,
                this::openHudEditor
        ));
        editHudButton.setTooltip(Tooltip.of(Text.literal(
                "Position and preview QOLmod HUD elements over gameplay"
        )));
    }

    private void addAppearanceWidgets() {
        accentHexInput = addDrawableChild(new QolTextInputWidget(
                textRenderer, 0, 0, 78, 18, Text.literal("Accent color hex value")
        ));
        accentHexInput.setMaxLength(7);
        accentHexInput.setTextPredicate(AccentColor::isPartialHex);
        accentHexInput.setText(AccentColor.formatHex(configManager.appearance().accentRgb()));
        accentHexInput.setChangedListener(value -> AccentColor.parseHex(value).ifPresent(this::setAccent));
        accentHexInput.setTooltip(Tooltip.of(Text.literal("Exact RGB color in #RRGGBB format")));

        for (AccentOption option : ACCENT_OPTIONS) {
            QolColorSwatchWidget swatch = addDrawableChild(new QolColorSwatchWidget(
                    0,
                    0,
                    option.rgb,
                    () -> configManager.appearance().accentRgb() == option.rgb,
                    () -> applyAccentOption(option.rgb),
                    Text.literal(option.name + " accent")
            ));
            swatch.setTooltip(Tooltip.of(Text.literal(option.name + "  " + AccentColor.formatHex(option.rgb))));
            accentSwatches.add(swatch);
        }

        addAppearanceSlider(
                "GUI Tint",
                AppearanceConfig.MIN_GUI_TINT,
                AppearanceConfig.MAX_GUI_TINT,
                AppearanceConfig::guiTintStrength,
                AppearanceConfig::withGuiTintStrength
        );
        addAppearanceSlider(
                "HUD Tint",
                AppearanceConfig.MIN_HUD_TINT,
                AppearanceConfig.MAX_HUD_TINT,
                AppearanceConfig::hudTintStrength,
                AppearanceConfig::withHudTintStrength
        );
        addAppearanceSlider(
                "Blur",
                AppearanceConfig.MIN_BLUR,
                AppearanceConfig.MAX_BLUR,
                AppearanceConfig::blurStrength,
                AppearanceConfig::withBlurStrength
        );
        for (GlassBorderStyle style : GlassBorderStyle.values()) {
            QolButtonWidget button = addDrawableChild(new QolButtonWidget(
                    0,
                    0,
                    58,
                    20,
                    Text.literal(style.name()),
                    QolButtonWidget.Style.NAVIGATION,
                    () -> configManager.appearance().borderStyle() == style,
                    () -> applyBorderStyle(style)
            ));
            button.setTooltip(Tooltip.of(Text.literal(
                    style.name() + " glass border thickness"
            )));
            borderStyleButtons.add(new BorderStyleButton(style, button));
        }

        for (AppearancePreset preset : List.of(
                AppearancePreset.DEFAULT,
                AppearancePreset.DARKER,
                AppearancePreset.CLEARER,
                AppearancePreset.MINIMAL_GLASS
        )) {
            String label = preset == AppearancePreset.MINIMAL_GLASS ? "MINIMAL" : preset.displayName();
            QolButtonWidget button = addDrawableChild(new QolButtonWidget(
                    0,
                    0,
                    80,
                    20,
                    Text.literal(label),
                    QolButtonWidget.Style.NAVIGATION,
                    () -> AppearancePreset.identify(configManager.appearance()) == preset,
                    () -> applyPreset(preset)
            ));
            button.setTooltip(Tooltip.of(Text.literal(preset.displayName() + " appearance preset")));
            presetButtons.add(new PresetButton(preset, button));
        }

        resetAppearanceButton = addDrawableChild(new QolButtonWidget(
                0,
                0,
                112,
                20,
                Text.literal("Reset Appearance"),
                QolButtonWidget.Style.GHOST,
                this::resetAppearance
        ));
        resetAppearanceButton.setTooltip(Tooltip.of(Text.literal(
                "Restore the approved default theme without changing other settings"
        )));
    }

    private void addKeybindWidgets() {
        for (KeybindManager.Binding binding : keybindManager.userBindings()) {
            QolButtonWidget button = addDrawableChild(new QolButtonWidget(
                    0,
                    0,
                    108,
                    20,
                    Text.empty(),
                    QolButtonWidget.Style.STANDARD,
                    () -> beginKeybindCapture(binding)
            ));
            button.setTooltip(Tooltip.of(Text.literal(
                    "Click, then press a key • Escape cancels • Delete clears"
            )));
            keybindButtons.add(new KeybindButton(binding, button));
        }
    }

    private void beginKeybindCapture(KeybindManager.Binding binding) {
        bindingCapture = binding;
    }

    private void addAppearanceSlider(
            String label,
            double minimum,
            double maximum,
            ToDoubleFunction<AppearanceConfig> read,
            BiFunction<AppearanceConfig, Double, AppearanceConfig> update
    ) {
        QolValueSliderWidget slider = addDrawableChild(new QolValueSliderWidget(
                0,
                0,
                90,
                minimum,
                maximum,
                () -> read.applyAsDouble(configManager.appearance()),
                value -> updateAppearance(update.apply(configManager.appearance(), value)),
                value -> Math.round(value * 100.0) + "%",
                Text.literal(label + " percentage")
        ));
        QolTextInputWidget input = addDrawableChild(new QolTextInputWidget(
                textRenderer, 0, 0, 34, 18, Text.literal(label + " exact percentage")
        ));
        input.setMaxLength(3);
        input.setTextPredicate(value -> value.matches("\\d{0,3}"));
        input.setText(Integer.toString((int) Math.round(read.applyAsDouble(configManager.appearance()) * 100.0)));
        input.setChangedListener(value -> parseAppearancePercent(value, minimum, maximum, update));
        Tooltip tooltip = Tooltip.of(Text.literal(label + " material strength"));
        slider.setTooltip(tooltip);
        input.setTooltip(tooltip);
        appearanceSliderControls.add(new AppearanceSliderControl(label, read, slider, input));
    }

    private void applyAccentOption(int rgb) {
        setAccent(rgb);
        saveAppearanceIfDirty();
    }

    private void setAccent(int rgb) {
        updateAppearance(configManager.appearance().withAccentRgb(rgb));
    }

    private void applyBorderStyle(GlassBorderStyle style) {
        updateAppearance(configManager.appearance().withBorderStyle(style));
        saveAppearanceIfDirty();
    }

    private void applyPreset(AppearancePreset preset) {
        configManager.setAppearance(preset.appearance());
        appearanceDirty = true;
        normalizeAppearanceInputs();
        saveAppearanceIfDirty();
    }

    private void resetAppearance() {
        configManager.setAppearance(AppearanceConfig.defaults());
        appearanceDirty = true;
        normalizeAppearanceInputs();
        saveAppearanceIfDirty();
    }

    private void updateAppearance(AppearanceConfig updated) {
        if (updated.equals(configManager.appearance())) {
            return;
        }
        configManager.setAppearance(updated);
        appearanceDirty = true;
    }

    private void syncAppearanceInputs() {
        syncingAppearanceInputs = true;
        try {
            if (accentHexInput != null && !accentHexInput.isFocused()) {
                accentHexInput.setText(AccentColor.formatHex(configManager.appearance().accentRgb()));
            }
            for (AppearanceSliderControl control : appearanceSliderControls) {
                if (!control.input.isFocused()) {
                    String value = Integer.toString((int) Math.round(
                            control.read.applyAsDouble(configManager.appearance()) * 100.0
                    ));
                    if (!control.input.getText().equals(value)) {
                        control.input.setText(value);
                    }
                }
            }
        } finally {
            syncingAppearanceInputs = false;
        }
    }

    private void normalizeAppearanceInputs() {
        syncingAppearanceInputs = true;
        try {
            if (accentHexInput != null) {
                accentHexInput.setText(AccentColor.formatHex(configManager.appearance().accentRgb()));
            }
            for (AppearanceSliderControl control : appearanceSliderControls) {
                control.input.setText(Integer.toString((int) Math.round(
                        control.read.applyAsDouble(configManager.appearance()) * 100.0
                )));
            }
        } finally {
            syncingAppearanceInputs = false;
        }
    }

    private void saveAppearanceIfDirty() {
        if (!appearanceDirty) {
            return;
        }
        configManager.save();
        appearanceDirty = false;
    }

    private void openHudEditor() {
        configManager.save();
        if (client != null) {
            client.setScreen(new HudEditorScreen(
                    parent, featureManager, configManager, hudManager, keybindManager
            ));
        }
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
        if (page == Page.SETTINGS && selected != Page.SETTINGS) {
            normalizeAppearanceInputs();
            saveAppearanceIfDirty();
        }
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
        } else if (page == Page.HUD) {
            renderHudPage(context, layout);
        } else if (page == Page.KEYBINDS) {
            renderKeybindPage(context, layout);
        } else if (page == Page.SETTINGS) {
            renderAppearanceSettings(context, layout);
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

    private void renderHudPage(DrawContext context, Layout layout) {
        ColorPalette colors = ThemeManager.active().colors();
        int left = layout.mainX + SIDEBAR_WIDTH + 14;
        int right = layout.mainX + layout.mainWidth - 14;
        int top = layout.panelY + HEADER_HEIGHT + 12;
        context.drawText(textRenderer, "HUD", left, top, colors.mutedText(), false);

        int rowY = top + 15;
        context.fill(left, rowY, right, rowY + 43, withAlpha(colors.elevatedSurface(), 57));
        context.drawText(textRenderer, "Contextual Status HUD", left + 9, rowY + 9, colors.primaryText(), false);
        context.drawText(textRenderer, "Task status cards in the upper-right", left + 9, rowY + 27,
                colors.secondaryText(), false);
        String value = hudManager.config().contextualStatus().enabled() ? "ON" : "OFF";
        int valueColor = hudManager.config().contextualStatus().enabled() ? colors.active() : colors.mutedText();
        context.drawText(textRenderer, value, right - 46 - textRenderer.getWidth(value), rowY + 9, valueColor, false);
        UiStroke.horizontal(context, left, right, rowY + 43, colors.subtleDivider());

        rowY += 51;
        context.fill(left, rowY, right, rowY + 43, withAlpha(colors.elevatedSurface(), 57));
        context.drawText(textRenderer, "Notifications", left + 9, rowY + 9, colors.primaryText(), false);
        context.drawText(textRenderer, "Brief updates for completed or failed events", left + 9, rowY + 27,
                colors.secondaryText(), false);
        value = hudManager.config().notifications().enabled() ? "ON" : "OFF";
        valueColor = hudManager.config().notifications().enabled() ? colors.active() : colors.mutedText();
        context.drawText(textRenderer, value, right - 46 - textRenderer.getWidth(value), rowY + 9, valueColor, false);
        UiStroke.horizontal(context, left, right, rowY + 43, colors.subtleDivider());

        rowY += 55;
        int descriptionWidth = right - left - 116;
        if (descriptionWidth >= 60) {
            context.drawText(textRenderer, trimToWidth("Preview and arrange HUD elements", descriptionWidth),
                    left + 2, rowY + 6, colors.secondaryText(), false);
        }
    }

    private void renderKeybindPage(DrawContext context, Layout layout) {
        ColorPalette colors = ThemeManager.active().colors();
        int left = layout.mainX + SIDEBAR_WIDTH + 14;
        int right = layout.mainX + layout.mainWidth - 14;
        int top = layout.panelY + HEADER_HEIGHT + 12;
        context.drawText(textRenderer, "KEYBINDS", left, top, colors.mutedText(), false);

        int rowY = top + 15;
        for (KeybindButton entry : keybindButtons) {
            context.fill(left, rowY, right, rowY + 43,
                    withAlpha(colors.elevatedSurface(), 57));
            context.drawText(textRenderer, entry.binding.displayName(), left + 9, rowY + 9,
                    colors.primaryText(), false);
            String detail = entry.binding == KeybindManager.Binding.PANIC
                    ? "Immediately stops automation and releases controlled input"
                    : "Opens the QOLmod menu during gameplay";
            context.drawText(textRenderer, trimToWidth(detail, right - left - 132),
                    left + 9, rowY + 27, colors.secondaryText(), false);
            if (keybindManager.hasConflict(entry.binding)) {
                String conflict = "CONFLICT";
                context.drawText(textRenderer, conflict,
                        entry.button.getX() - 8 - textRenderer.getWidth(conflict),
                        rowY + 9, colors.waiting(), false);
            }
            UiStroke.horizontal(context, left, right, rowY + 43, colors.subtleDivider());
            rowY += 51;
        }
        context.drawText(textRenderer,
                "Click a binding, then press a key. Escape cancels; Delete clears.",
                left + 2, rowY + 5, colors.mutedText(), false);
    }

    private void renderAppearanceSettings(DrawContext context, Layout layout) {
        ColorPalette colors = ThemeManager.active().colors();
        AppearancePageLayout appearanceLayout = appearancePageLayout(layout);
        context.enableScissor(
                appearanceLayout.contentLeft,
                appearanceLayout.viewportTop,
                appearanceLayout.contentRight,
                appearanceLayout.viewportBottom
        );
        try {
            int appearanceX = appearanceLayout.appearanceX;
            int appearanceY = appearanceLayout.appearanceY;
            int appearanceWidth = appearanceLayout.appearanceWidth;
            context.drawText(textRenderer, "APPEARANCE", appearanceX, appearanceY,
                    colors.accentHover(), false);
            context.drawText(textRenderer, "Accent Color", appearanceX, appearanceY + 18,
                    colors.secondaryText(), false);
            context.drawText(textRenderer, "HEX", appearanceX, appearanceY + 59,
                    colors.mutedText(), false);
            int previewX = appearanceX + Math.min(appearanceWidth - 18, 116);
            context.drawStrokedRectangle(previewX, appearanceY + 54, 18, 18, colors.subtleDivider());
            context.fill(previewX + 2, appearanceY + 56, previewX + 16, appearanceY + 70,
                    0xFF000000 | configManager.appearance().accentRgb());

            AppearancePreset selectedPreset = AppearancePreset.identify(configManager.appearance());
            context.drawText(textRenderer, "PRESETS", appearanceX, appearanceY + 84,
                    colors.accentHover(), false);
            String presetName = selectedPreset.displayName();
            context.drawText(
                    textRenderer,
                    presetName,
                    appearanceX + appearanceWidth - textRenderer.getWidth(presetName),
                    appearanceY + 84,
                    colors.mutedText(),
                    false
            );

            appearanceHudPreview.draw(
                    context,
                    appearanceX,
                    appearanceY + 150,
                    appearanceWidth,
                    22,
                    1.0,
                    colors.accent()
            );
            context.drawText(textRenderer, "HUD GLASS PREVIEW", appearanceX + 8, appearanceY + 157,
                    colors.secondaryText(), false);

            int glassX = appearanceLayout.glassX;
            int glassY = appearanceLayout.glassY;
            context.drawText(textRenderer, "GLASS", glassX, glassY, colors.accentHover(), false);
            for (int index = 0; index < appearanceSliderControls.size(); index++) {
                AppearanceSliderControl control = appearanceSliderControls.get(index);
                int rowY = glassY + 18 + index * 35;
                context.drawText(textRenderer, control.label, glassX, rowY,
                        colors.secondaryText(), false);
                context.drawText(
                        textRenderer,
                        "%",
                        glassX + appearanceLayout.glassWidth - textRenderer.getWidth("%"),
                        rowY + 15,
                        colors.mutedText(),
                        false
                );
            }
            context.drawText(textRenderer, "Border", glassX, glassY + 123,
                    colors.secondaryText(), false);
        } finally {
            context.disableScissor();
        }
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

        boolean hudControlsVisible = page == Page.HUD && !settingsContentActive(drawerProgress);
        statusHudToggle.setX(right - 39);
        statusHudToggle.setY(layout.panelY + HEADER_HEIGHT + 32);
        statusHudToggle.visible = hudControlsVisible;
        statusHudToggle.active = hudControlsVisible;
        notificationHudToggle.setX(right - 39);
        notificationHudToggle.setY(layout.panelY + HEADER_HEIGHT + 83);
        notificationHudToggle.visible = hudControlsVisible;
        notificationHudToggle.active = hudControlsVisible;
        editHudButton.setX(right - editHudButton.getWidth());
        editHudButton.setY(layout.panelY + HEADER_HEIGHT + 133);
        editHudButton.visible = hudControlsVisible;
        editHudButton.active = hudControlsVisible;

        boolean keybindControlsVisible = page == Page.KEYBINDS
                && !settingsContentActive(drawerProgress);
        int keybindY = layout.panelY + HEADER_HEIGHT + 37;
        for (KeybindButton entry : keybindButtons) {
            entry.button.setX(right - entry.button.getWidth());
            entry.button.setY(keybindY);
            entry.button.visible = keybindControlsVisible;
            entry.button.active = keybindControlsVisible;
            if (bindingCapture == entry.binding) {
                entry.button.setMessage(Text.literal("Press a key…"));
            } else if (keybindManager.isUnbound(entry.binding)) {
                entry.button.setMessage(Text.literal("Unbound"));
            } else {
                entry.button.setMessage(keybindManager.boundKeyText(entry.binding));
            }
            keybindY += 51;
        }

        positionAppearanceControls(layout, drawerProgress);
        positionSettingControls(layout, drawerProgress);
    }

    private void positionAppearanceControls(Layout layout, double drawerProgress) {
        appearanceScroll = Math.max(0, Math.min(appearanceScroll, maxAppearanceScroll(layout)));
        AppearancePageLayout appearanceLayout = appearancePageLayout(layout);
        boolean pageVisible = page == Page.SETTINGS && !settingsContentActive(drawerProgress);
        int appearanceX = appearanceLayout.appearanceX;
        int appearanceY = appearanceLayout.appearanceY;

        for (int index = 0; index < accentSwatches.size(); index++) {
            QolColorSwatchWidget swatch = accentSwatches.get(index);
            swatch.setX(appearanceX + index * 21);
            swatch.setY(appearanceY + 32);
            setAppearanceControlVisible(swatch, pageVisible, appearanceLayout);
        }

        accentHexInput.setX(appearanceX + 28);
        accentHexInput.setY(appearanceY + 54);
        accentHexInput.setWidth(Math.min(78, Math.max(52, appearanceLayout.appearanceWidth - 74)));
        setAppearanceControlVisible(accentHexInput, pageVisible, appearanceLayout);
        if (!accentHexInput.isFocused()) {
            String hex = AccentColor.formatHex(configManager.appearance().accentRgb());
            if (!accentHexInput.getText().equals(hex)) {
                accentHexInput.setText(hex);
            }
        }

        int buttonGap = 6;
        int buttonWidth = Math.max(54, (appearanceLayout.appearanceWidth - buttonGap) / 2);
        for (int index = 0; index < presetButtons.size(); index++) {
            QolButtonWidget button = presetButtons.get(index).button;
            int column = index % 2;
            int row = index / 2;
            button.setX(appearanceX + column * (buttonWidth + buttonGap));
            button.setY(appearanceY + 100 + row * 24);
            button.setWidth(buttonWidth);
            setAppearanceControlVisible(button, pageVisible, appearanceLayout);
        }

        resetAppearanceButton.setX(appearanceX);
        resetAppearanceButton.setY(appearanceY + 179);
        resetAppearanceButton.setWidth(Math.min(appearanceLayout.appearanceWidth, 128));
        setAppearanceControlVisible(resetAppearanceButton, pageVisible, appearanceLayout);

        int glassX = appearanceLayout.glassX;
        int glassY = appearanceLayout.glassY;
        int glassWidth = appearanceLayout.glassWidth;
        for (int index = 0; index < appearanceSliderControls.size(); index++) {
            AppearanceSliderControl control = appearanceSliderControls.get(index);
            int rowY = glassY + 18 + index * 35;
            int inputWidth = 34;
            int percentWidth = textRenderer.getWidth("%") + 3;
            int inputX = glassX + glassWidth - inputWidth - percentWidth;
            control.slider.setX(glassX);
            control.slider.setY(rowY + 12);
            control.slider.setWidth(Math.max(36, inputX - glassX - 6));
            control.input.setX(inputX);
            control.input.setY(rowY + 10);
            control.input.setWidth(inputWidth);
            setAppearanceControlVisible(control.slider, pageVisible, appearanceLayout);
            setAppearanceControlVisible(control.input, pageVisible, appearanceLayout);
        }
        int borderY = glassY + 135;
        int borderGap = 5;
        int borderWidth = Math.max(44, (glassWidth - borderGap * 2) / 3);
        for (int index = 0; index < borderStyleButtons.size(); index++) {
            QolButtonWidget button = borderStyleButtons.get(index).button;
            button.setX(glassX + index * (borderWidth + borderGap));
            button.setY(borderY);
            button.setWidth(borderWidth);
            setAppearanceControlVisible(button, pageVisible, appearanceLayout);
        }
        syncAppearanceInputs();
    }

    private void setAppearanceControlVisible(
            ClickableWidget widget,
            boolean pageVisible,
            AppearancePageLayout layout
    ) {
        boolean insideViewport = widget.getY() >= layout.viewportTop
                && widget.getBottom() <= layout.viewportBottom;
        widget.visible = pageVisible && insideViewport;
        widget.active = widget.visible;
    }

    private AppearancePageLayout appearancePageLayout(Layout layout) {
        int contentLeft = layout.mainX + SIDEBAR_WIDTH + 14;
        int contentRight = layout.mainX + layout.mainWidth - 14;
        int viewportTop = layout.panelY + HEADER_HEIGHT + 10;
        int viewportBottom = layout.panelY + layout.panelHeight - FOOTER_HEIGHT - 6;
        int contentWidth = Math.max(1, contentRight - contentLeft);
        boolean twoColumns = contentWidth >= 302;
        int gap = twoColumns ? 14 : 0;
        int columnWidth = twoColumns ? (contentWidth - gap) / 2 : contentWidth;
        int top = viewportTop + 2 - appearanceScroll;
        int glassX = twoColumns ? contentLeft + columnWidth + gap : contentLeft;
        int glassY = twoColumns ? top : top + APPEARANCE_SECTION_HEIGHT;
        return new AppearancePageLayout(
                contentLeft,
                contentRight,
                viewportTop,
                viewportBottom,
                contentLeft,
                top,
                columnWidth,
                glassX,
                glassY,
                columnWidth,
                twoColumns
        );
    }

    private int maxAppearanceScroll(Layout layout) {
        AppearancePageLayout pageLayout = appearancePageLayout(layout);
        int contentHeight = pageLayout.twoColumns
                ? Math.max(APPEARANCE_SECTION_HEIGHT, GLASS_SECTION_HEIGHT)
                : APPEARANCE_SECTION_HEIGHT + GLASS_SECTION_HEIGHT;
        return Math.max(0, contentHeight - (pageLayout.viewportBottom - pageLayout.viewportTop - 2));
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
        AppearancePageLayout appearanceLayout = appearancePageLayout(layout);
        if (page == Page.SETTINGS
                && mouseX >= appearanceLayout.contentLeft && mouseX < appearanceLayout.contentRight
                && mouseY >= appearanceLayout.viewportTop && mouseY < appearanceLayout.viewportBottom) {
            int maximum = maxAppearanceScroll(layout);
            appearanceScroll = Math.max(0, Math.min(
                    maximum,
                    appearanceScroll - (int) Math.round(verticalAmount * 22.0)
            ));
            return true;
        }
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
        if (bindingCapture != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                bindingCapture = null;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE
                    || input.key() == GLFW.GLFW_KEY_DELETE) {
                keybindManager.clearBinding(bindingCapture);
                bindingCapture = null;
                return true;
            }
            InputUtil.Key key = InputUtil.fromKeyCode(input);
            if (!key.equals(InputUtil.UNKNOWN_KEY)) {
                keybindManager.rebind(bindingCapture, key);
                bindingCapture = null;
            }
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && drawerOpen) {
            closeDrawer();
            return true;
        }
        boolean handled = super.keyPressed(input);
        if (page == Page.SETTINGS
                && (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            normalizeAppearanceInputs();
            saveAppearanceIfDirty();
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean handled = super.mouseReleased(click);
        if (page == Page.SETTINGS) {
            saveAppearanceIfDirty();
        }
        return handled;
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
        bindingCapture = null;
        configManager.save();
        appearanceDirty = false;
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

    private void parseAppearancePercent(
            String value,
            double minimum,
            double maximum,
            BiFunction<AppearanceConfig, Double, AppearanceConfig> update
    ) {
        if (syncingAppearanceInputs || value.isEmpty()) {
            return;
        }
        try {
            double normalized = Integer.parseInt(value) / 100.0;
            if (normalized >= minimum && normalized <= maximum) {
                updateAppearance(update.apply(configManager.appearance(), normalized));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    enum Page {
        FEATURES(QolNavigationWidget.Icon.FEATURES, "Features", "Gameplay improvements live here."),
        HUD(QolNavigationWidget.Icon.HUD, "HUD", "Contextual gameplay status."),
        KEYBINDS(QolNavigationWidget.Icon.KEYBINDS, "Keybinds", "Manage QOLmod controls."),
        SETTINGS(QolNavigationWidget.Icon.SETTINGS, "Settings", "Appearance and glass customization."),
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

    private record AccentOption(String name, int rgb) {
    }

    private record PresetButton(AppearancePreset preset, QolButtonWidget button) {
    }

    private record BorderStyleButton(GlassBorderStyle style, QolButtonWidget button) {
    }

    private record KeybindButton(KeybindManager.Binding binding, QolButtonWidget button) {
    }

    public boolean isCapturingKeybind() {
        return bindingCapture != null;
    }

    private record AppearanceSliderControl(
            String label,
            ToDoubleFunction<AppearanceConfig> read,
            QolValueSliderWidget slider,
            QolTextInputWidget input
    ) {
    }

    private record AppearancePageLayout(
            int contentLeft,
            int contentRight,
            int viewportTop,
            int viewportBottom,
            int appearanceX,
            int appearanceY,
            int appearanceWidth,
            int glassX,
            int glassY,
            int glassWidth,
            boolean twoColumns
    ) {
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

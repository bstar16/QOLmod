package com.bstar.qolmod.config;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.hud.HudAnchor;
import com.bstar.qolmod.hud.HudManager;
import com.bstar.qolmod.hud.HudPosition;
import com.bstar.qolmod.hud.HudWidgetConfig;
import com.bstar.qolmod.hud.editor.EditableHudWidget;
import com.bstar.qolmod.setting.Setting;
import com.bstar.qolmod.ui.theme.AppearanceConfig;
import com.bstar.qolmod.ui.theme.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class ConfigManager {
    private static final int CONFIG_VERSION = 7;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final FeatureManager featureManager;
    private final HudManager hudManager;
    private final Logger logger;
    private final Path configPath;
    private AppearanceConfig appearance = AppearanceConfig.defaults();

    public ConfigManager(FeatureManager featureManager, HudManager hudManager, Logger logger) {
        this(
                featureManager,
                hudManager,
                logger,
                FabricLoader.getInstance().getConfigDir().resolve(QOLmodClient.MOD_ID + ".json")
        );
    }

    ConfigManager(FeatureManager featureManager, HudManager hudManager, Logger logger, Path configPath) {
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.hudManager = Objects.requireNonNull(hudManager, "hudManager");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.configPath = Objects.requireNonNull(configPath, "configPath");
        ThemeManager.applyAppearance(appearance);
    }

    public void load() {
        if (!Files.exists(configPath)) {
            logger.info("QOLmod config not found; using defaults.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                logger.warn("QOLmod config root is not an object; using defaults.");
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            int configVersion = readConfigVersion(root);
            if (configVersion > CONFIG_VERSION) {
                logger.warn(
                        "QOLmod config version {} is newer than supported version {}; loading known fields only.",
                        configVersion,
                        CONFIG_VERSION
                );
            }

            appearance = AppearanceConfigJson.read(getObject(root, "appearance"), AppearanceConfig.defaults());
            ThemeManager.applyAppearance(appearance);
            loadHud(getObject(root, "hud"), configVersion);
            JsonObject features = getObject(root, "features");
            if (features == null) {
                logger.warn("QOLmod config has no features object; using feature defaults.");
            } else {
                for (QOLFeature feature : featureManager.all()) {
                    loadFeature(features, feature);
                }
            }
            logger.info("QOLmod config loaded (version {}).", configVersion);
        } catch (RuntimeException | IOException exception) {
            logger.warn("Failed to load QOLmod config, using defaults where necessary.", exception);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject features = new JsonObject();
        root.addProperty("configVersion", CONFIG_VERSION);
        root.add("features", features);
        root.add("hud", saveHud());
        root.add("appearance", AppearanceConfigJson.write(appearance));

        for (QOLFeature feature : featureManager.all()) {
            JsonObject featureObject = new JsonObject();
            JsonObject settings = new JsonObject();
            featureObject.addProperty("enabled", feature.isEnabled());
            featureObject.add("settings", settings);

            for (Setting<?> setting : feature.settings()) {
                try {
                    settings.add(setting.id(), setting.toJson());
                } catch (RuntimeException exception) {
                    logger.warn("Failed to save setting '{}.{}'.", feature.id(), setting.id(), exception);
                }
            }
            features.add(feature.id(), featureObject);
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            logger.warn("Failed to save QOLmod config.", exception);
        }
    }

    public Path configPath() {
        return configPath;
    }

    public AppearanceConfig appearance() {
        return appearance;
    }

    public void setAppearance(AppearanceConfig appearance) {
        this.appearance = Objects.requireNonNull(appearance, "appearance");
        ThemeManager.applyAppearance(appearance);
    }

    private void loadFeature(JsonObject features, QOLFeature feature) {
        JsonObject featureObject = getObject(features, feature.id());
        if (featureObject == null) {
            return;
        }

        JsonElement enabled = featureObject.get("enabled");
        if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
            featureManager.restoreEnabledState(feature, enabled.getAsBoolean());
        }

        JsonObject settings = getObject(featureObject, "settings");
        if (settings == null) {
            return;
        }

        for (Setting<?> setting : feature.settings()) {
            try {
                setting.load(settings.get(setting.id()));
            } catch (RuntimeException exception) {
                setting.reset();
                logger.warn(
                        "Invalid config value for setting '{}.{}'; using its default.",
                        feature.id(),
                        setting.id(),
                        exception
                );
            }
        }
    }

    private JsonObject saveHud() {
        JsonObject hud = new JsonObject();
        for (EditableHudWidget widget : hudManager.editableWidgets()) {
            hud.add(widget.editorMetadata().configKey(), saveHudWidget(widget.config()));
        }
        return hud;
    }

    private JsonObject saveHudWidget(HudWidgetConfig config) {
        HudPosition position = config.position();
        JsonObject widget = new JsonObject();
        widget.addProperty("enabled", config.enabled());
        widget.addProperty("anchor", position.anchor().name());
        widget.addProperty("xOffset", position.xOffset());
        widget.addProperty("yOffset", position.yOffset());
        widget.addProperty("scale", position.scale());
        widget.addProperty("opacity", position.opacity());
        return widget;
    }

    private void loadHud(JsonObject hud, int configVersion) {
        if (hud == null) {
            return;
        }
        for (EditableHudWidget widget : hudManager.editableWidgets()) {
            loadHudWidget(
                    hud,
                    widget.editorMetadata().configKey(),
                    widget.config(),
                    widget.editorMetadata().displayName()
            );
        }
        migrateSharedHudLane(configVersion);
    }

    private void migrateSharedHudLane(int configVersion) {
        if (configVersion >= 4) {
            return;
        }
        HudPosition status = hudManager.config().contextualStatus().position();
        HudWidgetConfig notifications = hudManager.config().notifications();
        if (notifications.position().equals(status)) {
            HudPosition defaults = notifications.defaultPosition();
            notifications.setPosition(new HudPosition(
                    defaults.anchor(),
                    defaults.xOffset(),
                    defaults.yOffset(),
                    status.scale(),
                    status.opacity()
            ));
        }
    }

    private void loadHudWidget(JsonObject hud, String key, HudWidgetConfig config, String description) {
        JsonObject widget = getObject(hud, key);
        if (widget == null) {
            return;
        }

        JsonElement enabled = widget.get("enabled");
        if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
            config.setEnabled(enabled.getAsBoolean());
        }

        HudPosition defaults = config.position();
        try {
            HudAnchor anchor = readEnum(widget, "anchor", HudAnchor.class, defaults.anchor());
            int xOffset = readInt(widget, "xOffset", defaults.xOffset());
            int yOffset = readInt(widget, "yOffset", defaults.yOffset());
            double scale = readDouble(widget, "scale", defaults.scale());
            double opacity = readDouble(widget, "opacity", defaults.opacity());
            config.setPosition(new HudPosition(anchor, xOffset, yOffset, scale, opacity));
        } catch (RuntimeException exception) {
            logger.warn("Invalid {} position; using defaults.", description, exception);
            config.setPosition(defaults);
        }
    }

    private int readInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : fallback;
    }

    private double readDouble(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsDouble()
                : fallback;
    }

    private <E extends Enum<E>> E readEnum(JsonObject object, String key, Class<E> type, E fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return fallback;
        }
        return Enum.valueOf(type, element.getAsString());
    }

    private JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private int readConfigVersion(JsonObject root) {
        JsonElement version = root.get("configVersion");
        if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()) {
            return 0;
        }
        return Math.max(0, version.getAsInt());
    }
}

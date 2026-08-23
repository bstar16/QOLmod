package com.bstar.qolmod.config;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.hud.HudAnchor;
import com.bstar.qolmod.hud.HudManager;
import com.bstar.qolmod.hud.HudPosition;
import com.bstar.qolmod.hud.HudWidgetConfig;
import com.bstar.qolmod.setting.Setting;
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
    private static final int CONFIG_VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final FeatureManager featureManager;
    private final HudManager hudManager;
    private final Logger logger;
    private final Path configPath;

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
    }

    public void load() {
        if (!Files.exists(configPath)) {
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

            loadHud(getObject(root, "hud"));
            JsonObject features = getObject(root, "features");
            if (features == null) {
                logger.warn("QOLmod config has no features object; using feature defaults.");
            } else {
                for (QOLFeature feature : featureManager.all()) {
                    loadFeature(features, feature);
                }
            }
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
        HudWidgetConfig config = hudManager.config().contextualStatus();
        HudPosition position = config.position();
        JsonObject status = new JsonObject();
        status.addProperty("enabled", config.enabled());
        status.addProperty("anchor", position.anchor().name());
        status.addProperty("xOffset", position.xOffset());
        status.addProperty("yOffset", position.yOffset());
        status.addProperty("scale", position.scale());
        status.addProperty("opacity", position.opacity());
        hud.add("contextualStatus", status);
        return hud;
    }

    private void loadHud(JsonObject hud) {
        if (hud == null) {
            return;
        }
        JsonObject status = getObject(hud, "contextualStatus");
        if (status == null) {
            return;
        }

        HudWidgetConfig config = hudManager.config().contextualStatus();
        JsonElement enabled = status.get("enabled");
        if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
            config.setEnabled(enabled.getAsBoolean());
        }

        HudPosition defaults = config.position();
        try {
            HudAnchor anchor = readEnum(status, "anchor", HudAnchor.class, defaults.anchor());
            int xOffset = readInt(status, "xOffset", defaults.xOffset());
            int yOffset = readInt(status, "yOffset", defaults.yOffset());
            double scale = readDouble(status, "scale", defaults.scale());
            double opacity = readDouble(status, "opacity", defaults.opacity());
            config.setPosition(new HudPosition(anchor, xOffset, yOffset, scale, opacity));
        } catch (RuntimeException exception) {
            logger.warn("Invalid contextual HUD position; using defaults.", exception);
            config.setPosition(HudPosition.upperRightDefault());
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

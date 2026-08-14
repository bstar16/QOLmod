package com.bstar.qolmod.config;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.QOLFeature;
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
    private static final int CONFIG_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final FeatureManager featureManager;
    private final Logger logger;
    private final Path configPath;

    public ConfigManager(FeatureManager featureManager, Logger logger) {
        this(
                featureManager,
                logger,
                FabricLoader.getInstance().getConfigDir().resolve(QOLmodClient.MOD_ID + ".json")
        );
    }

    ConfigManager(FeatureManager featureManager, Logger logger, Path configPath) {
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
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

            JsonObject features = getObject(root, "features");
            if (features == null) {
                logger.warn("QOLmod config has no features object; using defaults.");
                return;
            }

            for (QOLFeature feature : featureManager.all()) {
                loadFeature(features, feature);
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

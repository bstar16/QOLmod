package com.bstar.qolmod.config;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.feature.Feature;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.setting.Setting;
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
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final FeatureManager featureManager;
    private final Path configPath;

    public ConfigManager(FeatureManager featureManager) {
        this.featureManager = featureManager;
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(QOLmodClient.MOD_ID + ".json");
    }

    public void load() {
        if (!Files.exists(configPath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonObject features = getObject(root, "features");
            if (features == null) {
                return;
            }

            for (Feature feature : featureManager.all()) {
                JsonObject featureObject = getObject(features, feature.id());
                if (featureObject == null) {
                    continue;
                }

                JsonElement enabled = featureObject.get("enabled");
                if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
                    featureManager.loadEnabledState(feature, enabled.getAsBoolean());
                }

                JsonObject settings = getObject(featureObject, "settings");
                if (settings == null) {
                    continue;
                }

                for (Setting<?> setting : feature.settings()) {
                    setting.load(settings.get(setting.id()));
                }
            }
        } catch (RuntimeException | IOException exception) {
            QOLmodClient.LOGGER.warn("Failed to load QOLmod config, using defaults.", exception);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject features = new JsonObject();
        root.add("features", features);

        for (Feature feature : featureManager.all()) {
            JsonObject featureObject = new JsonObject();
            JsonObject settings = new JsonObject();

            featureObject.addProperty("enabled", feature.isEnabled());
            featureObject.add("settings", settings);

            for (Setting<?> setting : feature.settings()) {
                settings.add(setting.id(), setting.toJson());
            }

            features.add(feature.id(), featureObject);
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            QOLmodClient.LOGGER.warn("Failed to save QOLmod config.", exception);
        }
    }

    private JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}

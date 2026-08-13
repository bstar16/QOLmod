package com.bstar.qolmod.feature;

import com.bstar.qolmod.QOLmodClient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FeatureManager {
    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Set<String> initializedEnabledFeatures = new HashSet<>();

    public void register(Feature feature) {
        Feature previous = features.putIfAbsent(feature.id(), feature);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate feature id: " + feature.id());
        }
    }

    public Collection<Feature> all() {
        return Collections.unmodifiableCollection(features.values());
    }

    public Optional<Feature> find(String id) {
        return Optional.ofNullable(features.get(id));
    }

    public void toggle(String id) {
        Feature feature = features.get(id);
        if (feature == null) {
            QOLmodClient.LOGGER.warn("Tried to toggle unknown feature '{}'.", id);
            return;
        }

        setEnabled(feature, !feature.isEnabled(), MinecraftClient.getInstance());
    }

    public void setEnabled(Feature feature, boolean enabled, MinecraftClient client) {
        if (feature.isEnabled() == enabled) {
            return;
        }

        feature.setEnabled(enabled);

        if (enabled) {
            feature.onEnable(client);
            initializedEnabledFeatures.add(feature.id());
        } else {
            feature.onDisable(client);
            initializedEnabledFeatures.remove(feature.id());
        }

        String state = enabled ? "enabled" : "disabled";
        String message = feature.name() + " " + state;
        QOLmodClient.LOGGER.info(message);

        if (client.player != null) {
            Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
            client.player.sendMessage(Text.literal("[QOLmod] ").formatted(Formatting.GRAY)
                    .append(Text.literal(message).formatted(color)), false);
        }
    }

    public void loadEnabledState(Feature feature, boolean enabled) {
        feature.setEnabled(enabled);
        initializedEnabledFeatures.remove(feature.id());
    }

    public void tick(MinecraftClient client) {
        for (Feature feature : features.values()) {
            if (feature.isEnabled()) {
                if (!initializedEnabledFeatures.contains(feature.id())) {
                    if (!isInWorld(client)) {
                        continue;
                    }

                    feature.onEnable(client);
                    initializedEnabledFeatures.add(feature.id());
                }

                feature.onClientTick(client);
            }
        }
    }

    private boolean isInWorld(MinecraftClient client) {
        return client != null && client.player != null && client.world != null;
    }
}

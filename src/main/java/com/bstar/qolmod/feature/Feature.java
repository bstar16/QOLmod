package com.bstar.qolmod.feature;

import com.bstar.qolmod.feature.setting.Setting;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public interface Feature {
    String id();

    String name();

    String description();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    List<Setting<?>> settings();

    default boolean hasSettings() {
        return !settings().isEmpty();
    }

    default void onEnable(MinecraftClient client) {
    }

    default void onDisable(MinecraftClient client) {
    }

    default void onClientTick(MinecraftClient client) {
    }
}

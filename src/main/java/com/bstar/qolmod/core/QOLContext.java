package com.bstar.qolmod.core;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;

/**
 * Consistent access to Minecraft client state and the validity checks shared by features.
 * Minecraft types remain visible intentionally; this is a state boundary, not a facade.
 */
public final class QOLContext {
    private final MinecraftClient client;

    public QOLContext(MinecraftClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public MinecraftClient client() {
        return client;
    }

    public ClientPlayerEntity player() {
        return client.player;
    }

    public ClientWorld world() {
        return client.world;
    }

    public ClientPlayerInteractionManager interactionManager() {
        return client.interactionManager;
    }

    public ClientPlayNetworkHandler networkHandler() {
        return client.getNetworkHandler();
    }

    public Screen currentScreen() {
        return client.currentScreen;
    }

    public boolean isInGame() {
        return player() != null && world() != null;
    }

    public boolean isPlayable() {
        return isInGame() && interactionManager() != null && networkHandler() != null;
    }
}

package com.bstar.qolmod.feature.impl;

import com.bstar.qolmod.feature.AbstractFeature;
import net.minecraft.client.MinecraftClient;

public final class TestFeature extends AbstractFeature {
    public TestFeature() {
        super("test", "Test Feature", "Placeholder feature used to verify the feature framework.");
    }

    @Override
    public void onEnable(MinecraftClient client) {
        // Placeholder hook for future behavior.
    }

    @Override
    public void onDisable(MinecraftClient client) {
        // Placeholder hook for future cleanup.
    }
}

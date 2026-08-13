package com.bstar.qolmod;

import com.bstar.qolmod.command.QOLmodClientCommands;
import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.impl.StorageLabelsFeature;
import com.bstar.qolmod.feature.impl.AutoDuperFeature;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.impl.TestFeature;
import com.bstar.qolmod.input.KeybindManager;
import com.bstar.qolmod.render.StorageLabelRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QOLmodClient implements ClientModInitializer {
    public static final String MOD_ID = "qolmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("QOLmod");

    private final FeatureManager featureManager = new FeatureManager();
    private final ConfigManager configManager = new ConfigManager(featureManager);
    private final KeybindManager keybindManager = new KeybindManager(featureManager, configManager);
    private final StorageLabelsFeature storageLabelsFeature = new StorageLabelsFeature();
    private final StorageLabelRenderer storageLabelRenderer = new StorageLabelRenderer(storageLabelsFeature);

    @Override
    public void onInitializeClient() {
        featureManager.register(new AutoDuperFeature());
        featureManager.register(storageLabelsFeature);
        featureManager.register(new TestFeature());
        configManager.load();
        storageLabelsFeature.loadLabels();
        keybindManager.register();
        QOLmodClientCommands.register(storageLabelsFeature);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() && storageLabelsFeature.hasPendingPlacement()) {
                storageLabelsFeature.placePendingLabel(
                        net.minecraft.client.MinecraftClient.getInstance(),
                        hitResult.getBlockPos(),
                        hitResult.getSide()
                );
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        WorldRenderEvents.END_MAIN.register(storageLabelRenderer::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keybindManager.tick(client);
            featureManager.tick(client);
        });

        LOGGER.info("QOLmod client initialized.");
    }
}

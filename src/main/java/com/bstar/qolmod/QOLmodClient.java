package com.bstar.qolmod;

import com.bstar.qolmod.core.QOL;
import com.bstar.qolmod.ui.render.FramebufferGlassPanelMaterial;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QOLmodClient implements ClientModInitializer {
    public static final String MOD_ID = "qolmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("QOLmod");
    private static final Identifier GLASS_RESOURCE_RELOADER = Identifier.of(MOD_ID, "liquid_glass");

    private QOL qol;

    @Override
    public void onInitializeClient() {
        registerGlassResourceReloader();
        qol = new QOL(MinecraftClient.getInstance(), LOGGER);
        qol.initialize();
        LOGGER.info("QOLmod client initialized.");
    }

    public QOL qol() {
        if (qol == null) {
            throw new IllegalStateException("QOLmod has not initialized yet");
        }
        return qol;
    }

    private void registerGlassResourceReloader() {
        ResourceLoader loader = ResourceLoader.get(ResourceType.CLIENT_RESOURCES);
        loader.registerReloader(
                GLASS_RESOURCE_RELOADER,
                (SynchronousResourceReloader) resources ->
                        FramebufferGlassPanelMaterial.reloadAdvancedPath(MinecraftClient.getInstance())
        );
        loader.addReloaderOrdering(ResourceReloaderKeys.Client.SHADERS, GLASS_RESOURCE_RELOADER);
    }
}

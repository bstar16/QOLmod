package com.bstar.qolmod;

import com.bstar.qolmod.core.QOL;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QOLmodClient implements ClientModInitializer {
    public static final String MOD_ID = "qolmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("QOLmod");

    private QOL qol;

    @Override
    public void onInitializeClient() {
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
}

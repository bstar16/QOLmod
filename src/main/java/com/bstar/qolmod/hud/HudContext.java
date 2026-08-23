package com.bstar.qolmod.hud;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public record HudContext(DrawContext drawContext, MinecraftClient client, int screenWidth, int screenHeight) {
    public HudContext {
        Objects.requireNonNull(drawContext, "drawContext");
        Objects.requireNonNull(client, "client");
    }
}

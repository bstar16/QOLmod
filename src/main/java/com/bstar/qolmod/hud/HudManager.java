package com.bstar.qolmod.hud;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.hud.status.StatusRegistry;
import com.bstar.qolmod.hud.widgets.StatusStackWidget;
import com.bstar.qolmod.ui.QOLmodScreen;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

/** Owns QOLmod HUD widgets, render order, status storage, and widget configuration. */
public final class HudManager {
    private static final Identifier HUD_ELEMENT_ID = Identifier.of(QOLmodClient.MOD_ID, "contextual_status");

    private final QOLContext context;
    private final Logger logger;
    private final HudConfig config = new HudConfig();
    private final StatusRegistry statusRegistry = new StatusRegistry();
    private final List<HudWidget> widgets = List.of(
            new StatusStackWidget(statusRegistry, config.contextualStatus())
    );
    private boolean registered;

    public HudManager(QOLContext context, Logger logger) {
        this.context = Objects.requireNonNull(context, "context");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException("HudManager is already registered");
        }
        registered = true;
        HudElementRegistry.attachElementAfter(VanillaHudElements.STATUS_EFFECTS, HUD_ELEMENT_ID,
                (drawContext, tickCounter) -> render(drawContext));
        logger.info("QOLmod contextual HUD initialized.");
    }

    public void shutdown() {
        statusRegistry.clear();
    }

    public StatusRegistry statuses() {
        return statusRegistry;
    }

    public HudConfig config() {
        return config;
    }

    private void render(net.minecraft.client.gui.DrawContext drawContext) {
        if (!context.isInGame() || context.currentScreen() instanceof QOLmodScreen) {
            return;
        }
        HudContext hudContext = new HudContext(
                drawContext,
                context.client(),
                drawContext.getScaledWindowWidth(),
                drawContext.getScaledWindowHeight()
        );
        for (HudWidget widget : widgets) {
            widget.render(hudContext);
        }
    }
}

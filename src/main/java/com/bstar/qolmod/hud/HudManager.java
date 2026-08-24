package com.bstar.qolmod.hud;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.event.events.WorldLeaveEvent;
import com.bstar.qolmod.hud.notification.NotificationManager;
import com.bstar.qolmod.hud.status.StatusRegistry;
import com.bstar.qolmod.hud.editor.EditableHudWidget;
import com.bstar.qolmod.hud.widgets.NotificationStackWidget;
import com.bstar.qolmod.hud.widgets.StatusStackWidget;
import com.bstar.qolmod.ui.HudSuppressingScreen;
import java.util.ArrayList;
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
    private final QOLEventBus eventBus;
    private final Logger logger;
    private final HudConfig config = new HudConfig();
    private final StatusRegistry statusRegistry = new StatusRegistry();
    private final NotificationManager notificationManager = new NotificationManager();
    private final List<EditableHudWidget> widgets = List.of(
            new StatusStackWidget(statusRegistry, config.contextualStatus()),
            new NotificationStackWidget(notificationManager, config.notifications())
    );
    private final List<EventSubscription> subscriptions = new ArrayList<>();
    private boolean registered;
    private boolean editorInitializationLogged;

    public HudManager(QOLContext context, QOLEventBus eventBus, Logger logger) {
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException("HudManager is already registered");
        }
        registered = true;
        HudElementRegistry.attachElementAfter(VanillaHudElements.STATUS_EFFECTS, HUD_ELEMENT_ID,
                (drawContext, tickCounter) -> render(drawContext));
        subscriptions.add(eventBus.subscribe(WorldLeaveEvent.class, event -> clearNotifications()));
        logger.info("QOLmod shared liquid-glass material initialized (regional GUI, lightweight HUD).");
        logger.info("QOLmod contextual HUD initialized.");
        logger.info("QOLmod notification system initialized.");
    }

    public void shutdown() {
        statusRegistry.clear();
        clearNotifications();
        for (EventSubscription subscription : List.copyOf(subscriptions)) {
            subscription.close();
        }
        subscriptions.clear();
    }

    public StatusRegistry statuses() {
        return statusRegistry;
    }

    public NotificationManager notifications() {
        return notificationManager;
    }

    public void clearNotifications() {
        notificationManager.clear();
    }

    public HudConfig config() {
        return config;
    }

    public List<EditableHudWidget> editableWidgets() {
        return widgets;
    }

    public void editorInitialized() {
        if (!editorInitializationLogged) {
            editorInitializationLogged = true;
            logger.info("QOLmod HUD editor initialized.");
        }
    }

    private void render(net.minecraft.client.gui.DrawContext drawContext) {
        if (!context.isInGame() || context.currentScreen() instanceof HudSuppressingScreen) {
            return;
        }
        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();
        HudContext hudContext = new HudContext(
                drawContext,
                context.client(),
                screenWidth,
                screenHeight,
                new HudLayout(screenWidth, screenHeight)
        );
        for (EditableHudWidget widget : widgets) {
            widget.render(hudContext);
        }
    }
}

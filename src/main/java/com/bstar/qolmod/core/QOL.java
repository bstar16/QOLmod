package com.bstar.qolmod.core;

import com.bstar.qolmod.automation.AutomationEngine;
import com.bstar.qolmod.command.QOLmodClientCommands;
import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.FabricEventBridge;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.event.events.ClientShutdownEvent;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.feature.ResetReason;
import com.bstar.qolmod.feature.impl.AutoDuperFeature;
import com.bstar.qolmod.feature.impl.StorageLabelsFeature;
import com.bstar.qolmod.input.KeybindManager;
import com.bstar.qolmod.hud.HudManager;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

/** Explicitly constructed owner of QOLmod's core services and runtime lifecycle. */
public final class QOL {
    private final Logger logger;
    private final QOLContext context;
    private final QOLEventBus eventBus;
    private final AutomationEngine automationEngine;
    private final FeatureManager featureManager;
    private final HudManager hudManager;
    private final ConfigManager configManager;
    private final KeybindManager keybindManager;
    private final FabricEventBridge fabricEventBridge;
    private EventSubscription shutdownSubscription;
    private boolean initialized;
    private boolean shuttingDown;

    public QOL(MinecraftClient client, Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        context = new QOLContext(Objects.requireNonNull(client, "client"));
        eventBus = new QOLEventBus(logger);
        automationEngine = new AutomationEngine(context, eventBus, logger);
        featureManager = new FeatureManager(context, eventBus, logger);
        hudManager = new HudManager(context, eventBus, logger);
        configManager = new ConfigManager(featureManager, hudManager, logger);
        keybindManager = new KeybindManager(
                context, eventBus, featureManager, configManager, hudManager, this::panic
        );
        fabricEventBridge = new FabricEventBridge(context, eventBus);
    }

    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("QOL is already initialized");
        }
        initialized = true;

        automationEngine.register();
        featureManager.register(new AutoDuperFeature(
                automationEngine,
                hudManager.statuses(),
                hudManager.notifications()
        ));
        StorageLabelsFeature storageLabels = new StorageLabelsFeature();
        featureManager.register(storageLabels);
        QOLmodClientCommands.register(this, storageLabels);
        configManager.load();
        hudManager.register();
        keybindManager.register();
        shutdownSubscription = eventBus.subscribe(ClientShutdownEvent.class, event -> shutdown());
        fabricEventBridge.register();
    }

    public void panic() {
        automationEngine.panic();
        featureManager.disableAll(ResetReason.PANIC);
        hudManager.clearNotifications();
        keybindManager.clearTransientState();
        PanicNotification.publish(hudManager.notifications());
        configManager.save();
        logger.warn("QOLmod panic reset completed.");
    }

    public void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        automationEngine.shutdown();
        configManager.save();
        keybindManager.shutdown();
        featureManager.shutdown();
        hudManager.shutdown();
        if (shutdownSubscription != null) {
            shutdownSubscription.close();
            shutdownSubscription = null;
        }
    }

    public QOLContext context() {
        return context;
    }

    public QOLEventBus eventBus() {
        return eventBus;
    }

    public FeatureManager featureManager() {
        return featureManager;
    }

    public AutomationEngine automation() {
        return automationEngine;
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public HudManager hud() {
        return hudManager;
    }

    public KeybindManager keybindManager() {
        return keybindManager;
    }
}

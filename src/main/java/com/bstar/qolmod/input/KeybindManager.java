package com.bstar.qolmod.input;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.ui.QOLmodScreen;
import com.bstar.qolmod.hud.HudManager;
import java.util.Objects;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindManager {
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
            Identifier.of(QOLmodClient.MOD_ID, "controls")
    );

    private final QOLContext context;
    private final QOLEventBus eventBus;
    private final FeatureManager featureManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private KeyBinding openConfigKey;
    private KeyBinding toggleAutoDuperKey;
    private EventSubscription tickSubscription;
    private boolean suppressAutoDuperToggleQueue;

    public KeybindManager(
            QOLContext context,
            QOLEventBus eventBus,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.hudManager = Objects.requireNonNull(hudManager, "hudManager");
    }

    public void register() {
        if (tickSubscription != null) {
            throw new IllegalStateException("KeybindManager is already registered");
        }

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.qolmod.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KEY_CATEGORY
        ));
        toggleAutoDuperKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.qolmod.toggle_auto_duper",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEY_CATEGORY
        ));
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HorseScreen) {
                ScreenKeyboardEvents.allowKeyPress(screen).register(this::allowHorseScreenKeyPress);
            }
        });
        tickSubscription = eventBus.subscribe(ClientTickEvent.class, this::onClientTick);
    }

    public void clearTransientState() {
        // KeyBinding#wasPressed drains queued presses; no gameplay key is held by this manager.
        suppressAutoDuperToggleQueue = false;
        drain(openConfigKey);
        drain(toggleAutoDuperKey);
    }

    public void shutdown() {
        if (tickSubscription != null) {
            tickSubscription.close();
            tickSubscription = null;
        }
        clearTransientState();
    }

    private void onClientTick(ClientTickEvent event) {
        while (openConfigKey.wasPressed()) {
            if (context.currentScreen() == null) {
                context.client().setScreen(new QOLmodScreen(null, featureManager, configManager, hudManager));
            }
        }
        if (suppressAutoDuperToggleQueue) {
            drain(toggleAutoDuperKey);
            suppressAutoDuperToggleQueue = false;
        } else {
            while (toggleAutoDuperKey.wasPressed()) {
                toggleAutoDuper();
            }
        }
    }

    private boolean allowHorseScreenKeyPress(Screen screen, KeyInput input) {
        if (!toggleAutoDuperKey.matchesKey(input)) {
            return true;
        }
        suppressAutoDuperToggleQueue = true;
        toggleAutoDuper();
        return false;
    }

    private void toggleAutoDuper() {
        featureManager.toggle("auto-duper");
        configManager.save();
    }

    private void drain(KeyBinding keyBinding) {
        if (keyBinding != null) {
            while (keyBinding.wasPressed()) {
                // Drain queued transitions during panic/shutdown.
            }
        }
    }
}

package com.bstar.qolmod.input;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.gui.QOLmodScreen;
import java.util.Objects;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
    private KeyBinding openConfigKey;
    private KeyBinding toggleAutoDuperKey;
    private KeyBinding toggleTestFeatureKey;
    private EventSubscription tickSubscription;

    public KeybindManager(
            QOLContext context,
            QOLEventBus eventBus,
            FeatureManager featureManager,
            ConfigManager configManager
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
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
        toggleTestFeatureKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.qolmod.toggle_test_feature",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));
        tickSubscription = eventBus.subscribe(ClientTickEvent.class, this::onClientTick);
    }

    public void clearTransientState() {
        // KeyBinding#wasPressed drains queued presses; no gameplay key is held by this manager.
        drain(openConfigKey);
        drain(toggleAutoDuperKey);
        drain(toggleTestFeatureKey);
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
                context.client().setScreen(new QOLmodScreen(null, featureManager, configManager));
            }
        }
        while (toggleAutoDuperKey.wasPressed()) {
            featureManager.toggle("auto-duper");
            configManager.save();
        }
        while (toggleTestFeatureKey.wasPressed()) {
            featureManager.toggle("test");
            configManager.save();
        }
    }

    private void drain(KeyBinding keyBinding) {
        if (keyBinding != null) {
            while (keyBinding.wasPressed()) {
                // Drain queued transitions during panic/shutdown.
            }
        }
    }
}

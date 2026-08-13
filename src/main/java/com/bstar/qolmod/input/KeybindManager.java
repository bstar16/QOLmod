package com.bstar.qolmod.input;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.config.ConfigManager;
import com.bstar.qolmod.feature.FeatureManager;
import com.bstar.qolmod.gui.QOLmodScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindManager {
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of(QOLmodClient.MOD_ID, "controls"));

    private final FeatureManager featureManager;
    private final ConfigManager configManager;
    private KeyBinding openConfigKey;
    private KeyBinding toggleAutoDuperKey;
    private KeyBinding toggleTestFeatureKey;

    public KeybindManager(FeatureManager featureManager, ConfigManager configManager) {
        this.featureManager = featureManager;
        this.configManager = configManager;
    }

    public void register() {
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
    }

    public void tick(MinecraftClient client) {
        while (openConfigKey.wasPressed()) {
            if (client != null && client.currentScreen == null) {
                client.setScreen(new QOLmodScreen(null, featureManager, configManager));
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
}

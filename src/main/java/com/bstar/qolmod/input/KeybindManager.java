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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindManager {
    public enum Binding {
        OPEN_QOLMOD("Open QOLmod", "key.qolmod.open_config", GLFW.GLFW_KEY_RIGHT_SHIFT, "Right Shift"),
        PANIC("Panic", "key.qolmod.panic", GLFW.GLFW_KEY_END, "End");

        private final String displayName;
        private final String translationKey;
        private final int defaultKeyCode;
        private final String defaultKeyName;

        Binding(String displayName, String translationKey, int defaultKeyCode, String defaultKeyName) {
            this.displayName = displayName;
            this.translationKey = translationKey;
            this.defaultKeyCode = defaultKeyCode;
            this.defaultKeyName = defaultKeyName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
            Identifier.of(QOLmodClient.MOD_ID, "controls")
    );

    private final QOLContext context;
    private final QOLEventBus eventBus;
    private final FeatureManager featureManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final Runnable panicAction;
    private final Map<Binding, KeyBinding> userBindings = new EnumMap<>(Binding.class);
    private KeyBinding openConfigKey;
    private KeyBinding panicKey;
    private KeyBinding toggleAutoDuperKey;
    private EventSubscription tickSubscription;
    private boolean suppressAutoDuperToggleQueue;
    private final KeyActivationLatch panicActivation = new KeyActivationLatch();

    public KeybindManager(
            QOLContext context,
            QOLEventBus eventBus,
            FeatureManager featureManager,
            ConfigManager configManager,
            HudManager hudManager,
            Runnable panicAction
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.hudManager = Objects.requireNonNull(hudManager, "hudManager");
        this.panicAction = Objects.requireNonNull(panicAction, "panicAction");
    }

    public void register() {
        if (tickSubscription != null) {
            throw new IllegalStateException("KeybindManager is already registered");
        }

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                Binding.OPEN_QOLMOD.translationKey,
                InputUtil.Type.KEYSYM,
                Binding.OPEN_QOLMOD.defaultKeyCode,
                KEY_CATEGORY
        ));
        panicKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                Binding.PANIC.translationKey,
                InputUtil.Type.KEYSYM,
                Binding.PANIC.defaultKeyCode,
                KEY_CATEGORY
        ));
        userBindings.put(Binding.OPEN_QOLMOD, openConfigKey);
        userBindings.put(Binding.PANIC, panicKey);
        toggleAutoDuperKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.qolmod.toggle_auto_duper",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEY_CATEGORY
        ));
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register(this::allowScreenKeyPress);
            ScreenKeyboardEvents.allowKeyRelease(screen).register(this::allowScreenKeyRelease);
        });
        tickSubscription = eventBus.subscribe(ClientTickEvent.class, this::onClientTick);
        QOLmodClient.LOGGER.info(
                "QOLmod keybind manager initialized; panic binding registered (default: {}).",
                Binding.PANIC.defaultKeyName
        );
    }

    public void clearTransientState() {
        // KeyBinding#wasPressed drains queued presses; no gameplay key is held by this manager.
        suppressAutoDuperToggleQueue = false;
        drain(openConfigKey);
        drain(panicKey);
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
        if (!panicKey.isPressed()) {
            panicActivation.release();
        }
        boolean panicQueued = false;
        while (panicKey.wasPressed()) {
            panicQueued = true;
        }
        if (panicQueued && panicActivation.press()) {
            panicAction.run();
        }
        while (openConfigKey.wasPressed()) {
            if (context.currentScreen() == null) {
                context.client().setScreen(new QOLmodScreen(
                        null, featureManager, configManager, hudManager, this
                ));
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

    private boolean allowScreenKeyPress(Screen screen, KeyInput input) {
        if (!(screen instanceof QOLmodScreen qolmodScreen && qolmodScreen.isCapturingKeybind())
                && panicKey.matchesKey(input)) {
            if (panicActivation.press()) {
                panicAction.run();
            }
            return false;
        }
        if (screen instanceof HorseScreen && toggleAutoDuperKey.matchesKey(input)) {
            suppressAutoDuperToggleQueue = true;
            toggleAutoDuper();
            return false;
        }
        return true;
    }

    private boolean allowScreenKeyRelease(Screen screen, KeyInput input) {
        if (panicKey.matchesKey(input)) {
            panicActivation.release();
        }
        return true;
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

    public List<Binding> userBindings() {
        return List.of(Binding.OPEN_QOLMOD, Binding.PANIC);
    }

    public Text boundKeyText(Binding binding) {
        // getBoundKeyLocalizedText() may call glfwGetKeyName. Translation keys are
        // sufficient for QOLmod's keyboard-only capture and never query native GLFW state.
        return Text.translatable(registeredBinding(binding).getBoundKeyTranslationKey());
    }

    public boolean isUnbound(Binding binding) {
        return registeredBinding(binding).isUnbound();
    }

    public boolean hasConflict(Binding binding) {
        KeyBinding target = registeredBinding(binding);
        if (target.isUnbound()) {
            return false;
        }
        String key = target.getBoundKeyTranslationKey();
        for (KeyBinding candidate : context.client().options.allKeys) {
            if (candidate != target && !candidate.isUnbound()
                    && candidate.getBoundKeyTranslationKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public void rebind(Binding binding, InputUtil.Key key) {
        Objects.requireNonNull(key, "key");
        KeyBinding target = registeredBinding(binding);
        for (Binding candidate : userBindings()) {
            KeyBinding other = registeredBinding(candidate);
            if (candidate != binding && !key.equals(InputUtil.UNKNOWN_KEY)
                    && other.getBoundKeyTranslationKey().equals(key.getTranslationKey())) {
                other.setBoundKey(InputUtil.UNKNOWN_KEY);
            }
        }
        target.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        clearTransientState();
        if (binding == Binding.PANIC && !key.equals(InputUtil.UNKNOWN_KEY)) {
            // The key used to finish capture is still physically down. Wait for its release.
            panicActivation.press();
        }
        context.client().options.write();
    }

    public void clearBinding(Binding binding) {
        rebind(binding, InputUtil.UNKNOWN_KEY);
    }

    static int defaultKeyCode(Binding binding) {
        return binding.defaultKeyCode;
    }

    static String defaultKeyName(Binding binding) {
        return binding.defaultKeyName;
    }

    private KeyBinding registeredBinding(Binding binding) {
        KeyBinding keyBinding = userBindings.get(Objects.requireNonNull(binding, "binding"));
        if (keyBinding == null) {
            throw new IllegalStateException("Key bindings are not registered yet");
        }
        return keyBinding;
    }
}

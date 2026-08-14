package com.bstar.qolmod.automation.input;

import com.bstar.qolmod.automation.AutomationInputOwner;
import com.bstar.qolmod.core.QOLContext;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Owns all synthetic player key holds made by the active automation run. */
public final class InputController {
    private static final Set<ControlledInput> MANUAL_OVERRIDE_INPUTS = Collections.unmodifiableSet(EnumSet.of(
            ControlledInput.FORWARD,
            ControlledInput.BACK,
            ControlledInput.LEFT,
            ControlledInput.RIGHT,
            ControlledInput.JUMP,
            ControlledInput.SNEAK
    ));

    private final QOLContext context;
    private final EnumSet<ControlledInput> heldInputs = EnumSet.noneOf(ControlledInput.class);
    private final EnumMap<ControlledInput, Boolean> priorPressedStates = new EnumMap<>(ControlledInput.class);
    private AutomationInputOwner activeOwner;

    public InputController(QOLContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void activate(AutomationInputOwner owner) {
        Objects.requireNonNull(owner, "owner");
        if (activeOwner != null && activeOwner != owner) {
            throw new IllegalStateException("Automation input is already owned by " + activeOwner);
        }
        activeOwner = owner;
    }

    public void hold(AutomationInputOwner owner, ControlledInput input) {
        requireOwner(owner);
        ControlledInput checkedInput = Objects.requireNonNull(input, "input");
        priorPressedStates.putIfAbsent(checkedInput, binding(checkedInput).isPressed());
        heldInputs.add(checkedInput);
        binding(checkedInput).setPressed(true);
    }

    public void release(AutomationInputOwner owner, ControlledInput input) {
        requireOwner(owner);
        ControlledInput checkedInput = Objects.requireNonNull(input, "input");
        if (heldInputs.remove(checkedInput)) {
            restorePhysicalState(checkedInput);
        }
    }

    /** Reasserts owned holds in case a physical key callback updated the same KeyBinding. */
    public void tick(AutomationInputOwner owner) {
        requireOwner(owner);
        for (ControlledInput input : heldInputs) {
            binding(input).setPressed(true);
        }
    }

    public void releaseAll(AutomationInputOwner owner) {
        requireOwner(owner);
        RuntimeException failure = releaseHeldInputs();
        activeOwner = null;
        if (failure != null) {
            throw failure;
        }
    }

    /** Emergency cleanup for panic/shutdown paths, even if the engine lost its run token. */
    public void releaseAllAutomationInputs() {
        RuntimeException failure = releaseHeldInputs();
        activeOwner = null;
        if (failure != null) {
            throw failure;
        }
    }

    public boolean hasHeldInputs(AutomationInputOwner owner) {
        requireOwner(owner);
        return !heldInputs.isEmpty();
    }

    public Set<ControlledInput> heldInputs(AutomationInputOwner owner) {
        requireOwner(owner);
        return Collections.unmodifiableSet(EnumSet.copyOf(heldInputs));
    }

    /** Uses raw GLFW state, not synthetic KeyBinding pressed state. */
    public boolean hasPhysicalManualMovementInput() {
        for (ControlledInput input : MANUAL_OVERRIDE_INPUTS) {
            if (isPhysicallyPressed(input)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPhysicallyPressed(ControlledInput input) {
        KeyBinding keyBinding = binding(Objects.requireNonNull(input, "input"));
        if (context.client().getWindow() == null || keyBinding.isUnbound()) {
            return false;
        }

        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
            if (key.getCategory() == InputUtil.Type.KEYSYM) {
                return InputUtil.isKeyPressed(context.client().getWindow(), key.getCode());
            }
            if (key.getCategory() == InputUtil.Type.MOUSE) {
                return GLFW.glfwGetMouseButton(context.client().getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
            }
            // GLFW cannot query an arbitrary physical key by scan code alone.
            return false;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private RuntimeException releaseHeldInputs() {
        RuntimeException failure = null;
        for (ControlledInput input : EnumSet.copyOf(heldInputs)) {
            try {
                restorePhysicalState(input);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to release one or more automation inputs");
                }
                failure.addSuppressed(exception);
            }
        }
        heldInputs.clear();
        priorPressedStates.clear();
        return failure;
    }

    private void restorePhysicalState(ControlledInput input) {
        boolean wasPressedBeforeAutomation = priorPressedStates.getOrDefault(input, false);
        binding(input).setPressed(wasPressedBeforeAutomation || isPhysicallyPressed(input));
        priorPressedStates.remove(input);
    }

    private void requireOwner(AutomationInputOwner owner) {
        Objects.requireNonNull(owner, "owner");
        if (activeOwner != owner) {
            throw new IllegalStateException("Input owner is not the active automation");
        }
    }

    private KeyBinding binding(ControlledInput input) {
        return switch (input) {
            case FORWARD -> context.client().options.forwardKey;
            case BACK -> context.client().options.backKey;
            case LEFT -> context.client().options.leftKey;
            case RIGHT -> context.client().options.rightKey;
            case JUMP -> context.client().options.jumpKey;
            case SNEAK -> context.client().options.sneakKey;
            case SPRINT -> context.client().options.sprintKey;
            case USE -> context.client().options.useKey;
            case ATTACK -> context.client().options.attackKey;
        };
    }
}

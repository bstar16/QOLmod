package com.bstar.qolmod.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class KeybindManagerTest {
    @Test
    void panicDefaultsToEnd() {
        assertEquals(
                GLFW.GLFW_KEY_END,
                KeybindManager.defaultKeyCode(KeybindManager.Binding.PANIC)
        );
        assertEquals("End", KeybindManager.defaultKeyName(KeybindManager.Binding.PANIC));
    }
}

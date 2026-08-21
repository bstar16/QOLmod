package com.bstar.qolmod.ui.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class AnimatedValueTest {
    @Test
    void interpolatesToDeterministicTargetUsingElapsedTime() {
        AtomicLong clock = new AtomicLong();
        AnimatedValue value = new AnimatedValue(0.0, 200, AnimatedValue.Easing.LINEAR, clock::get);

        value.setTarget(1.0);
        clock.set(100_000_000L);

        assertEquals(0.5, value.value(), 0.0001);
        assertFalse(value.isAtTarget());

        clock.set(200_000_000L);
        assertEquals(1.0, value.value(), 0.0001);
        assertTrue(value.isAtTarget());
    }

    @Test
    void changingTargetContinuesFromCurrentValue() {
        AtomicLong clock = new AtomicLong();
        AnimatedValue value = new AnimatedValue(0.0, 200, AnimatedValue.Easing.LINEAR, clock::get);

        value.setTarget(1.0);
        clock.set(100_000_000L);
        value.setTarget(0.0);
        clock.set(200_000_000L);

        assertEquals(0.25, value.value(), 0.0001);
    }
}

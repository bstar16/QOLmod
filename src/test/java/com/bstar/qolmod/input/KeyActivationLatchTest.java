package com.bstar.qolmod.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyActivationLatchTest {
    @Test
    void oneActivationIsAllowedUntilRelease() {
        KeyActivationLatch latch = new KeyActivationLatch();

        assertTrue(latch.press());
        assertFalse(latch.press());
        assertFalse(latch.press());

        latch.release();

        assertTrue(latch.press());
    }
}

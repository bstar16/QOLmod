package com.bstar.qolmod.input;

/** Suppresses repeated key events until the matching physical key release. */
final class KeyActivationLatch {
    private boolean pressed;

    boolean press() {
        if (pressed) {
            return false;
        }
        pressed = true;
        return true;
    }

    void release() {
        pressed = false;
    }
}

package com.bstar.qolmod.automation;

import com.bstar.qolmod.automation.input.InputController;
import com.bstar.qolmod.core.QOLContext;

/** The deliberately small set of services and timing information available to tasks. */
public interface TaskContext {
    QOLContext qol();

    AutomationEngine automation();

    InputController inputs();

    AutomationInputOwner inputOwner();

    long elapsedTicks();

    default double elapsedSeconds() {
        return elapsedTicks() / 20.0;
    }
}

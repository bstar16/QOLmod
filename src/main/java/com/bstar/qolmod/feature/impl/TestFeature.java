package com.bstar.qolmod.feature.impl;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.event.events.WorldJoinEvent;
import com.bstar.qolmod.event.events.WorldLeaveEvent;
import com.bstar.qolmod.feature.FeatureState;
import com.bstar.qolmod.feature.FeatureStatus;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.feature.ResetReason;
import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.IntSetting;

/** Temporary validation feature for the Phase 1 core. */
public final class TestFeature extends QOLFeature {
    private final BooleanSetting logLifecycle = registerSetting(new BooleanSetting(
            "log-lifecycle",
            "Log Lifecycle",
            "Writes core lifecycle validation messages to the log.",
            true
    ));
    private final IntSetting progressWindow = registerSetting(new IntSetting(
            "progress-window",
            "Progress Window",
            "Ticks used to exercise the generic status progress model.",
            200,
            20,
            1200
    ));
    private int ticks;

    public TestFeature() {
        super("test", "Core Test Feature", "Temporary validation for QOLmod core events and lifecycle.");
    }

    @Override
    protected void onRegister() {
        listenForLifetime(WorldJoinEvent.class, event -> onWorldJoin());
        listenForLifetime(WorldLeaveEvent.class, event -> onWorldLeave());
        log("registered");
    }

    @Override
    protected void onEnable() {
        ticks = 0;
        listen(ClientTickEvent.class, this::onClientTick);
        updateProgress("Enabled");
        log("enabled");
    }

    @Override
    protected void onDisable() {
        log("disabled");
    }

    @Override
    protected void onReset(ResetReason reason) {
        ticks = 0;
        updateStatus(FeatureStatus.detailed(FeatureState.WAITING, "Reset", reason.name()));
        log("reset: " + reason);
    }

    private void onClientTick(ClientTickEvent event) {
        ticks = (ticks + 1) % (progressWindow.get() + 1);
        updateProgress("Receiving ClientTickEvent");
    }

    private void onWorldJoin() {
        if (isEnabled()) {
            updateProgress("World joined");
        }
        log("world joined");
    }

    private void onWorldLeave() {
        if (isEnabled()) {
            updateStatus(FeatureStatus.of(FeatureState.WAITING, "World left"));
        }
        log("world left");
    }

    private void updateProgress(String activity) {
        updateStatus(FeatureStatus.progressing(
                FeatureState.RUNNING,
                activity,
                "Core validation",
                ticks,
                progressWindow.get()
        ));
    }

    private void log(String message) {
        if (logLifecycle.get()) {
            QOLmodClient.LOGGER.info("Core Test Feature {}.", message);
        }
    }
}

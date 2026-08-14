package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.DoubleSetting;
import com.bstar.qolmod.setting.IntSetting;

public final class
AutoDuperConfig {
    private final IntSetting cycles;
    private final BooleanSetting shulkersOnly;
    private final BooleanSetting mountWithoutChest;
    private final DoubleSetting mountDelay;
    private final DoubleSetting keyPressDelay;
    private final DoubleSetting inventoryDelay;
    private final DoubleSetting moveItemsDelay;
    private final DoubleSetting chestApplyDelay;
    private final DoubleSetting dismountDelay;

    public AutoDuperConfig(
            IntSetting cycles,
            BooleanSetting shulkersOnly,
            BooleanSetting mountWithoutChest,
            DoubleSetting mountDelay,
            DoubleSetting keyPressDelay,
            DoubleSetting inventoryDelay,
            DoubleSetting moveItemsDelay,
            DoubleSetting chestApplyDelay,
            DoubleSetting dismountDelay
    ) {
        this.cycles = cycles;
        this.shulkersOnly = shulkersOnly;
        this.mountWithoutChest = mountWithoutChest;
        this.mountDelay = mountDelay;
        this.keyPressDelay = keyPressDelay;
        this.inventoryDelay = inventoryDelay;
        this.moveItemsDelay = moveItemsDelay;
        this.chestApplyDelay = chestApplyDelay;
        this.dismountDelay = dismountDelay;
    }

    public int cycles() {
        return cycles.get();
    }

    public boolean shulkersOnly() {
        return shulkersOnly.get();
    }

    public boolean mountWithoutChest() {
        return mountWithoutChest.get();
    }

    public int mountDelayTicks() {
        return secondsToTicks(mountDelay.get());
    }

    public int keyPressDelayTicks() {
        return secondsToTicks(keyPressDelay.get());
    }

    public int inventoryDelayTicks() {
        return secondsToTicks(inventoryDelay.get());
    }

    public int moveItemsDelayTicks() {
        return secondsToTicks(moveItemsDelay.get());
    }

    public int chestApplyDelayTicks() {
        return secondsToTicks(chestApplyDelay.get());
    }

    public int dismountDelayTicks() {
        return secondsToTicks(dismountDelay.get());
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) (seconds * 20.0));
    }
}

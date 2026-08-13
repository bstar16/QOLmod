package com.bstar.qolmod.feature.impl;

import com.bstar.qolmod.feature.AbstractFeature;
import com.bstar.qolmod.feature.dupe.AutoDuperConfig;
import com.bstar.qolmod.feature.dupe.DupeSequencer;
import com.bstar.qolmod.feature.setting.BooleanSetting;
import com.bstar.qolmod.feature.setting.DoubleSetting;
import com.bstar.qolmod.feature.setting.IntSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HorseScreen;

public final class AutoDuperFeature extends AbstractFeature {
    private final IntSetting cycles = registerSetting(new IntSetting(
            "cycles",
            "Cycles",
            "Number of dupe cycles to do. 0 means infinite.",
            1,
            0,
            1000000
    ));
    private final BooleanSetting shulkersOnly = registerSetting(new BooleanSetting(
            "shulkers-only",
            "Shulkers Only",
            "Only moves shulker boxes back from the donkey inventory.",
            false
    ));
    private final BooleanSetting mountWithoutChest = registerSetting(new BooleanSetting(
            "mount-without-chest",
            "Mount Without Chest",
            "Mounts the donkey without having a chest selected.",
            false
    ));
    private final DoubleSetting mountDelay = registerSetting(new DoubleSetting(
            "mount-delay",
            "Mount Delay",
            "Delay for mounting in seconds.",
            1.0,
            0.0,
            2.0
    ));
    private final DoubleSetting keyPressDelay = registerSetting(new DoubleSetting(
            "keypress-delay",
            "Keypress Delay",
            "Delay for key presses in seconds.",
            0.1,
            0.0,
            1.0
    ));
    private final DoubleSetting inventoryDelay = registerSetting(new DoubleSetting(
            "inventory-delay",
            "Inventory Delay",
            "Delay for inventory actions in seconds.",
            0.25,
            0.0,
            1.0
    ));
    private final DoubleSetting moveItemsDelay = registerSetting(new DoubleSetting(
            "move-items-delay",
            "Move Items Delay",
            "Delay for moving items in seconds.",
            0.1,
            0.0,
            1.0
    ));
    private final DoubleSetting chestApplyDelay = registerSetting(new DoubleSetting(
            "chest-apply-delay",
            "Chest Apply Delay",
            "Delay for applying the chest in seconds.",
            0.1,
            0.0,
            1.0
    ));
    private final DoubleSetting dismountDelay = registerSetting(new DoubleSetting(
            "dismount-delay",
            "Dismount Delay",
            "Delay for dismounting in seconds.",
            0.1,
            0.0,
            1.0
    ));
    private final AutoDuperConfig config = new AutoDuperConfig(
            cycles,
            shulkersOnly,
            mountWithoutChest,
            mountDelay,
            keyPressDelay,
            inventoryDelay,
            moveItemsDelay,
            chestApplyDelay,
            dismountDelay
    );
    private final DupeSequencer sequencer = new DupeSequencer(config);
    private boolean wasInInventory;
    private int cyclesCompleted;
    private boolean cycleInProgress;

    public AutoDuperFeature() {
        super("auto-duper", "Auto Duper", "Automatically dupes items using the donkey method.");
    }

    @Override
    public void onEnable(MinecraftClient client) {
        cyclesCompleted = 0;
        cycleInProgress = false;
        wasInInventory = false;
        sequencer.start(client);
    }

    @Override
    public void onDisable(MinecraftClient client) {
        cleanup(client);
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (!hasRequiredClientState(client)) {
            stopFromTick(client, "Missing client state - stopping dupe sequence");
            return;
        }

        int currentStage = sequencer.getCurrentStage();

        if (client.currentScreen instanceof HorseScreen) {
            wasInInventory = true;
        } else if (wasInInventory && client.currentScreen == null) {
            wasInInventory = false;
            if (currentStage >= 4 && currentStage <= 7) {
                stopFromTick(client, "Inventory closed - stopping dupe sequence");
                return;
            }
        }

        if (currentStage == 1 && !cycleInProgress) {
            cycleInProgress = true;
        } else if (currentStage == 0 && cycleInProgress) {
            cycleInProgress = false;
            cyclesCompleted++;
            sequencer.sendMessage(client, "Completed cycle " + cyclesCompleted);

            if (config.cycles() != 0 && cyclesCompleted >= config.cycles()) {
                stopFromTick(client, "Completed all " + config.cycles() + " cycles - stopping");
                return;
            }
        }

        sequencer.tick(client);
    }

    private boolean hasRequiredClientState(MinecraftClient client) {
        return client != null
                && client.player != null
                && client.world != null
                && client.interactionManager != null
                && client.getNetworkHandler() != null;
    }

    private void stopFromTick(MinecraftClient client, String reason) {
        sequencer.sendMessage(client, reason);
        cleanup(client);
        setEnabled(false);
    }

    private void cleanup(MinecraftClient client) {
        sequencer.reset(client);
        wasInInventory = false;
        cyclesCompleted = 0;
        cycleInProgress = false;
    }
}

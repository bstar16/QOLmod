package com.bstar.qolmod.feature.impl;

import com.bstar.qolmod.automation.AutomationEngine;
import com.bstar.qolmod.automation.AutomationStartResult;
import com.bstar.qolmod.automation.AutomationState;
import com.bstar.qolmod.automation.AutomationStatus;
import com.bstar.qolmod.automation.AutomationStopReason;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.feature.FeatureState;
import com.bstar.qolmod.feature.FeatureStatus;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.feature.ResetReason;
import com.bstar.qolmod.feature.dupe.AutoDuperConfig;
import com.bstar.qolmod.feature.dupe.AutoDuperWorkflow;
import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.DoubleSetting;
import com.bstar.qolmod.setting.IntSetting;
import java.util.Objects;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AutoDuperFeature extends QOLFeature {
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
            mountDelay,
            keyPressDelay,
            inventoryDelay,
            moveItemsDelay,
            chestApplyDelay,
            dismountDelay
    );
    private final AutomationEngine automation;
    private AutoDuperWorkflow workflow;
    private String startFailure;

    public AutoDuperFeature(AutomationEngine automation) {
        super("auto-duper", "Auto Duper", "Automatically dupes items using the donkey method.");
        this.automation = Objects.requireNonNull(automation, "automation");
    }

    @Override
    protected void onEnable() {
        workflow = new AutoDuperWorkflow(config);
        startFailure = null;
        AutomationStartResult result = automation.start(workflow);
        listen(ClientTickEvent.class, event -> syncWorkflowStatus());
        if (result.started()) {
            updateStatus(workflowStatus(FeatureState.RUNNING, "Starting AutoDuper"));
        } else {
            startFailure = result.message();
            updateStatus(FeatureStatus.detailed(FeatureState.ERROR, "AutoDuper could not start", startFailure));
        }
    }

    @Override
    protected void onReset(ResetReason reason) {
        if ((reason == ResetReason.USER_DISABLED || reason == ResetReason.ERROR)
                && isOwnWorkflowRunning()) {
            automation.cancel();
        }
        workflow = null;
        startFailure = null;
    }

    private void syncWorkflowStatus() {
        if (startFailure != null) {
            featureManager().disable(this, ResetReason.ERROR);
            return;
        }
        if (workflow == null) {
            return;
        }

        AutomationStatus automationStatus = automation.status();
        if (automationStatus.workflowId().filter(AutoDuperWorkflow.ID::equals).isEmpty()) {
            return;
        }

        if (automationStatus.state() == AutomationState.RUNNING
                || automationStatus.state() == AutomationState.WAITING) {
            FeatureState state = automationStatus.state() == AutomationState.WAITING
                    ? FeatureState.WAITING
                    : FeatureState.RUNNING;
            updateStatus(workflowStatus(state, automationStatus.activity()));
            return;
        }

        AutomationStopReason stopReason = automationStatus.stopReason().orElse(AutomationStopReason.ERROR);
        String detail = automationStatus.detail().orElse(stopReason.name());
        if (stopReason == AutomationStopReason.COMPLETED) {
            sendMessage("Completed all " + workflow.completedCycles() + " cycles - stopping");
            updateStatus(FeatureStatus.detailed(FeatureState.COMPLETED, "AutoDuper complete", detail));
            featureManager().disable(this, ResetReason.USER_DISABLED);
            return;
        }
        if (automationStatus.state() == AutomationState.ERROR) {
            sendMessage(detail);
            updateStatus(FeatureStatus.detailed(FeatureState.ERROR, "AutoDuper stopped", detail));
            featureManager().disable(this, ResetReason.ERROR);
            return;
        }

        sendMessage("Automation cancelled: " + detail);
        featureManager().disable(this, ResetReason.USER_DISABLED);
    }

    private boolean isOwnWorkflowRunning() {
        return automation.isRunning()
                && automation.status().workflowId().filter(AutoDuperWorkflow.ID::equals).isPresent();
    }

    private FeatureStatus workflowStatus(FeatureState state, String activity) {
        int completed = workflow == null ? 0 : workflow.completedCycles();
        int target = workflow == null ? config.cycles() : workflow.targetCycles();
        if (target == 0) {
            return FeatureStatus.detailed(state, activity, "Completed " + completed + " cycles");
        }
        return FeatureStatus.progressing(
                state,
                activity,
                "Completed " + completed + " / " + target + " cycles",
                Math.min(completed, target),
                target
        );
    }

    private void sendMessage(String message) {
        if (context().player() != null) {
            context().player().sendMessage(Text.literal("[AutoDuper] ").formatted(Formatting.AQUA)
                    .append(Text.literal(message).formatted(Formatting.WHITE)), false);
        }
    }
}

package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.automation.TaskContext;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/** Mutable state and exploit-specific Minecraft assumptions shared by one AutoDuper run. */
final class AutoDuperSession {
    static final int DONKEY_SLOT_FIRST = 2;
    static final int DONKEY_SLOT_LAST = 16;
    static final int PLAYER_SLOT_FIRST = 17;
    static final int PLAYER_SLOT_LAST = 52;

    private static final double DONKEY_SEARCH_RADIUS = 5.0;

    private final AutoDuperConfig config;

    AutoDuperSession(AutoDuperConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    AutoDuperConfig config() {
        return config;
    }

    void begin(TaskContext context) {
        sendMessage(context, "Dupe Started");
    }

    Optional<String> invalidEnvironment(TaskContext context) {
        if (!context.qol().isPlayable()) {
            return Optional.of("Missing playable client state");
        }
        if (context.qol().player().isDead()) {
            return Optional.of("Player died during AutoDuper workflow");
        }
        return Optional.empty();
    }

    DonkeyEntity mountedDonkey(TaskContext context) {
        return context.qol().player().getVehicle() instanceof DonkeyEntity donkey ? donkey : null;
    }

    DonkeyEntity findNearestAvailableDonkey(TaskContext context) {
        DonkeyEntity closest = null;
        double closestDistance = DONKEY_SEARCH_RADIUS * DONKEY_SEARCH_RADIUS;
        for (Entity entity : context.qol().world().getEntities()) {
            if (!(entity instanceof DonkeyEntity donkey) || donkey.hasPassengers()) {
                continue;
            }

            double distance = context.qol().player().squaredDistanceTo(donkey);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = donkey;
            }
        }

        if (closest != null) {
            faceEntity(context, closest);
        }
        return closest;
    }

    void interactWithDonkey(TaskContext context, DonkeyEntity donkey) {
        context.qol().interactionManager().interactEntity(context.qol().player(), donkey, Hand.MAIN_HAND);
    }

    HorseScreen currentHorseScreen(TaskContext context) {
        return context.qol().currentScreen() instanceof HorseScreen screen ? screen : null;
    }

    HorseScreenHandler currentHorseHandler(TaskContext context) {
        HorseScreen screen = currentHorseScreen(context);
        return screen == null ? null : screen.getScreenHandler();
    }

    int findChestInHotbar(TaskContext context) {
        for (int slot = 0; slot < 9; slot++) {
            if (context.qol().player().getInventory().getStack(slot).getItem() == Items.CHEST) {
                return slot;
            }
        }
        return -1;
    }

    void selectHotbarSlot(TaskContext context, int slot) {
        context.qol().player().getInventory().setSelectedSlot(slot);
    }

    void sendMessage(TaskContext context, String message) {
        if (context.qol().player() != null) {
            context.qol().player().sendMessage(Text.literal("[AutoDuper] ").formatted(Formatting.AQUA)
                    .append(Text.literal(message).formatted(Formatting.WHITE)), false);
        }
    }

    void closeMountInventoryIfSafe(TaskContext context) {
        MinecraftClient client = context.qol().client();
        if (context.qol().isPlayable() && client.currentScreen instanceof HorseScreen) {
            client.player.closeHandledScreen();
        }
    }

    static boolean isValidSlot(HorseScreenHandler handler, int slot) {
        return slot >= 0 && slot < handler.slots.size();
    }

    static boolean isShulker(ItemStack stack) {
        return stack.getItem().toString().contains("shulker_box");
    }

    private void faceEntity(TaskContext context, Entity entity) {
        double deltaX = entity.getX() - context.qol().player().getX();
        double deltaY = entity.getY() + entity.getHeight() / 2.0
                - (context.qol().player().getY()
                + context.qol().player().getEyeHeight(context.qol().player().getPose()));
        double deltaZ = entity.getZ() - context.qol().player().getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        context.qol().player().setYaw((float) Math.toDegrees(Math.atan2(-deltaX, deltaZ)));
        context.qol().player().setPitch((float) Math.toDegrees(-Math.atan2(deltaY, horizontalDistance)));
    }
}

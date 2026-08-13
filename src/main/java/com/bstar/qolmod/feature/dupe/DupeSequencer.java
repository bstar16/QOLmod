package com.bstar.qolmod.feature.dupe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public final class DupeSequencer {
    private final AutoDuperConfig config;
    private boolean running;
    private int currentStage;
    private int tickDelay;
    private int lastNonChestSlot;
    private ItemMovePlan movePlan;

    public DupeSequencer(AutoDuperConfig config) {
        this.config = config;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void start(MinecraftClient client) {
        running = true;
        currentStage = 0;
        tickDelay = 0;
        movePlan = null;

        if (hasPlayer(client) && config.mountWithoutChest()) {
            lastNonChestSlot = client.player.getInventory().getSelectedSlot();
            client.player.getInventory().setSelectedSlot(findSafeSlot(client));
        }

        sendMessage(client, "Dupe Started (Stage: " + currentStage + ")");
    }

    public void reset(MinecraftClient client) {
        running = false;
        currentStage = 0;
        tickDelay = 0;
        movePlan = null;

        if (client != null && client.options != null && client.options.sneakKey != null) {
            client.options.sneakKey.setPressed(false);
        }
    }

    public void tick(MinecraftClient client) {
        if (!running || !hasRequiredState(client)) {
            reset(client);
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        switch (currentStage) {
            case 0 -> selectChestIfNeeded(client);
            case 1 -> mountDonkey(client);
            case 2 -> checkMounted(client);
            case 3 -> openInventory(client);
            case 4 -> verifyInventoryOpened(client);
            case 5 -> moveItemsToDonkey(client);
            case 6 -> applyChest(client);
            case 7 -> moveItemsFromDonkey(client);
            case 8 -> closeInventory(client);
            case 9 -> dismount(client);
            case 10 -> releaseDismountKey(client);
            default -> reset(client);
        }
    }

    public void sendMessage(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[AutoDuper] ").formatted(Formatting.AQUA)
                    .append(Text.literal(message).formatted(Formatting.WHITE)), false);
        }
    }

    private void selectChestIfNeeded(MinecraftClient client) {
        if (!config.mountWithoutChest()) {
            int chestSlot = findChestInHotbar(client);
            if (chestSlot != -1) {
                sendMessage(client, "Selecting chest in hotbar");
                client.player.getInventory().setSelectedSlot(chestSlot);
                tickDelay = config.keyPressDelayTicks();
                currentStage = 1;
            } else {
                stop(client, "No chest found in hotbar! Stopping.");
            }
            return;
        }

        client.player.getInventory().setSelectedSlot(findSafeSlot(client));
        currentStage = 1;
    }

    private void mountDonkey(MinecraftClient client) {
        if (!(client.player.getVehicle() instanceof DonkeyEntity)) {
            DonkeyEntity nearestDonkey = findNearestDonkey(client);
            if (nearestDonkey == null) {
                stop(client, "No donkey found nearby! Stopping.");
                return;
            }

            if (config.mountWithoutChest() && !hasChestInInventory(client)) {
                stop(client, "No chest found in inventory! Stopping.");
                return;
            }

            sendMessage(client, "Mounting donkey");
            client.interactionManager.interactEntity(client.player, nearestDonkey, Hand.MAIN_HAND);
            tickDelay = config.keyPressDelayTicks();
            currentStage = 2;
        } else {
            currentStage = 3;
        }
    }

    private void checkMounted(MinecraftClient client) {
        if (client.player.getVehicle() instanceof DonkeyEntity) {
            tickDelay = config.mountDelayTicks();
            currentStage = 3;
        } else {
            currentStage = 1;
        }
    }

    private void openInventory(MinecraftClient client) {
        if (client.player.getVehicle() instanceof DonkeyEntity) {
            sendMessage(client, "Opening inventory");
            client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                    client.player,
                    ClientCommandC2SPacket.Mode.OPEN_INVENTORY
            ));
            tickDelay = config.inventoryDelayTicks();
            currentStage = 4;
        } else {
            currentStage = 1;
        }
    }

    private void verifyInventoryOpened(MinecraftClient client) {
        if (client.currentScreen instanceof HorseScreen) {
            tickDelay = config.keyPressDelayTicks();
            currentStage = 5;
        } else {
            currentStage = 3;
        }
    }

    private void moveItemsToDonkey(MinecraftClient client) {
        if (!(client.currentScreen instanceof HorseScreen screen)) {
            return;
        }

        HorseScreenHandler handler = screen.getScreenHandler();
        if (movePlan == null) {
            sendMessage(client, "Moving items to donkey");
            movePlan = ItemMovePlan.toDonkey(handler);
        }

        if (movePlan.tick(client, handler)) {
            movePlan = null;
            tickDelay = config.moveItemsDelayTicks();
            currentStage = 6;
        }
    }

    private void applyChest(MinecraftClient client) {
        if (!(client.currentScreen instanceof HorseScreen)) {
            return;
        }

        sendMessage(client, "Applying chest");
        if (config.mountWithoutChest()) {
            lastNonChestSlot = client.player.getInventory().getSelectedSlot();
        }

        int chestSlot = findChestInHotbar(client);
        if (chestSlot == -1) {
            stop(client, "No chest found for applying! Stopping.");
            return;
        }

        client.player.getInventory().setSelectedSlot(chestSlot);
        if (client.player.getVehicle() instanceof DonkeyEntity donkey) {
            client.interactionManager.interactEntity(client.player, donkey, Hand.MAIN_HAND);
        }

        if (config.mountWithoutChest()) {
            client.player.getInventory().setSelectedSlot(findSafeSlot(client));
        }

        tickDelay = config.chestApplyDelayTicks();
        currentStage = 7;
    }

    private void moveItemsFromDonkey(MinecraftClient client) {
        if (!(client.currentScreen instanceof HorseScreen screen)) {
            return;
        }

        sendMessage(client, "Taking items from donkey");
        HorseScreenHandler handler = screen.getScreenHandler();
        for (int donkeySlot = 2; donkeySlot < 17; donkeySlot++) {
            if (!isValidSlot(handler, donkeySlot)) {
                continue;
            }

            ItemStack stack = handler.getSlot(donkeySlot).getStack();
            if (stack.isEmpty()) {
                continue;
            }

            if (config.shulkersOnly() && !isShulker(stack)) {
                continue;
            }

            client.interactionManager.clickSlot(handler.syncId, donkeySlot, 0, SlotActionType.QUICK_MOVE, client.player);
        }

        tickDelay = config.moveItemsDelayTicks();
        currentStage = 8;
    }

    private void closeInventory(MinecraftClient client) {
        if (client.currentScreen != null) {
            sendMessage(client, "Closing inventory");
            client.player.closeHandledScreen();
            tickDelay = config.keyPressDelayTicks();
            currentStage = 9;
        }
    }

    private void dismount(MinecraftClient client) {
        if (client.player.getVehicle() instanceof DonkeyEntity) {
            sendMessage(client, "Dismounting donkey");
            client.options.sneakKey.setPressed(true);
            tickDelay = config.dismountDelayTicks();
            currentStage = 10;
        } else {
            currentStage = 0;
        }
    }

    private void releaseDismountKey(MinecraftClient client) {
        client.options.sneakKey.setPressed(false);
        tickDelay = config.dismountDelayTicks();

        if (client.player.getVehicle() == null) {
            if (config.mountWithoutChest()) {
                client.player.getInventory().setSelectedSlot(findSafeSlot(client));
            }
            currentStage = 0;
        } else {
            currentStage = 9;
        }
    }

    private void stop(MinecraftClient client, String message) {
        sendMessage(client, message);
        reset(client);
    }

    private int findSafeSlot(MinecraftClient client) {
        if (!hasPlayer(client)) {
            return 0;
        }

        if (lastNonChestSlot >= 0 && lastNonChestSlot < 9
                && client.player.getInventory().getStack(lastNonChestSlot).getItem() != Items.CHEST) {
            return lastNonChestSlot;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).getItem() != Items.CHEST) {
                return slot;
            }
        }

        return 0;
    }

    private int findChestInHotbar(MinecraftClient client) {
        if (!hasPlayer(client)) {
            return -1;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).getItem() == Items.CHEST) {
                return slot;
            }
        }

        return -1;
    }

    private boolean hasChestInInventory(MinecraftClient client) {
        if (!hasPlayer(client)) {
            return false;
        }

        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getStack(slot).getItem() == Items.CHEST) {
                return true;
            }
        }

        return false;
    }

    private DonkeyEntity findNearestDonkey(MinecraftClient client) {
        double searchRadius = 5.0;
        DonkeyEntity closest = null;
        double closestDistance = searchRadius * searchRadius;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof DonkeyEntity donkey) {
                double distance = client.player.squaredDistanceTo(entity);
                if (distance < closestDistance && !donkey.hasPassengers()) {
                    closestDistance = distance;
                    closest = donkey;
                }
            }
        }

        if (closest != null) {
            faceEntity(client, closest);
        }

        return closest;
    }

    private void faceEntity(MinecraftClient client, Entity entity) {
        double deltaX = entity.getX() - client.player.getX();
        double deltaY = entity.getY() + entity.getHeight() / 2.0
                - (client.player.getY() + client.player.getEyeHeight(client.player.getPose()));
        double deltaZ = entity.getZ() - client.player.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        client.player.setYaw((float) Math.toDegrees(Math.atan2(-deltaX, deltaZ)));
        client.player.setPitch((float) Math.toDegrees(-Math.atan2(deltaY, horizontalDistance)));
    }

    private boolean hasRequiredState(MinecraftClient client) {
        return hasPlayer(client)
                && client.world != null
                && client.interactionManager != null
                && client.getNetworkHandler() != null
                && client.options != null
                && client.options.sneakKey != null;
    }

    private boolean hasPlayer(MinecraftClient client) {
        return client != null && client.player != null;
    }

    private static boolean isValidSlot(HorseScreenHandler handler, int slot) {
        return slot >= 0 && slot < handler.slots.size();
    }

    private static boolean isShulker(ItemStack stack) {
        return stack.getItem().toString().contains("shulker_box");
    }

    private static final class ItemMovePlan {
        private static final int CLICK_DELAY_TICKS = 1;

        private final List<Move> moves;
        private int moveIndex;
        private boolean holdingStack;
        private int delay;

        private ItemMovePlan(List<Move> moves) {
            this.moves = moves;
        }

        static ItemMovePlan toDonkey(HorseScreenHandler handler) {
            List<Integer> shulkerSlots = new ArrayList<>();
            for (int slot = 17; slot <= 52; slot++) {
                if (isValidSlot(handler, slot) && isShulker(handler.getSlot(slot).getStack())) {
                    shulkerSlots.add(slot);
                }
            }

            List<Move> moves = new ArrayList<>();
            int shulkerIndex = 0;
            for (int donkeySlot = 2; donkeySlot <= 16 && shulkerIndex < shulkerSlots.size(); donkeySlot++) {
                if (isValidSlot(handler, donkeySlot) && handler.getSlot(donkeySlot).getStack().isEmpty()) {
                    moves.add(new Move(shulkerSlots.get(shulkerIndex), donkeySlot));
                    shulkerIndex++;
                }
            }

            return new ItemMovePlan(moves);
        }

        boolean tick(MinecraftClient client, HorseScreenHandler handler) {
            if (delay > 0) {
                delay--;
                return false;
            }

            if (moveIndex >= moves.size()) {
                return true;
            }

            Move move = moves.get(moveIndex);
            if (!isValidSlot(handler, move.fromSlot()) || !isValidSlot(handler, move.toSlot())) {
                moveIndex++;
                holdingStack = false;
                return false;
            }

            if (!holdingStack) {
                if (handler.getSlot(move.fromSlot()).getStack().isEmpty()) {
                    moveIndex++;
                    return false;
                }

                client.interactionManager.clickSlot(handler.syncId, move.fromSlot(), 0, SlotActionType.PICKUP, client.player);
                holdingStack = true;
                delay = CLICK_DELAY_TICKS;
                return false;
            }

            client.interactionManager.clickSlot(handler.syncId, move.toSlot(), 0, SlotActionType.PICKUP, client.player);
            holdingStack = false;
            moveIndex++;
            delay = CLICK_DELAY_TICKS;
            return false;
        }
    }

    private record Move(int fromSlot, int toSlot) {
    }
}

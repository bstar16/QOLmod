package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.automation.TaskContext;
import com.bstar.qolmod.automation.input.ControlledInput;
import com.bstar.qolmod.automation.task.DelayTask;
import com.bstar.qolmod.automation.task.QOLTask;
import com.bstar.qolmod.automation.task.SequenceTask;
import com.bstar.qolmod.automation.task.TaskOutcome;
import com.bstar.qolmod.automation.task.TaskResult;
import com.bstar.qolmod.automation.task.TimeoutTask;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** Named, exploit-specific tasks used to compose one AutoDuper cycle. */
final class AutoDuperTasks {
    private static final int MOUNT_TIMEOUT_TICKS = 10 * DelayTask.TICKS_PER_SECOND;
    private static final int INVENTORY_OPEN_TIMEOUT_TICKS = 10 * DelayTask.TICKS_PER_SECOND;
    private static final int INVENTORY_CLOSE_TIMEOUT_TICKS = 5 * DelayTask.TICKS_PER_SECOND;
    private static final int DISMOUNT_TIMEOUT_TICKS = 5 * DelayTask.TICKS_PER_SECOND;

    private AutoDuperTasks() {
    }

    static QOLTask oneCycle(AutoDuperSession session) {
        List<QOLTask> cycle = new ArrayList<>();
        cycle.add(new PrepareHotbarTask(session));
        addDelay(cycle, session.config().keyPressDelayTicks(), "Preparing hotbar");
        cycle.add(new TimeoutTask(new MountDonkeyTask(session), MOUNT_TIMEOUT_TICKS));
        cycle.add(new TimeoutTask(new OpenMountInventoryTask(session), INVENTORY_OPEN_TIMEOUT_TICKS));
        addDelay(cycle, session.config().keyPressDelayTicks(), "Opening mount inventory");

        List<QOLTask> screenSensitive = new ArrayList<>();
        screenSensitive.add(new MoveItemsToDonkeyTask(session));
        addDelay(screenSensitive, session.config().moveItemsDelayTicks(), "Moving items to donkey");
        screenSensitive.add(new ApplyChestTask(session));
        addDelay(screenSensitive, session.config().chestApplyDelayTicks(), "Applying chest");
        screenSensitive.add(new MoveItemsFromDonkeyTask(session));
        addDelay(screenSensitive, session.config().moveItemsDelayTicks(), "Moving duplicated items");
        cycle.add(new MountScreenGuardTask(session, new SequenceTask(screenSensitive)));

        cycle.add(new TimeoutTask(new CloseMountInventoryTask(session), INVENTORY_CLOSE_TIMEOUT_TICKS));
        addDelay(cycle, session.config().keyPressDelayTicks(), "Closing mount inventory");
        cycle.add(new TimeoutTask(new DismountDonkeyTask(session), DISMOUNT_TIMEOUT_TICKS));
        return new SequenceTask(cycle);
    }

    private static void addDelay(List<QOLTask> tasks, int ticks, String activity) {
        if (ticks > 0) {
            tasks.add(DelayTask.ticks(ticks, activity));
        }
    }

    private static final class PrepareHotbarTask implements QOLTask {
        private final AutoDuperSession session;

        private PrepareHotbarTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            int chestSlot = session.findChestInHotbar(context);
            if (chestSlot < 0) {
                return TaskResult.failure("No chest found in hotbar");
            }
            session.sendMessage(context, "Selecting chest in hotbar");
            session.selectHotbarSlot(context, chestSlot);
            return TaskResult.success();
        }

        @Override
        public String activity() {
            return "Preparing hotbar";
        }
    }

    private static final class MountDonkeyTask implements QOLTask {
        private enum Phase {
            READY_TO_INTERACT,
            WAITING_TO_CHECK,
            SETTLING
        }

        private final AutoDuperSession session;
        private Phase phase = Phase.READY_TO_INTERACT;
        private int interactionDelay;
        private DelayTask mountDelay;
        private boolean announced;

        private MountDonkeyTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            DonkeyEntity mountedDonkey = session.mountedDonkey(context);
            if (phase == Phase.READY_TO_INTERACT && mountedDonkey != null) {
                return TaskResult.success();
            }

            if (phase == Phase.WAITING_TO_CHECK) {
                if (interactionDelay > 0) {
                    interactionDelay--;
                    return TaskResult.running();
                }
                if (mountedDonkey == null) {
                    phase = Phase.READY_TO_INTERACT;
                    return TaskResult.running();
                }

                int settleTicks = session.config().mountDelayTicks();
                if (settleTicks == 0) {
                    return TaskResult.success();
                }
                mountDelay = DelayTask.ticks(settleTicks, "Waiting for donkey mount to settle");
                mountDelay.start(context);
                phase = Phase.SETTLING;
                return TaskResult.running();
            }

            if (phase == Phase.SETTLING) {
                if (mountedDonkey == null) {
                    return TaskResult.failure("Player dismounted before the mount delay completed");
                }
                return mountDelay.tick(context);
            }

            if (context.qol().player().getVehicle() != null) {
                return TaskResult.failure("Player is riding an entity that is not a donkey");
            }
            DonkeyEntity donkey = session.findNearestAvailableDonkey(context);
            if (donkey == null) {
                return TaskResult.failure("No unoccupied donkey found within 5 blocks");
            }
            if (!announced) {
                session.sendMessage(context, "Mounting donkey");
                announced = true;
            }
            session.interactWithDonkey(context, donkey);
            interactionDelay = session.config().keyPressDelayTicks();
            phase = Phase.WAITING_TO_CHECK;
            return TaskResult.running();
        }

        @Override
        public String activity() {
            return phase == Phase.READY_TO_INTERACT ? "Mounting donkey" : "Waiting for donkey mount";
        }

        @Override
        public boolean isWaiting() {
            return true;
        }
    }

    private static final class OpenMountInventoryTask implements QOLTask {
        private enum Phase {
            SEND_OPEN_PACKET,
            WAITING_TO_CHECK
        }

        private final AutoDuperSession session;
        private Phase phase = Phase.SEND_OPEN_PACKET;
        private int inventoryDelay;
        private boolean announced;

        private OpenMountInventoryTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            if (session.mountedDonkey(context) == null) {
                return TaskResult.failure("Player dismounted before mount inventory opened");
            }

            if (phase == Phase.WAITING_TO_CHECK) {
                if (inventoryDelay > 0) {
                    inventoryDelay--;
                    return TaskResult.running();
                }
                if (session.currentHorseScreen(context) != null) {
                    return TaskResult.success();
                }
                phase = Phase.SEND_OPEN_PACKET;
                return TaskResult.running();
            }

            if (!announced) {
                session.sendMessage(context, "Opening inventory");
                announced = true;
            }
            context.qol().networkHandler().sendPacket(new ClientCommandC2SPacket(
                    context.qol().player(),
                    ClientCommandC2SPacket.Mode.OPEN_INVENTORY
            ));
            inventoryDelay = session.config().inventoryDelayTicks();
            phase = Phase.WAITING_TO_CHECK;
            return TaskResult.running();
        }

        @Override
        public void cancel(TaskContext context) {
            session.closeMountInventoryIfSafe(context);
        }

        @Override
        public String activity() {
            return phase == Phase.SEND_OPEN_PACKET ? "Opening mount inventory" : "Waiting for mount inventory";
        }

        @Override
        public boolean isWaiting() {
            return true;
        }
    }

    private static final class MountScreenGuardTask implements QOLTask {
        private final AutoDuperSession session;
        private final QOLTask child;

        private MountScreenGuardTask(AutoDuperSession session, QOLTask child) {
            this.session = session;
            this.child = child;
        }

        @Override
        public void start(TaskContext context) {
            child.start(context);
        }

        @Override
        public TaskResult tick(TaskContext context) {
            if (session.currentHorseScreen(context) == null) {
                return TaskResult.failure("Mount inventory closed unexpectedly");
            }
            if (session.mountedDonkey(context) == null) {
                return TaskResult.failure("Player dismounted before item transfer completed");
            }
            return child.tick(context);
        }

        @Override
        public void cancel(TaskContext context) {
            child.cancel(context);
            session.closeMountInventoryIfSafe(context);
        }

        @Override
        public String activity() {
            return child.activity();
        }

        @Override
        public boolean isWaiting() {
            return child.isWaiting();
        }
    }

    private static final class MoveItemsToDonkeyTask implements QOLTask {
        private final AutoDuperSession session;
        private ItemMovePlan movePlan;

        private MoveItemsToDonkeyTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            HorseScreenHandler handler = session.currentHorseHandler(context);
            if (handler == null) {
                return TaskResult.failure("Mount inventory closed unexpectedly while moving items to donkey");
            }
            if (movePlan == null) {
                session.sendMessage(context, "Moving items to donkey");
                movePlan = ItemMovePlan.toDonkey(handler);
            }
            return movePlan.tick(context, handler) ? TaskResult.success() : TaskResult.running();
        }

        @Override
        public String activity() {
            return "Moving items to donkey";
        }
    }

    private static final class ApplyChestTask implements QOLTask {
        private final AutoDuperSession session;

        private ApplyChestTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            if (session.currentHorseScreen(context) == null) {
                return TaskResult.failure("Mount inventory closed before chest application");
            }
            DonkeyEntity donkey = session.mountedDonkey(context);
            if (donkey == null) {
                return TaskResult.failure("Player dismounted before chest application");
            }

            session.sendMessage(context, "Applying chest");
            int chestSlot = session.findChestInHotbar(context);
            if (chestSlot < 0) {
                return TaskResult.failure("No chest found in hotbar for chest application");
            }

            session.selectHotbarSlot(context, chestSlot);
            session.interactWithDonkey(context, donkey);
            return TaskResult.success();
        }

        @Override
        public String activity() {
            return "Applying chest";
        }
    }

    private static final class MoveItemsFromDonkeyTask implements QOLTask {
        private final AutoDuperSession session;

        private MoveItemsFromDonkeyTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            HorseScreenHandler handler = session.currentHorseHandler(context);
            if (handler == null) {
                return TaskResult.failure("Mount inventory closed unexpectedly while moving duplicated items");
            }

            session.sendMessage(context, "Taking items from donkey");
            for (int slot = AutoDuperSession.DONKEY_SLOT_FIRST; slot <= AutoDuperSession.DONKEY_SLOT_LAST; slot++) {
                if (!AutoDuperSession.isValidSlot(handler, slot)) {
                    continue;
                }
                ItemStack stack = handler.getSlot(slot).getStack();
                if (stack.isEmpty() || (session.config().shulkersOnly() && !AutoDuperSession.isShulker(stack))) {
                    continue;
                }
                context.qol().interactionManager().clickSlot(
                        handler.syncId,
                        slot,
                        0,
                        SlotActionType.QUICK_MOVE,
                        context.qol().player()
                );
            }
            return TaskResult.success();
        }

        @Override
        public String activity() {
            return "Moving duplicated items";
        }
    }

    private static final class CloseMountInventoryTask implements QOLTask {
        private final AutoDuperSession session;
        private boolean closeSent;

        private CloseMountInventoryTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            if (!closeSent) {
                if (session.currentHorseScreen(context) == null) {
                    return TaskResult.failure("Mount inventory closed unexpectedly before the close step");
                }
                session.sendMessage(context, "Closing inventory");
                context.qol().player().closeHandledScreen();
                closeSent = true;
            }

            if (context.qol().currentScreen() == null) {
                return TaskResult.success();
            }
            if (!(context.qol().currentScreen() instanceof HorseScreen)) {
                return TaskResult.failure("Another screen opened while closing mount inventory");
            }
            return TaskResult.running();
        }

        @Override
        public void cancel(TaskContext context) {
            session.closeMountInventoryIfSafe(context);
        }

        @Override
        public String activity() {
            return "Closing mount inventory";
        }

        @Override
        public boolean isWaiting() {
            return true;
        }
    }

    private static final class DismountDonkeyTask implements QOLTask {
        private enum Phase {
            READY_TO_HOLD,
            HOLDING,
            RETRY_DELAY,
            FINAL_DELAY
        }

        private final AutoDuperSession session;
        private Phase phase = Phase.READY_TO_HOLD;
        private int holdDelay;
        private DelayTask delay;
        private boolean announced;

        private DismountDonkeyTask(AutoDuperSession session) {
            this.session = session;
        }

        @Override
        public TaskResult tick(TaskContext context) {
            if (phase == Phase.RETRY_DELAY || phase == Phase.FINAL_DELAY) {
                TaskResult result = delay.tick(context);
                if (result.outcome() == TaskOutcome.RUNNING) {
                    return result;
                }
                if (phase == Phase.FINAL_DELAY) {
                    return TaskResult.success();
                }
                phase = Phase.READY_TO_HOLD;
                return TaskResult.running();
            }

            if (phase == Phase.HOLDING) {
                if (holdDelay > 0) {
                    holdDelay--;
                    return TaskResult.running();
                }
                context.inputs().release(context.inputOwner(), ControlledInput.SNEAK);
                int dismountDelay = session.config().dismountDelayTicks();
                if (context.qol().player().getVehicle() == null) {
                    if (dismountDelay == 0) {
                        return TaskResult.success();
                    }
                    delay = DelayTask.ticks(dismountDelay, "Waiting after dismount");
                    delay.start(context);
                    phase = Phase.FINAL_DELAY;
                    return TaskResult.running();
                }

                if (dismountDelay == 0) {
                    phase = Phase.READY_TO_HOLD;
                    return TaskResult.running();
                }
                delay = DelayTask.ticks(dismountDelay, "Waiting to retry dismount");
                delay.start(context);
                phase = Phase.RETRY_DELAY;
                return TaskResult.running();
            }

            if (context.qol().player().getVehicle() == null) {
                return TaskResult.success();
            }
            if (session.mountedDonkey(context) == null) {
                return TaskResult.failure("Player changed to a non-donkey vehicle before dismount");
            }

            if (!announced) {
                session.sendMessage(context, "Dismounting donkey");
                announced = true;
            }
            context.inputs().hold(context.inputOwner(), ControlledInput.SNEAK);
            holdDelay = session.config().dismountDelayTicks();
            phase = Phase.HOLDING;
            return TaskResult.running();
        }

        @Override
        public void cancel(TaskContext context) {
            context.inputs().release(context.inputOwner(), ControlledInput.SNEAK);
        }

        @Override
        public String activity() {
            return "Dismounting donkey";
        }

        @Override
        public boolean isWaiting() {
            return true;
        }
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

        private static ItemMovePlan toDonkey(HorseScreenHandler handler) {
            List<Integer> shulkerSlots = new ArrayList<>();
            for (int slot = AutoDuperSession.PLAYER_SLOT_FIRST; slot <= AutoDuperSession.PLAYER_SLOT_LAST; slot++) {
                if (AutoDuperSession.isValidSlot(handler, slot)
                        && AutoDuperSession.isShulker(handler.getSlot(slot).getStack())) {
                    shulkerSlots.add(slot);
                }
            }

            List<Move> moves = new ArrayList<>();
            int shulkerIndex = 0;
            for (int slot = AutoDuperSession.DONKEY_SLOT_FIRST;
                    slot <= AutoDuperSession.DONKEY_SLOT_LAST && shulkerIndex < shulkerSlots.size();
                    slot++) {
                if (AutoDuperSession.isValidSlot(handler, slot) && handler.getSlot(slot).getStack().isEmpty()) {
                    moves.add(new Move(shulkerSlots.get(shulkerIndex), slot));
                    shulkerIndex++;
                }
            }
            return new ItemMovePlan(moves);
        }

        private boolean tick(TaskContext context, HorseScreenHandler handler) {
            if (delay > 0) {
                delay--;
                return false;
            }
            if (moveIndex >= moves.size()) {
                return true;
            }

            Move move = moves.get(moveIndex);
            if (!AutoDuperSession.isValidSlot(handler, move.fromSlot())
                    || !AutoDuperSession.isValidSlot(handler, move.toSlot())) {
                moveIndex++;
                holdingStack = false;
                return false;
            }
            if (!holdingStack) {
                if (handler.getSlot(move.fromSlot()).getStack().isEmpty()) {
                    moveIndex++;
                    return false;
                }
                context.qol().interactionManager().clickSlot(
                        handler.syncId,
                        move.fromSlot(),
                        0,
                        SlotActionType.PICKUP,
                        context.qol().player()
                );
                holdingStack = true;
                delay = CLICK_DELAY_TICKS;
                return false;
            }

            context.qol().interactionManager().clickSlot(
                    handler.syncId,
                    move.toSlot(),
                    0,
                    SlotActionType.PICKUP,
                    context.qol().player()
            );
            holdingStack = false;
            moveIndex++;
            delay = CLICK_DELAY_TICKS;
            return false;
        }
    }

    private record Move(int fromSlot, int toSlot) {
    }
}

package com.bstar.qolmod.automation.dev;

import com.bstar.qolmod.automation.AutomationWorkflow;
import com.bstar.qolmod.automation.TaskContext;
import com.bstar.qolmod.automation.input.ControlledInput;
import com.bstar.qolmod.automation.task.DelayTask;
import com.bstar.qolmod.automation.task.QOLTask;
import com.bstar.qolmod.automation.task.SequenceTask;
import com.bstar.qolmod.automation.task.TaskResult;
import com.bstar.qolmod.automation.task.TimeoutTask;
import com.bstar.qolmod.automation.task.WaitUntilTask;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.network.ClientPlayerEntity;
import org.slf4j.Logger;

/** Temporary in-game validation workflows; these are not gameplay features. */
public final class DevelopmentWorkflows {
    private DevelopmentWorkflows() {
    }

    public static AutomationWorkflow delayTest() {
        return workflow(
                "dev-delay-test",
                "Delay Test",
                "Waits for 40 client ticks.",
                context -> DelayTask.ticks(40, "Waiting for delay test")
        );
    }

    public static AutomationWorkflow sneakTest() {
        return workflow(
                "dev-sneak-test",
                "Input Hold Test",
                "Holds sneak for 20 client ticks, then releases it.",
                context -> new SequenceTask(
                        new SetInputTask(ControlledInput.SNEAK, true, "Holding sneak"),
                        DelayTask.ticks(20, "Holding sneak"),
                        new SetInputTask(ControlledInput.SNEAK, false, "Releasing sneak")
                )
        );
    }

    public static AutomationWorkflow mountConditionTest() {
        return workflow(
                "dev-mount-condition-test",
                "Condition Test",
                "Waits up to 30 seconds for the player to mount a vehicle.",
                context -> new TimeoutTask(new WaitUntilTask(
                        "Waiting for player to mount",
                        DevelopmentWorkflows::isLocalPlayerMounted
                ), 30 * DelayTask.TICKS_PER_SECOND)
        );
    }

    private static boolean isLocalPlayerMounted(TaskContext context) {
        ClientPlayerEntity player = context.qol().player();
        return player != null && player.getVehicle() != null;
    }

    public static AutomationWorkflow cancellationTest(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        return workflow(
                "dev-cancellation-test",
                "Cancellation Test",
                "Holds forward until cancelled and logs task cancellation.",
                context -> new CancellationTestTask(logger)
        );
    }

    private static AutomationWorkflow workflow(
            String id,
            String name,
            String description,
            Function<TaskContext, QOLTask> taskFactory
    ) {
        return new AutomationWorkflow() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public QOLTask createTask(TaskContext context) {
                return taskFactory.apply(context);
            }
        };
    }

    private static final class SetInputTask implements QOLTask {
        private final ControlledInput input;
        private final boolean pressed;
        private final String activity;

        private SetInputTask(ControlledInput input, boolean pressed, String activity) {
            this.input = input;
            this.pressed = pressed;
            this.activity = activity;
        }

        @Override
        public void start(TaskContext context) {
            if (pressed) {
                context.inputs().hold(context.inputOwner(), input);
            } else {
                context.inputs().release(context.inputOwner(), input);
            }
        }

        @Override
        public TaskResult tick(TaskContext context) {
            return TaskResult.success();
        }

        @Override
        public String activity() {
            return activity;
        }
    }

    private static final class CancellationTestTask implements QOLTask {
        private final Logger logger;
        private boolean cancelled;

        private CancellationTestTask(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void start(TaskContext context) {
            context.inputs().hold(context.inputOwner(), ControlledInput.FORWARD);
        }

        @Override
        public TaskResult tick(TaskContext context) {
            return TaskResult.running();
        }

        @Override
        public void cancel(TaskContext context) {
            cancelled = true;
            context.inputs().release(context.inputOwner(), ControlledInput.FORWARD);
            logger.info("Development Cancellation Test task.cancel() called; forward input released.");
        }

        @Override
        public String activity() {
            return cancelled ? "Cancellation received" : "Holding forward; cancel this workflow";
        }
    }
}

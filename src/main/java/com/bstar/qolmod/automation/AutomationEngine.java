package com.bstar.qolmod.automation;

import com.bstar.qolmod.automation.input.InputController;
import com.bstar.qolmod.automation.task.QOLTask;
import com.bstar.qolmod.automation.task.TaskOutcome;
import com.bstar.qolmod.automation.task.TaskResult;
import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.event.events.ClientShutdownEvent;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.event.events.PlayerDeathEvent;
import com.bstar.qolmod.event.events.WorldLeaveEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

/** Single-slot, client-tick automation scheduler and lifecycle owner. */
public final class AutomationEngine {
    private final QOLContext qolContext;
    private final QOLEventBus eventBus;
    private final Logger logger;
    private final InputController inputController;
    private final List<EventSubscription> subscriptions = new ArrayList<>();
    private AutomationStatus status = AutomationStatus.idle();
    private ActiveRun activeRun;
    private boolean registered;
    private boolean stopping;
    private boolean shutDown;

    public AutomationEngine(QOLContext qolContext, QOLEventBus eventBus, Logger logger) {
        this.qolContext = Objects.requireNonNull(qolContext, "qolContext");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.logger = Objects.requireNonNull(logger, "logger");
        inputController = new InputController(qolContext);
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException("AutomationEngine is already registered");
        }
        registered = true;
        subscriptions.add(eventBus.subscribe(ClientTickEvent.class, this::onClientTick));
        subscriptions.add(eventBus.subscribe(WorldLeaveEvent.class,
                event -> lifecycleStop(AutomationStopReason.WORLD_LEFT, "World left")));
        subscriptions.add(eventBus.subscribe(PlayerDeathEvent.class,
                event -> lifecycleStop(AutomationStopReason.PLAYER_DIED, "Player died")));
        subscriptions.add(eventBus.subscribe(ClientShutdownEvent.class,
                event -> lifecycleStop(AutomationStopReason.CLIENT_SHUTDOWN, "Client shutdown")));
    }

    public AutomationStartResult start(AutomationWorkflow workflow) {
        Objects.requireNonNull(workflow, "workflow");
        if (activeRun != null) {
            String requestedName = safeWorkflowName(workflow);
            return AutomationStartResult.rejected(
                    "Cannot start " + requestedName + ": \"" + activeRun.name + "\" is already running."
            );
        }
        if (stopping) {
            return AutomationStartResult.rejected("Cannot start automation while the previous workflow is stopping.");
        }
        if (shutDown) {
            return AutomationStartResult.rejected("Cannot start automation after client shutdown.");
        }
        if (!qolContext.isPlayable() || qolContext.player().isDead()) {
            return AutomationStartResult.rejected("Cannot start automation without a living player in a playable world.");
        }

        final String id;
        final String name;
        final String description;
        try {
            id = requireMetadata(workflow.id(), "id");
            name = requireMetadata(workflow.name(), "name");
            description = Objects.requireNonNullElse(workflow.description(), "");
        } catch (RuntimeException exception) {
            logger.error("Failed to read automation workflow metadata.", exception);
            status = errorStatus(null, null, "Workflow metadata failed", exception.getMessage(), 0);
            return AutomationStartResult.rejected("Cannot start workflow: invalid workflow metadata.");
        }

        AutomationInputOwner owner = new AutomationInputOwner(id);
        ActiveRun run = new ActiveRun(id, name, description, owner);
        try {
            inputController.activate(owner);
            activeRun = run;
            run.root = Objects.requireNonNull(workflow.createTask(run.context), "Workflow returned a null root task");
            run.root.start(run.context);
            status = runningStatus(run, Optional.empty());
            logger.info("Started automation workflow '{}' ({}).", name, id);
            return AutomationStartResult.started("Started automation: " + name);
        } catch (RuntimeException exception) {
            logger.error("Failed to create or start automation workflow '{}' ({}).", name, id, exception);
            if (activeRun == null) {
                activeRun = run;
            }
            finishRun(AutomationStopReason.ERROR, "Workflow start failed: " + usefulMessage(exception), true);
            return AutomationStartResult.rejected("Cannot start " + name + ": " + usefulMessage(exception));
        }
    }

    public boolean cancel() {
        return stop(AutomationStopReason.USER_CANCELLED, "Cancelled by user");
    }

    public boolean panic() {
        boolean stopped = stop(AutomationStopReason.PANIC, "Panic reset");
        releaseOrphanedInputs("panic");
        return stopped;
    }

    public void shutdown() {
        if (shutDown) {
            return;
        }
        shutDown = true;
        stop(AutomationStopReason.CLIENT_SHUTDOWN, "Client shutdown");
        releaseOrphanedInputs("client shutdown");
        for (EventSubscription subscription : List.copyOf(subscriptions)) {
            subscription.close();
        }
        subscriptions.clear();
    }

    public AutomationStatus status() {
        return status;
    }

    public boolean isRunning() {
        return activeRun != null;
    }

    public Optional<String> activeWorkflowName() {
        return activeRun == null ? Optional.empty() : Optional.of(activeRun.name);
    }

    public InputController inputs() {
        return inputController;
    }

    private void onClientTick(ClientTickEvent event) {
        ActiveRun run = activeRun;
        if (run == null || stopping) {
            return;
        }

        try {
            if (qolContext.currentScreen() == null
                    && inputController.hasHeldInputs(run.owner)
                    && inputController.hasPhysicalManualMovementInput()) {
                stop(AutomationStopReason.MANUAL_OVERRIDE, "Manual movement input detected");
                notifyPlayer("Automation cancelled: manual input", Formatting.YELLOW);
                return;
            }

            inputController.tick(run.owner);
            TaskResult result = Objects.requireNonNull(run.root.tick(run.context), "Root task returned null");
            run.elapsedTicks++;

            if (result.outcome() == TaskOutcome.RUNNING) {
                status = runningStatus(run, result.detail());
                return;
            }
            if (result.outcome() == TaskOutcome.SUCCESS) {
                finishRun(
                        AutomationStopReason.COMPLETED,
                        result.detail().orElse("Workflow completed"),
                        false
                );
                return;
            }

            result.cause().ifPresent(exception -> logger.error(
                    "Automation task failed in workflow '{}' ({}).", run.name, run.id, exception));
            finishRun(
                    result.stopReason().orElse(AutomationStopReason.ERROR),
                    result.detail().orElse("Task failed"),
                    true
            );
        } catch (RuntimeException exception) {
            logger.error("Automation tick failed in workflow '{}' ({}).", run.name, run.id, exception);
            finishRun(AutomationStopReason.ERROR, "Task tick failed: " + usefulMessage(exception), true);
        }
    }

    private boolean stop(AutomationStopReason reason, String detail) {
        Objects.requireNonNull(reason, "reason");
        if (activeRun == null) {
            return false;
        }
        finishRun(reason, detail, reason != AutomationStopReason.COMPLETED);
        return true;
    }

    private void lifecycleStop(AutomationStopReason reason, String detail) {
        stop(reason, detail);
        releaseOrphanedInputs(reason.name().toLowerCase());
    }

    private void finishRun(AutomationStopReason requestedReason, String detail, boolean cancelTask) {
        ActiveRun run = activeRun;
        if (run == null || stopping) {
            return;
        }

        stopping = true;
        AutomationStopReason finalReason = requestedReason;
        String finalDetail = Objects.requireNonNullElse(detail, requestedReason.name());
        RuntimeException cleanupFailure = null;
        try {
            if (cancelTask && run.root != null) {
                try {
                    run.root.cancel(run.context);
                } catch (RuntimeException exception) {
                    cleanupFailure = exception;
                    logger.error("Automation task cancellation failed in workflow '{}' ({}).", run.name, run.id, exception);
                }
            }

            try {
                inputController.releaseAll(run.owner);
            } catch (RuntimeException exception) {
                if (cleanupFailure == null) {
                    cleanupFailure = exception;
                } else {
                    cleanupFailure.addSuppressed(exception);
                }
                logger.error("Failed to release inputs for automation workflow '{}' ({}).", run.name, run.id, exception);
                try {
                    inputController.releaseAllAutomationInputs();
                } catch (RuntimeException emergencyException) {
                    exception.addSuppressed(emergencyException);
                    logger.error("Emergency input release also failed for workflow '{}' ({}).",
                            run.name, run.id, emergencyException);
                }
            }

            if (cleanupFailure != null) {
                finalReason = AutomationStopReason.ERROR;
                finalDetail = "Automation cleanup failed: " + usefulMessage(cleanupFailure);
            }

            AutomationState finalState = stateFor(finalReason);
            status = new AutomationStatus(
                    finalState,
                    Optional.of(run.id),
                    Optional.of(run.name),
                    finalState == AutomationState.COMPLETED ? "Completed" : finalState == AutomationState.ERROR ? "Error" : "Cancelled",
                    Optional.of(finalDetail),
                    run.elapsedTicks,
                    Optional.of(finalReason)
            );
            logger.info("Stopped automation workflow '{}' ({}): {} - {}", run.name, run.id, finalReason, finalDetail);
        } finally {
            activeRun = null;
            stopping = false;
        }
    }

    private AutomationStatus runningStatus(ActiveRun run, Optional<String> detail) {
        AutomationState state = run.root != null && run.root.isWaiting()
                ? AutomationState.WAITING
                : AutomationState.RUNNING;
        return new AutomationStatus(
                state,
                Optional.of(run.id),
                Optional.of(run.name),
                run.root == null ? "Starting" : run.root.activity(),
                detail.isPresent() ? detail : optionalText(run.description),
                run.elapsedTicks,
                Optional.empty()
        );
    }

    private AutomationStatus errorStatus(String id, String name, String activity, String detail, long elapsedTicks) {
        return new AutomationStatus(
                AutomationState.ERROR,
                optionalText(id),
                optionalText(name),
                activity,
                optionalText(detail),
                elapsedTicks,
                Optional.of(AutomationStopReason.ERROR)
        );
    }

    private void releaseOrphanedInputs(String lifecycle) {
        try {
            inputController.releaseAllAutomationInputs();
        } catch (RuntimeException exception) {
            logger.error("Failed to release orphaned automation inputs during {}.", lifecycle, exception);
            status = errorStatus(null, null, "Input cleanup failed", usefulMessage(exception), 0);
        }
    }

    private void notifyPlayer(String message, Formatting formatting) {
        if (qolContext.player() != null) {
            qolContext.player().sendMessage(
                    Text.literal("[QOLmod] ").formatted(Formatting.GRAY)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }

    private AutomationState stateFor(AutomationStopReason reason) {
        return switch (reason) {
            case COMPLETED -> AutomationState.COMPLETED;
            case ERROR, TIMEOUT -> AutomationState.ERROR;
            default -> AutomationState.CANCELLED;
        };
    }

    private String safeWorkflowName(AutomationWorkflow workflow) {
        try {
            return requireMetadata(workflow.name(), "name");
        } catch (RuntimeException exception) {
            return "workflow";
        }
    }

    private String requireMetadata(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Workflow " + field + " cannot be blank");
        }
        return value;
    }

    private Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private String usefulMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private final class ActiveRun {
        private final String id;
        private final String name;
        private final String description;
        private final AutomationInputOwner owner;
        private final TaskContext context;
        private QOLTask root;
        private long elapsedTicks;

        private ActiveRun(String id, String name, String description, AutomationInputOwner owner) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.owner = owner;
            context = new TaskContext() {
                @Override
                public QOLContext qol() {
                    return qolContext;
                }

                @Override
                public AutomationEngine automation() {
                    return AutomationEngine.this;
                }

                @Override
                public InputController inputs() {
                    return inputController;
                }

                @Override
                public AutomationInputOwner inputOwner() {
                    return owner;
                }

                @Override
                public long elapsedTicks() {
                    return ActiveRun.this.elapsedTicks;
                }
            };
        }
    }
}

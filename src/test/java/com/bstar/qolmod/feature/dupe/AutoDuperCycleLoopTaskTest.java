package com.bstar.qolmod.feature.dupe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bstar.qolmod.automation.AutomationEngine;
import com.bstar.qolmod.automation.AutomationInputOwner;
import com.bstar.qolmod.automation.TaskContext;
import com.bstar.qolmod.automation.input.InputController;
import com.bstar.qolmod.automation.task.QOLTask;
import com.bstar.qolmod.automation.task.TaskOutcome;
import com.bstar.qolmod.automation.task.TaskResult;
import com.bstar.qolmod.core.QOLContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AutoDuperCycleLoopTaskTest {
    private static final TaskContext CONTEXT = new EmptyTaskContext();

    @Test
    void finiteLoopCompletesAfterExactConfiguredCycleCount() {
        List<Integer> createdCycles = new ArrayList<>();
        List<Integer> completedCycles = new ArrayList<>();
        AutoDuperCycleLoopTask loop = new AutoDuperCycleLoopTask(
                3,
                cycle -> {
                    createdCycles.add(cycle);
                    return successfulCycle();
                },
                completedCycles::add
        );

        loop.start(CONTEXT);

        assertEquals(TaskOutcome.RUNNING, loop.tick(CONTEXT).outcome());
        assertEquals(TaskOutcome.RUNNING, loop.tick(CONTEXT).outcome());
        TaskResult result = loop.tick(CONTEXT);

        assertEquals(TaskOutcome.SUCCESS, result.outcome());
        assertEquals(3, loop.completedCycles());
        assertEquals(List.of(1, 2, 3), createdCycles);
        assertEquals(List.of(1, 2, 3), completedCycles);
    }

    @Test
    void zeroCyclesRepeatsUntilCancelledAndCancelsActiveCycle() {
        AtomicBoolean activeCycleCancelled = new AtomicBoolean();
        AutoDuperCycleLoopTask loop = new AutoDuperCycleLoopTask(
                0,
                cycle -> new QOLTask() {
                    @Override
                    public TaskResult tick(TaskContext context) {
                        return TaskResult.success();
                    }

                    @Override
                    public void cancel(TaskContext context) {
                        activeCycleCancelled.set(true);
                    }
                },
                ignored -> {
                }
        );

        loop.start(CONTEXT);
        for (int cycle = 0; cycle < 4; cycle++) {
            assertEquals(TaskOutcome.RUNNING, loop.tick(CONTEXT).outcome());
        }
        loop.cancel(CONTEXT);

        assertEquals(4, loop.completedCycles());
        assertTrue(activeCycleCancelled.get());
    }

    @Test
    void failureDoesNotIncrementOrCreateAnotherCycle() {
        List<Integer> createdCycles = new ArrayList<>();
        AutoDuperCycleLoopTask loop = new AutoDuperCycleLoopTask(
                2,
                cycle -> {
                    createdCycles.add(cycle);
                    return new QOLTask() {
                        @Override
                        public TaskResult tick(TaskContext context) {
                            return TaskResult.failure("expected failure");
                        }
                    };
                },
                ignored -> {
                }
        );

        loop.start(CONTEXT);
        TaskResult result = loop.tick(CONTEXT);

        assertEquals(TaskOutcome.FAILURE, result.outcome());
        assertEquals(0, loop.completedCycles());
        assertEquals(List.of(1), createdCycles);
    }

    private static QOLTask successfulCycle() {
        return new QOLTask() {
            @Override
            public TaskResult tick(TaskContext context) {
                return TaskResult.success();
            }
        };
    }

    private static final class EmptyTaskContext implements TaskContext {
        @Override
        public QOLContext qol() {
            return null;
        }

        @Override
        public AutomationEngine automation() {
            return null;
        }

        @Override
        public InputController inputs() {
            return null;
        }

        @Override
        public AutomationInputOwner inputOwner() {
            return null;
        }

        @Override
        public long elapsedTicks() {
            return 0;
        }
    }
}

package com.bstar.qolmod.automation.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bstar.qolmod.automation.AutomationEngine;
import com.bstar.qolmod.automation.AutomationInputOwner;
import com.bstar.qolmod.automation.AutomationStopReason;
import com.bstar.qolmod.automation.TaskContext;
import com.bstar.qolmod.automation.input.InputController;
import com.bstar.qolmod.core.QOLContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CoreTaskTest {
    private static final TaskContext CONTEXT = new EmptyTaskContext();

    @Test
    void emptySequenceCompletesSafely() {
        SequenceTask sequence = new SequenceTask(List.of());

        sequence.start(CONTEXT);

        assertEquals(TaskOutcome.SUCCESS, sequence.tick(CONTEXT).outcome());
    }

    @Test
    void sequenceStartsAndTicksChildrenInOrder() {
        List<String> events = new ArrayList<>();
        RecordingTask first = new RecordingTask("first", events, TaskResult.success());
        RecordingTask second = new RecordingTask("second", events, TaskResult.running(), TaskResult.success());
        SequenceTask sequence = new SequenceTask(first, second);

        sequence.start(CONTEXT);
        assertEquals(List.of("start:first"), events);

        assertEquals(TaskOutcome.RUNNING, sequence.tick(CONTEXT).outcome());
        assertEquals(List.of("start:first", "tick:first", "start:second"), events);
        assertEquals("second", sequence.activity());

        assertEquals(TaskOutcome.RUNNING, sequence.tick(CONTEXT).outcome());
        assertEquals(TaskOutcome.SUCCESS, sequence.tick(CONTEXT).outcome());
        assertEquals(List.of(
                "start:first",
                "tick:first",
                "start:second",
                "tick:second",
                "tick:second"
        ), events);
    }

    @Test
    void sequencePropagatesFailureAndCancelsActiveChild() {
        List<String> events = new ArrayList<>();
        RecordingTask failing = new RecordingTask("failing", events, TaskResult.failure("expected failure"));
        RecordingTask neverStarted = new RecordingTask("never", events, TaskResult.success());
        SequenceTask sequence = new SequenceTask(failing, neverStarted);

        sequence.start(CONTEXT);
        TaskResult result = sequence.tick(CONTEXT);
        sequence.cancel(CONTEXT);

        assertEquals(TaskOutcome.FAILURE, result.outcome());
        assertEquals("expected failure", result.detail().orElseThrow());
        assertEquals(List.of("start:failing", "tick:failing", "cancel:failing"), events);
    }

    @Test
    void delayUsesAnExactNumberOfTicks() {
        DelayTask delay = DelayTask.ticks(3);
        delay.start(CONTEXT);

        assertEquals(TaskOutcome.RUNNING, delay.tick(CONTEXT).outcome());
        assertEquals(TaskOutcome.RUNNING, delay.tick(CONTEXT).outcome());
        assertEquals(TaskOutcome.SUCCESS, delay.tick(CONTEXT).outcome());
        assertEquals(3, delay.elapsedTicks());
    }

    @Test
    void delaySecondsRoundsUpPredictably() {
        assertEquals(1, DelayTask.seconds(0.001).durationTicks());
        assertEquals(20, DelayTask.seconds(1.0).durationTicks());
        assertEquals(21, DelayTask.seconds(1.001).durationTicks());
    }

    @Test
    void waitUntilRunsUntilConditionBecomesTrue() {
        AtomicInteger checks = new AtomicInteger();
        WaitUntilTask wait = new WaitUntilTask("Waiting for test", context -> checks.incrementAndGet() == 2);

        assertEquals(TaskOutcome.RUNNING, wait.tick(CONTEXT).outcome());
        assertEquals(TaskOutcome.SUCCESS, wait.tick(CONTEXT).outcome());
        assertTrue(wait.isWaiting());
    }

    @Test
    void waitUntilTurnsConditionExceptionsIntoFailure() {
        WaitUntilTask wait = new WaitUntilTask("Waiting for failure", context -> {
            throw new IllegalStateException("condition exploded");
        });

        TaskResult result = wait.tick(CONTEXT);

        assertEquals(TaskOutcome.FAILURE, result.outcome());
        assertEquals(AutomationStopReason.ERROR, result.stopReason().orElseThrow());
        assertNotNull(result.cause().orElseThrow());
    }

    @Test
    void timeoutCancelsAStillRunningChild() {
        AtomicBoolean cancelled = new AtomicBoolean();
        QOLTask child = new QOLTask() {
            @Override
            public TaskResult tick(TaskContext context) {
                return TaskResult.running();
            }

            @Override
            public void cancel(TaskContext context) {
                cancelled.set(true);
            }
        };
        TimeoutTask timeout = new TimeoutTask(child, 3);
        timeout.start(CONTEXT);

        assertEquals(TaskOutcome.RUNNING, timeout.tick(CONTEXT).outcome());
        assertEquals(TaskOutcome.RUNNING, timeout.tick(CONTEXT).outcome());
        TaskResult result = timeout.tick(CONTEXT);

        assertEquals(TaskOutcome.FAILURE, result.outcome());
        assertEquals(AutomationStopReason.TIMEOUT, result.stopReason().orElseThrow());
        assertTrue(cancelled.get());
    }

    @Test
    void timeoutAllowsSuccessOnTheFinalTick() {
        AtomicInteger ticks = new AtomicInteger();
        AtomicBoolean cancelled = new AtomicBoolean();
        QOLTask child = new QOLTask() {
            @Override
            public TaskResult tick(TaskContext context) {
                return ticks.incrementAndGet() == 3 ? TaskResult.success() : TaskResult.running();
            }

            @Override
            public void cancel(TaskContext context) {
                cancelled.set(true);
            }
        };
        TimeoutTask timeout = new TimeoutTask(child, 3);
        timeout.start(CONTEXT);

        timeout.tick(CONTEXT);
        timeout.tick(CONTEXT);
        TaskResult result = timeout.tick(CONTEXT);

        assertEquals(TaskOutcome.SUCCESS, result.outcome());
        assertFalse(cancelled.get());
    }

    private static final class RecordingTask implements QOLTask {
        private final String name;
        private final List<String> events;
        private final List<TaskResult> results;
        private int ticks;

        private RecordingTask(String name, List<String> events, TaskResult... results) {
            this.name = name;
            this.events = events;
            this.results = List.of(results);
        }

        @Override
        public void start(TaskContext context) {
            events.add("start:" + name);
        }

        @Override
        public TaskResult tick(TaskContext context) {
            events.add("tick:" + name);
            return results.get(Math.min(ticks++, results.size() - 1));
        }

        @Override
        public void cancel(TaskContext context) {
            events.add("cancel:" + name);
        }

        @Override
        public String activity() {
            return name;
        }
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

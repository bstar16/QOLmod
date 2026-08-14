package com.bstar.qolmod.automation.task;

import com.bstar.qolmod.automation.TaskContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Runs one child at a time in declaration order. */
public final class SequenceTask implements QOLTask {
    private final List<QOLTask> children;
    private int activeIndex;
    private boolean started;
    private boolean complete;

    public SequenceTask(List<? extends QOLTask> children) {
        Objects.requireNonNull(children, "children");
        this.children = List.copyOf(children);
        if (this.children.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Sequence children cannot contain null");
        }
    }

    public SequenceTask(QOLTask... children) {
        this(Arrays.asList(children));
    }

    @Override
    public void start(TaskContext context) {
        if (started) {
            throw new IllegalStateException("SequenceTask cannot be started twice");
        }
        started = true;
        if (children.isEmpty()) {
            complete = true;
            return;
        }
        current().start(context);
    }

    @Override
    public TaskResult tick(TaskContext context) {
        requireStarted();
        if (complete) {
            return TaskResult.success();
        }

        TaskResult result = Objects.requireNonNull(current().tick(context), "Child task returned null");
        if (result.outcome() != TaskOutcome.SUCCESS) {
            return result;
        }

        activeIndex++;
        if (activeIndex >= children.size()) {
            complete = true;
            return result.detail().map(TaskResult::success).orElseGet(TaskResult::success);
        }

        current().start(context);
        return TaskResult.running();
    }

    @Override
    public void cancel(TaskContext context) {
        if (started && !complete && activeIndex < children.size()) {
            current().cancel(context);
        }
    }

    @Override
    public String activity() {
        if (!started) {
            return children.isEmpty() ? "Empty sequence" : children.getFirst().activity();
        }
        return complete ? "Sequence complete" : current().activity();
    }

    @Override
    public boolean isWaiting() {
        return started && !complete && current().isWaiting();
    }

    private QOLTask current() {
        return children.get(activeIndex);
    }

    private void requireStarted() {
        if (!started) {
            throw new IllegalStateException("SequenceTask must be started before it is ticked");
        }
    }
}

package com.bstar.qolmod.automation.task;

import com.bstar.qolmod.automation.TaskContext;

/** A non-blocking delay measured against client tick callbacks. */
public final class DelayTask implements QOLTask {
    public static final int TICKS_PER_SECOND = 20;

    private final long durationTicks;
    private final String activity;
    private long elapsedTicks;
    private boolean started;

    private DelayTask(long durationTicks, String activity) {
        if (durationTicks < 0) {
            throw new IllegalArgumentException("Delay duration cannot be negative");
        }
        this.durationTicks = durationTicks;
        this.activity = activity;
    }

    public static DelayTask ticks(long ticks) {
        return ticks(ticks, "Waiting " + ticks + " tick" + (ticks == 1 ? "" : "s"));
    }

    public static DelayTask ticks(long ticks, String activity) {
        return new DelayTask(ticks, activity == null || activity.isBlank() ? "Waiting" : activity);
    }

    /** Converts seconds with ceil(seconds * 20), so a delay is never shorter than requested. */
    public static DelayTask seconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Delay seconds must be finite and non-negative");
        }
        long ticks = (long) Math.ceil(seconds * TICKS_PER_SECOND);
        return ticks(ticks, "Waiting " + seconds + " seconds");
    }

    @Override
    public void start(TaskContext context) {
        if (started) {
            throw new IllegalStateException("DelayTask cannot be started twice");
        }
        started = true;
    }

    @Override
    public TaskResult tick(TaskContext context) {
        if (!started) {
            throw new IllegalStateException("DelayTask must be started before it is ticked");
        }
        if (elapsedTicks >= durationTicks) {
            return TaskResult.success();
        }
        elapsedTicks++;
        return elapsedTicks >= durationTicks ? TaskResult.success() : TaskResult.running();
    }

    @Override
    public String activity() {
        return activity;
    }

    @Override
    public boolean isWaiting() {
        return true;
    }

    public long durationTicks() {
        return durationTicks;
    }

    public long elapsedTicks() {
        return elapsedTicks;
    }
}

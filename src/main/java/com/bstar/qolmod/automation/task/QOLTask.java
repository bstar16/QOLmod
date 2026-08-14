package com.bstar.qolmod.automation.task;

import com.bstar.qolmod.automation.TaskContext;

public interface QOLTask {
    default void start(TaskContext context) {
    }

    TaskResult tick(TaskContext context);

    default void cancel(TaskContext context) {
    }

    default String activity() {
        return getClass().getSimpleName();
    }

    default boolean isWaiting() {
        return false;
    }
}

package com.bstar.qolmod.automation;

import com.bstar.qolmod.automation.task.QOLTask;

public interface AutomationWorkflow {
    String id();

    String name();

    String description();

    QOLTask createTask(TaskContext context);
}

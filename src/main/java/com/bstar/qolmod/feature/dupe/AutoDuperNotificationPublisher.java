package com.bstar.qolmod.feature.dupe;

import com.bstar.qolmod.hud.notification.NotificationManager;
import com.bstar.qolmod.hud.notification.NotificationType;
import java.util.Objects;

/** Restrained Auto Duper terminal-event adapter for the generic notification system. */
public final class AutoDuperNotificationPublisher {
    private static final String COMPLETED_KEY = "automation:auto_duper:completed";
    private static final String ERROR_KEY = "automation:auto_duper:error";
    private static final String TITLE = "Auto Duper";

    private final NotificationManager notifications;

    public AutoDuperNotificationPublisher(NotificationManager notifications) {
        this.notifications = Objects.requireNonNull(notifications, "notifications");
    }

    public void completed(int completedCycles) {
        String cycles = completedCycles + " cycle" + (completedCycles == 1 ? "" : "s");
        notifications.notify(COMPLETED_KEY, NotificationType.SUCCESS, TITLE, "Completed " + cycles);
    }

    public void error(String detail) {
        notifications.notify(ERROR_KEY, NotificationType.ERROR, TITLE, detail);
    }
}

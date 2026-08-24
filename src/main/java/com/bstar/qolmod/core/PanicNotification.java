package com.bstar.qolmod.core;

import com.bstar.qolmod.hud.notification.NotificationManager;
import com.bstar.qolmod.hud.notification.NotificationType;
import java.util.Objects;

/** Publishes the single keyed notification associated with a panic activation. */
final class PanicNotification {
    static final String KEY = "qolmod:panic";

    private PanicNotification() {
    }

    static void publish(NotificationManager notifications) {
        Objects.requireNonNull(notifications, "notifications").notify(
                KEY,
                NotificationType.WARNING,
                "QOLmod",
                "Automation and controlled input stopped"
        );
    }
}

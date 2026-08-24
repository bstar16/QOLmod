package com.bstar.qolmod.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bstar.qolmod.hud.notification.NotificationData;
import com.bstar.qolmod.hud.notification.NotificationEntry;
import com.bstar.qolmod.hud.notification.NotificationManager;
import com.bstar.qolmod.hud.notification.NotificationType;
import org.junit.jupiter.api.Test;

class PanicNotificationTest {
    @Test
    void repeatedPublishingUsesOneStableWarningNotification() {
        NotificationManager notifications = new NotificationManager();

        PanicNotification.publish(notifications);
        PanicNotification.publish(notifications);

        assertEquals(1, notifications.size());
        NotificationEntry entry = notifications.getVisibleEntries().getFirst();
        NotificationData data = entry.data();
        assertEquals(PanicNotification.KEY, data.key().orElseThrow());
        assertEquals(NotificationType.WARNING, data.type());
        assertEquals("QOLmod", data.title());
        assertEquals("Automation and controlled input stopped", data.detail().orElseThrow());
    }
}

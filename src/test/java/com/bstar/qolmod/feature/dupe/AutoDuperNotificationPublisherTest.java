package com.bstar.qolmod.feature.dupe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bstar.qolmod.hud.notification.NotificationManager;
import com.bstar.qolmod.hud.notification.NotificationType;
import org.junit.jupiter.api.Test;

class AutoDuperNotificationPublisherTest {
    private final NotificationManager manager = new NotificationManager();
    private final AutoDuperNotificationPublisher publisher = new AutoDuperNotificationPublisher(manager);

    @Test
    void publishesCompletionAsSuccess() {
        publisher.completed(20);

        var notification = manager.getVisibleEntries().getFirst().data();
        assertEquals(NotificationType.SUCCESS, notification.type());
        assertEquals("Auto Duper", notification.title());
        assertEquals("Completed 20 cycles", notification.detail().orElseThrow());
    }

    @Test
    void repeatedErrorsDeduplicateThroughStableKey() {
        publisher.error("No chest found in hotbar");
        publisher.error("No chest found in hotbar");

        assertEquals(1, manager.size());
        assertEquals(NotificationType.ERROR, manager.getVisibleEntries().getFirst().data().type());
    }
}

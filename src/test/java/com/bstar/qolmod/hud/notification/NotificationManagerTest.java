package com.bstar.qolmod.hud.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class NotificationManagerTest {
    private final AtomicLong now = new AtomicLong(25_000);
    private final NotificationManager manager = new NotificationManager(now::get);

    @Test
    void createsUnkeyedNotification() {
        long id = manager.info("QOLmod", "Configuration saved");

        NotificationEntry entry = manager.getVisibleEntries().getFirst();
        assertEquals(id, entry.instanceId());
        assertTrue(entry.data().key().isEmpty());
        assertEquals(NotificationType.INFO, entry.data().type());
    }

    @Test
    void createsKeyedNotification() {
        manager.notify("auto_duper:no_chest", NotificationType.ERROR, "Auto Duper", "No chest found");

        assertEquals("auto_duper:no_chest",
                manager.getVisibleEntries().getFirst().data().key().orElseThrow());
    }

    @Test
    void repeatedKeyUpdatesExistingEntryWithoutRestartingEntrance() {
        long id = manager.notify("finder:missing", NotificationType.WARNING, "Finder", "Missing one item");
        long createdAt = manager.getVisibleEntries().getFirst().createdAtMillis();
        now.addAndGet(700);

        long updatedId = manager.notify("finder:missing", NotificationType.WARNING, "Finder", "Missing two items");

        assertEquals(id, updatedId);
        assertEquals(1, manager.size());
        NotificationEntry entry = manager.getVisibleEntries().getFirst();
        assertEquals(createdAt, entry.createdAtMillis());
        assertEquals("Missing two items", entry.data().detail().orElseThrow());
    }

    @Test
    void keyedUpdateRefreshesLifetime() {
        manager.notify("builder:materials", NotificationType.WARNING, "Builder", "Materials missing");
        long originalFadeAt = manager.getVisibleEntries().getFirst().fadeAtMillis();
        now.addAndGet(1_000);

        manager.notify("builder:materials", NotificationType.WARNING, "Builder", "Still missing");

        NotificationEntry refreshed = manager.getVisibleEntries().getFirst();
        assertEquals(now.get(), refreshed.updatedAtMillis());
        assertEquals(originalFadeAt + 1_000, refreshed.fadeAtMillis());
    }

    @Test
    void unkeyedNotificationsRemainIndependent() {
        long first = manager.info("QOLmod", "First");
        long second = manager.info("QOLmod", "First");

        assertNotEquals(first, second);
        assertEquals(2, manager.size());
    }

    @Test
    void supportsEverySemanticType() {
        manager.info("Info", null);
        manager.success("Success", null);
        manager.warning("Warning", null);
        manager.error("Error", null);

        assertEquals(
                List.of(NotificationType.INFO, NotificationType.SUCCESS, NotificationType.WARNING, NotificationType.ERROR),
                manager.getVisibleEntries().stream().map(entry -> entry.data().type()).toList()
        );
    }

    @Test
    void notificationExpiresAfterLifetimeAndExitAnimation() {
        manager.success("Auto Duper", "Completed");
        now.addAndGet(NotificationType.SUCCESS.defaultDuration().toMillis());
        assertEquals(1, manager.size());

        now.addAndGet(NotificationManager.EXIT_ANIMATION.toMillis());
        assertEquals(0, manager.size());
    }

    @Test
    void overflowEvictsOldestNonErrorAndPreservesOrdering() {
        manager.error("Error 1", null);
        manager.info("Info 1", null);
        manager.warning("Warning 1", null);
        manager.error("Error 2", null);
        manager.success("Success 1", null);

        manager.info("Newest", null);

        List<String> titles = manager.getVisibleEntries().stream().map(entry -> entry.data().title()).toList();
        assertEquals(NotificationManager.MAX_VISIBLE, titles.size());
        assertEquals(List.of("Error 1", "Warning 1", "Error 2", "Success 1", "Newest"), titles);
    }

    @Test
    void overflowEvictsOldestWhenAllVisibleEntriesAreErrors() {
        for (int index = 0; index < NotificationManager.MAX_VISIBLE; index++) {
            manager.error("Error " + index, null);
        }

        manager.error("Newest error", null);

        List<String> titles = manager.getVisibleEntries().stream().map(entry -> entry.data().title()).toList();
        assertEquals(NotificationManager.MAX_VISIBLE, titles.size());
        assertEquals("Error 1", titles.getFirst());
        assertEquals("Newest error", titles.getLast());
    }

    @Test
    void clearRemovesEntriesAndKeyMappings() {
        long original = manager.notify("stable:key", NotificationType.INFO, "Title", "Detail");
        manager.clear();

        assertTrue(manager.getVisibleEntries().isEmpty());
        long replacement = manager.notify("stable:key", NotificationType.INFO, "Title", "Detail");
        assertNotEquals(original, replacement);
    }

    @Test
    void repeatedKeyNeverCreatesSpam() {
        for (int update = 0; update < 100; update++) {
            manager.notify("auto_duper:no_chest", NotificationType.ERROR, "Auto Duper", "Attempt " + update);
        }

        assertEquals(1, manager.size());
        assertEquals("Attempt 99", manager.getVisibleEntries().getFirst().data().detail().orElseThrow());
    }
}

package com.bstar.qolmod.hud.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StatusRegistryTest {
    private final AtomicLong now = new AtomicLong(10_000);
    private final StatusRegistry registry = new StatusRegistry(now::get);

    @Test
    void publishesNewSource() {
        registry.publish(card("automation:auto_duper", "Starting", StatusState.ACTIVE, StatusProgress.none()));

        assertEquals(1, registry.size());
        assertEquals("automation:auto_duper", registry.getVisibleStatuses().getFirst().data().sourceId());
    }

    @Test
    void updatesSameSourceWithoutRecreatingCard() {
        registry.publish(card("automation:auto_duper", "Finding donkey", StatusState.ACTIVE, StatusProgress.none()));
        long firstPublished = registry.getVisibleStatuses().getFirst().firstPublishedAtMillis();
        now.addAndGet(500);

        registry.publish(card("automation:auto_duper", "Mounting donkey", StatusState.WAITING, StatusProgress.none()));

        assertEquals(1, registry.size());
        VisibleStatus status = registry.getVisibleStatuses().getFirst();
        assertEquals(firstPublished, status.firstPublishedAtMillis());
        assertEquals("Mounting donkey", status.data().activity().orElseThrow());
    }

    @Test
    void multipleSourcesCoexistInPublicationOrder() {
        registry.publish(card("automation:auto_duper", "Working", StatusState.ACTIVE, StatusProgress.none()));
        registry.publish(card("assistant:finder", "Searching", StatusState.WAITING,
                StatusProgress.indeterminate("Searching...")));

        assertEquals(2, registry.size());
        assertEquals("automation:auto_duper", registry.getVisibleStatuses().get(0).data().sourceId());
        assertEquals("assistant:finder", registry.getVisibleStatuses().get(1).data().sourceId());
    }

    @Test
    void removesSource() {
        registry.publish(card("automation:auto_duper", "Working", StatusState.ACTIVE, StatusProgress.none()));

        registry.remove("automation:auto_duper");

        assertTrue(registry.getVisibleStatuses().isEmpty());
    }

    @Test
    void completionIsRetainedThenExpiresAfterExitAnimation() {
        registry.publish(card("automation:auto_duper", "Completed", StatusState.COMPLETED,
                StatusProgress.finite(20, 20, "Cycle")));
        now.addAndGet(StatusRegistry.COMPLETED_RETENTION.toMillis());

        assertEquals(1, registry.size());

        now.addAndGet(StatusRegistry.EXIT_ANIMATION.toMillis());
        assertEquals(0, registry.size());
    }

    @Test
    void errorUsesLongerRetentionAndRepeatedUpdatesDoNotExtendIt() {
        registry.publish(card("automation:auto_duper", "Error", StatusState.ERROR, StatusProgress.none()));
        long terminalAt = registry.getVisibleStatuses().getFirst().terminalAtMillis();
        now.addAndGet(1_000);
        registry.publish(card("automation:auto_duper", "Error detail updated", StatusState.ERROR, StatusProgress.none()));

        assertEquals(terminalAt, registry.getVisibleStatuses().getFirst().terminalAtMillis());
        now.set(terminalAt + StatusRegistry.ERROR_RETENTION.toMillis()
                + StatusRegistry.EXIT_ANIMATION.toMillis());
        assertEquals(0, registry.size());
    }

    @Test
    void finiteProgressProvidesRealPercentage() {
        StatusProgress progress = StatusProgress.finite(4, 20, "Cycle");

        assertEquals(StatusProgress.Kind.FINITE, progress.kind());
        assertEquals(0.2, progress.percentage().orElseThrow(), 0.0001);
        assertEquals(20, progress.total().orElseThrow());
    }

    @Test
    void countOnlyProgressHasNoTotalOrPercentage() {
        StatusProgress progress = StatusProgress.countOnly(7, "Cycle");

        assertEquals(StatusProgress.Kind.COUNT_ONLY, progress.kind());
        assertTrue(progress.total().isEmpty());
        assertTrue(progress.percentage().isEmpty());
    }

    @Test
    void repeatedUpdatesNeverCreateDuplicateSourceCards() {
        for (int update = 0; update < 50; update++) {
            registry.publish(card("automation:auto_duper", "Step " + update, StatusState.ACTIVE,
                    StatusProgress.countOnly(update, "Cycle")));
        }

        assertEquals(1, registry.size());
        assertEquals("Step 49", registry.getVisibleStatuses().getFirst().data().activity().orElseThrow());
    }

    @Test
    void activeStatusDoesNotExpireAndNewRunGetsFreshEntranceTime() {
        registry.publish(card("automation:auto_duper", "Error", StatusState.ERROR, StatusProgress.none()));
        now.addAndGet(500);
        registry.publish(card("automation:auto_duper", "Restarted", StatusState.ACTIVE, StatusProgress.none()));
        long restartedAt = registry.getVisibleStatuses().getFirst().firstPublishedAtMillis();
        now.addAndGet(StatusRegistry.ERROR_RETENTION.toMillis() * 2);

        assertEquals(1, registry.size());
        assertEquals(restartedAt, registry.getVisibleStatuses().getFirst().firstPublishedAtMillis());
        assertFalse(registry.getVisibleStatuses().getFirst().terminal());
    }

    private StatusCardData card(
            String sourceId,
            String activity,
            StatusState state,
            StatusProgress progress
    ) {
        return StatusCardData.of(sourceId, "Test Source", activity, state, progress);
    }
}

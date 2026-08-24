package com.bstar.qolmod.hud.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Ordered, source-keyed store for generic contextual statuses. */
public final class StatusRegistry {
    public static final Duration COMPLETED_RETENTION = Duration.ofMillis(2_500);
    public static final Duration ERROR_RETENTION = Duration.ofMillis(6_000);
    public static final Duration CANCELLED_RETENTION = Duration.ofMillis(1_800);
    public static final Duration EXIT_ANIMATION = Duration.ofMillis(220);

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final LongSupplier clock;

    public StatusRegistry() {
        this(System::currentTimeMillis);
    }

    StatusRegistry(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized void publish(StatusCardData data) {
        Objects.requireNonNull(data, "data");
        long now = clock.getAsLong();
        Entry existing = entries.get(data.sourceId());
        if (existing == null) {
            entries.put(data.sourceId(), Entry.create(data, now));
            return;
        }

        boolean startingNewRun = existing.data.state().terminal() && !data.state().terminal();
        long firstPublished = startingNewRun ? now : existing.firstPublishedAt;
        long terminalAt = terminalAt(existing, data, now);
        entries.put(data.sourceId(), new Entry(data, firstPublished, now, terminalAt));
    }

    public void publish(
            String sourceId,
            String title,
            String activity,
            StatusState state,
            StatusProgress progress
    ) {
        publish(StatusCardData.of(sourceId, title, activity, state, progress));
    }

    public synchronized void remove(String sourceId) {
        entries.remove(Objects.requireNonNull(sourceId, "sourceId"));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized List<VisibleStatus> getVisibleStatuses() {
        long now = clock.getAsLong();
        List<String> expired = new ArrayList<>();
        List<VisibleStatus> visible = new ArrayList<>();
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            long fadeAt = entry.terminalAt < 0 ? Long.MAX_VALUE : entry.terminalAt + retention(entry.data.state());
            long expiresAt = fadeAt == Long.MAX_VALUE ? Long.MAX_VALUE : fadeAt + EXIT_ANIMATION.toMillis();
            if (now >= expiresAt) {
                expired.add(mapEntry.getKey());
            } else {
                visible.add(new VisibleStatus(
                        entry.data,
                        entry.firstPublishedAt,
                        entry.updatedAt,
                        entry.terminalAt,
                        fadeAt,
                        expiresAt
                ));
            }
        }
        expired.forEach(entries::remove);
        return List.copyOf(visible);
    }

    public synchronized int size() {
        getVisibleStatuses();
        return entries.size();
    }

    private long terminalAt(Entry existing, StatusCardData data, long now) {
        if (!data.state().terminal()) {
            return -1;
        }
        if (existing.data.state() == data.state() && existing.terminalAt >= 0) {
            return existing.terminalAt;
        }
        return now;
    }

    private long retention(StatusState state) {
        return switch (state) {
            case COMPLETED -> COMPLETED_RETENTION.toMillis();
            case ERROR -> ERROR_RETENTION.toMillis();
            case CANCELLED -> CANCELLED_RETENTION.toMillis();
            case ACTIVE, WAITING -> Long.MAX_VALUE;
        };
    }

    private record Entry(StatusCardData data, long firstPublishedAt, long updatedAt, long terminalAt) {
        private static Entry create(StatusCardData data, long now) {
            return new Entry(data, now, now, data.state().terminal() ? now : -1);
        }
    }
}

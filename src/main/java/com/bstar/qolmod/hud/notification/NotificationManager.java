package com.bstar.qolmod.hud.notification;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Source-neutral owner of transient notification identity, lifetime, deduplication, and overflow. */
public final class NotificationManager {
    public static final int MAX_VISIBLE = 5;
    public static final Duration EXIT_ANIMATION = Duration.ofMillis(220);

    private final Map<Long, NotificationEntry> entries = new LinkedHashMap<>();
    private final Map<String, Long> keyedEntries = new LinkedHashMap<>();
    private final LongSupplier clock;
    private long nextInstanceId = 1;

    public NotificationManager() {
        this(System::currentTimeMillis);
    }

    NotificationManager(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public long info(String title, String detail) {
        return notify(NotificationType.INFO, title, detail);
    }

    public long success(String title, String detail) {
        return notify(NotificationType.SUCCESS, title, detail);
    }

    public long warning(String title, String detail) {
        return notify(NotificationType.WARNING, title, detail);
    }

    public long error(String title, String detail) {
        return notify(NotificationType.ERROR, title, detail);
    }

    public long notify(NotificationType type, String title, String detail) {
        return notify(NotificationData.unkeyed(type, title, detail));
    }

    public long notify(String key, NotificationType type, String title, String detail) {
        return notify(NotificationData.keyed(key, type, title, detail));
    }

    public synchronized long notify(NotificationData data) {
        Objects.requireNonNull(data, "data");
        long now = clock.getAsLong();
        pruneExpired(now);

        Optional<String> key = data.key();
        if (key.isPresent()) {
            Long existingId = keyedEntries.get(key.orElseThrow());
            NotificationEntry existing = existingId == null ? null : entries.get(existingId);
            if (existing != null) {
                entries.put(existingId, createEntry(existingId, data, existing.createdAtMillis(), now));
                return existingId;
            }
        }

        makeRoom();
        long instanceId = nextInstanceId++;
        entries.put(instanceId, createEntry(instanceId, data, now, now));
        key.ifPresent(value -> keyedEntries.put(value, instanceId));
        return instanceId;
    }

    public synchronized List<NotificationEntry> getVisibleEntries() {
        pruneExpired(clock.getAsLong());
        return List.copyOf(entries.values());
    }

    public synchronized int size() {
        pruneExpired(clock.getAsLong());
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
        keyedEntries.clear();
    }

    private NotificationEntry createEntry(long id, NotificationData data, long createdAt, long updatedAt) {
        long fadeAt = updatedAt + data.durationMillis();
        return new NotificationEntry(
                id,
                data,
                createdAt,
                updatedAt,
                fadeAt,
                fadeAt + EXIT_ANIMATION.toMillis()
        );
    }

    private void makeRoom() {
        if (entries.size() < MAX_VISIBLE) {
            return;
        }
        Long eviction = entries.entrySet().stream()
                .filter(entry -> entry.getValue().data().type() != NotificationType.ERROR)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(entries.keySet().iterator().next());
        removeEntry(eviction);
    }

    private void pruneExpired(long now) {
        List<Long> expired = new ArrayList<>();
        for (NotificationEntry entry : entries.values()) {
            if (now >= entry.expiresAtMillis()) {
                expired.add(entry.instanceId());
            }
        }
        expired.forEach(this::removeEntry);
    }

    private void removeEntry(long instanceId) {
        NotificationEntry removed = entries.remove(instanceId);
        if (removed != null) {
            removed.data().key().ifPresent(key -> keyedEntries.remove(key, instanceId));
        }
    }
}

package com.bstar.qolmod.hud.notification;

import java.util.Objects;

/** Manager-owned identity and lifetime metadata for a notification request. */
public record NotificationEntry(
        long instanceId,
        NotificationData data,
        long createdAtMillis,
        long updatedAtMillis,
        long fadeAtMillis,
        long expiresAtMillis
) {
    public NotificationEntry {
        if (instanceId <= 0) {
            throw new IllegalArgumentException("Notification instance ID must be positive");
        }
        Objects.requireNonNull(data, "data");
        if (updatedAtMillis < createdAtMillis || fadeAtMillis < updatedAtMillis || expiresAtMillis < fadeAtMillis) {
            throw new IllegalArgumentException("Invalid notification lifetime ordering");
        }
    }
}

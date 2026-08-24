package com.bstar.qolmod.hud.notification;

import java.time.Duration;

/** Stable semantic notification types with restrained default presentation lifetimes. */
public enum NotificationType {
    INFO(Duration.ofSeconds(3)),
    SUCCESS(Duration.ofSeconds(3)),
    WARNING(Duration.ofMillis(4_500)),
    ERROR(Duration.ofSeconds(6));

    private final Duration defaultDuration;

    NotificationType(Duration defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public Duration defaultDuration() {
        return defaultDuration;
    }
}

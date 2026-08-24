package com.bstar.qolmod.hud.notification;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Module-neutral presentation request for one transient notification. */
public record NotificationData(
        Optional<String> key,
        NotificationType type,
        String title,
        Optional<String> detail,
        OptionalLong customDurationMillis
) {
    private static final long MIN_DURATION_MILLIS = 250;
    private static final long MAX_DURATION_MILLIS = 60_000;

    public NotificationData {
        key = clean(key);
        type = Objects.requireNonNull(type, "type");
        title = requireText(title, "title");
        detail = clean(detail);
        customDurationMillis = customDurationMillis == null ? OptionalLong.empty() : customDurationMillis;
        if (customDurationMillis.isPresent()) {
            long duration = customDurationMillis.getAsLong();
            if (duration < MIN_DURATION_MILLIS || duration > MAX_DURATION_MILLIS) {
                throw new IllegalArgumentException("Notification duration must be between "
                        + MIN_DURATION_MILLIS + " and " + MAX_DURATION_MILLIS + " milliseconds");
            }
        }
    }

    public static NotificationData unkeyed(NotificationType type, String title, String detail) {
        return new NotificationData(
                Optional.empty(),
                type,
                title,
                Optional.ofNullable(detail),
                OptionalLong.empty()
        );
    }

    public static NotificationData keyed(String key, NotificationType type, String title, String detail) {
        return new NotificationData(
                Optional.of(requireText(key, "key")),
                type,
                title,
                Optional.ofNullable(detail),
                OptionalLong.empty()
        );
    }

    public NotificationData withDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return new NotificationData(key, type, title, detail, OptionalLong.of(duration.toMillis()));
    }

    public long durationMillis() {
        return customDurationMillis.orElse(type.defaultDuration().toMillis());
    }

    private static Optional<String> clean(Optional<String> value) {
        if (value == null || value.isEmpty() || value.orElseThrow().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.orElseThrow().strip());
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.strip();
    }
}

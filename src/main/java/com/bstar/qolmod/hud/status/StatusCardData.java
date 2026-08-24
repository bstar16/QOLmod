package com.bstar.qolmod.hud.status;

import java.util.Objects;
import java.util.Optional;

/** Feature-neutral data published into the contextual status HUD. */
public record StatusCardData(
        String sourceId,
        String title,
        Optional<String> activity,
        StatusState state,
        StatusProgress progress,
        Optional<String> detail
) {
    public StatusCardData {
        sourceId = requireText(sourceId, "sourceId");
        title = requireText(title, "title");
        activity = clean(activity);
        state = Objects.requireNonNull(state, "state");
        progress = progress == null ? StatusProgress.none() : progress;
        detail = clean(detail);
    }

    public static StatusCardData of(
            String sourceId,
            String title,
            String activity,
            StatusState state,
            StatusProgress progress
    ) {
        return new StatusCardData(
                sourceId,
                title,
                Optional.ofNullable(activity),
                state,
                progress,
                Optional.empty()
        );
    }

    public StatusCardData withDetail(String detail) {
        return new StatusCardData(sourceId, title, activity, state, progress, Optional.ofNullable(detail));
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

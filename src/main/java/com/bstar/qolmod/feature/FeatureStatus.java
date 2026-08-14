package com.bstar.qolmod.feature;

import java.util.Objects;
import java.util.Optional;

public record FeatureStatus(
        FeatureState state,
        String activity,
        Optional<String> detail,
        Optional<Progress> progress
) {
    public FeatureStatus {
        state = Objects.requireNonNull(state, "state");
        activity = activity == null ? "" : activity;
        detail = detail == null ? Optional.empty() : detail;
        progress = progress == null ? Optional.empty() : progress;
    }

    public static FeatureStatus idle() {
        return of(FeatureState.IDLE, "Idle");
    }

    public static FeatureStatus of(FeatureState state, String activity) {
        return new FeatureStatus(state, activity, Optional.empty(), Optional.empty());
    }

    public static FeatureStatus detailed(FeatureState state, String activity, String detail) {
        return new FeatureStatus(state, activity, Optional.ofNullable(detail), Optional.empty());
    }

    public static FeatureStatus progressing(
            FeatureState state,
            String activity,
            String detail,
            long current,
            long max
    ) {
        return new FeatureStatus(
                state,
                activity,
                Optional.ofNullable(detail),
                Optional.of(new Progress(current, max))
        );
    }

    public record Progress(long current, long max) {
        public Progress {
            if (current < 0 || max < 0 || (max > 0 && current > max)) {
                throw new IllegalArgumentException("Invalid feature progress: " + current + "/" + max);
            }
        }
    }
}

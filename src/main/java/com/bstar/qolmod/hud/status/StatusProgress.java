package com.bstar.qolmod.hud.status;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/** Optional progress that never invents a total or percentage. */
public record StatusProgress(Kind kind, long current, OptionalLong total, String label) {
    public enum Kind {
        FINITE,
        INDETERMINATE,
        COUNT_ONLY,
        NONE
    }

    public StatusProgress {
        kind = Objects.requireNonNull(kind, "kind");
        total = total == null ? OptionalLong.empty() : total;
        label = label == null ? "" : label.strip();
        switch (kind) {
            case FINITE -> {
                if (current < 0 || total.isEmpty() || total.getAsLong() <= 0 || current > total.getAsLong()) {
                    throw new IllegalArgumentException("Invalid finite progress: " + current + "/" + total);
                }
            }
            case COUNT_ONLY -> {
                if (current < 0 || total.isPresent()) {
                    throw new IllegalArgumentException("Count-only progress requires a non-negative count and no total");
                }
            }
            case INDETERMINATE, NONE -> {
                if (current != 0 || total.isPresent()) {
                    throw new IllegalArgumentException(kind + " progress cannot contain numeric values");
                }
            }
        }
    }

    public static StatusProgress finite(long current, long total, String label) {
        return new StatusProgress(Kind.FINITE, current, OptionalLong.of(total), label);
    }

    public static StatusProgress indeterminate(String label) {
        return new StatusProgress(Kind.INDETERMINATE, 0, OptionalLong.empty(), label);
    }

    public static StatusProgress countOnly(long current, String label) {
        return new StatusProgress(Kind.COUNT_ONLY, current, OptionalLong.empty(), label);
    }

    public static StatusProgress none() {
        return new StatusProgress(Kind.NONE, 0, OptionalLong.empty(), "");
    }

    public OptionalDouble percentage() {
        return kind == Kind.FINITE
                ? OptionalDouble.of(current / (double) total.orElseThrow())
                : OptionalDouble.empty();
    }

    public boolean visible() {
        return kind != Kind.NONE;
    }
}

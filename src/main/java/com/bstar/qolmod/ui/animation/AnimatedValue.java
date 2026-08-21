package com.bstar.qolmod.ui.animation;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Small time-based interpolation primitive for restrained UI transitions. */
public final class AnimatedValue {
    public enum Easing {
        LINEAR {
            @Override
            double apply(double value) {
                return value;
            }
        },
        CUBIC_OUT {
            @Override
            double apply(double value) {
                double inverse = 1.0 - value;
                return 1.0 - inverse * inverse * inverse;
            }
        },
        SMOOTH_STEP {
            @Override
            double apply(double value) {
                return value * value * (3.0 - 2.0 * value);
            }
        };

        abstract double apply(double value);
    }

    private final long durationNanos;
    private final Easing easing;
    private final LongSupplier clock;
    private double start;
    private double target;
    private long startedAt;

    public AnimatedValue(double initialValue, long durationMillis, Easing easing) {
        this(initialValue, durationMillis, easing, System::nanoTime);
    }

    AnimatedValue(double initialValue, long durationMillis, Easing easing, LongSupplier clock) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        start = initialValue;
        target = initialValue;
        durationNanos = durationMillis * 1_000_000L;
        this.easing = Objects.requireNonNull(easing, "easing");
        this.clock = Objects.requireNonNull(clock, "clock");
        startedAt = clock.getAsLong();
    }

    public double value() {
        long elapsed = Math.max(0L, clock.getAsLong() - startedAt);
        double progress = Math.min(1.0, elapsed / (double) durationNanos);
        return start + (target - start) * easing.apply(progress);
    }

    public void setTarget(double newTarget) {
        if (Double.compare(target, newTarget) == 0) {
            return;
        }
        start = value();
        target = newTarget;
        startedAt = clock.getAsLong();
    }

    public void snap(double value) {
        start = value;
        target = value;
        startedAt = clock.getAsLong();
    }

    public double target() {
        return target;
    }

    public boolean isAtTarget() {
        return Math.abs(value() - target) < 0.001;
    }
}

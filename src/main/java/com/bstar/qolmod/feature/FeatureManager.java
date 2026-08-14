package com.bstar.qolmod.feature;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.event.events.PlayerDeathEvent;
import com.bstar.qolmod.event.events.WorldJoinEvent;
import com.bstar.qolmod.event.events.WorldLeaveEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

/** Owns every feature state transition and guarantees matching lifecycle cleanup. */
public final class FeatureManager {
    private final Map<String, QOLFeature> features = new LinkedHashMap<>();
    private final QOLContext context;
    private final QOLEventBus eventBus;
    private final Logger logger;
    private final List<EventSubscription> subscriptions;

    public FeatureManager(QOLContext context, QOLEventBus eventBus, Logger logger) {
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.logger = Objects.requireNonNull(logger, "logger");
        subscriptions = List.of(
                eventBus.subscribe(ClientTickEvent.class, this::onClientTick),
                eventBus.subscribe(WorldJoinEvent.class, event -> activateConfiguredFeatures()),
                eventBus.subscribe(WorldLeaveEvent.class, event -> resetActiveFeatures(ResetReason.WORLD_LEFT)),
                eventBus.subscribe(PlayerDeathEvent.class, event -> resetActiveFeatures(ResetReason.PLAYER_DIED))
        );
    }

    public void register(QOLFeature feature) {
        Objects.requireNonNull(feature, "feature");
        if (features.containsKey(feature.id())) {
            throw new IllegalArgumentException("Duplicate feature id: " + feature.id());
        }

        feature.attach(this, context, eventBus);
        features.put(feature.id(), feature);
        try {
            feature.invokeRegister();
        } catch (RuntimeException exception) {
            features.remove(feature.id());
            feature.finishRegisteredLifecycle();
            throw new IllegalStateException("Failed to register feature: " + feature.id(), exception);
        }
    }

    public Collection<QOLFeature> all() {
        return Collections.unmodifiableCollection(features.values());
    }

    public Optional<QOLFeature> find(String id) {
        return Optional.ofNullable(features.get(id));
    }

    public <T extends QOLFeature> Optional<T> find(String id, Class<T> featureType) {
        return find(id).filter(featureType::isInstance).map(featureType::cast);
    }

    public void toggle(String id) {
        QOLFeature feature = features.get(id);
        if (feature == null) {
            logger.warn("Tried to toggle unknown feature '{}'.", id);
            return;
        }
        setEnabled(feature, !feature.isEnabled());
    }

    public void setEnabled(QOLFeature feature, boolean enabled) {
        if (enabled) {
            enable(feature);
        } else {
            disable(feature, ResetReason.USER_DISABLED);
        }
    }

    public void enable(QOLFeature feature) {
        requireManaged(feature);
        if (feature.isEnabled()) {
            return;
        }

        feature.setEnabledFromManager(true);
        if (context.isPlayable() && !isPlayerDead()) {
            activate(feature);
        } else {
            feature.setStatusFromManager(FeatureStatus.of(FeatureState.WAITING, "Waiting for a playable world"));
        }
        if (feature.isEnabled()) {
            announce(feature, true);
        }
    }

    public void disable(QOLFeature feature, ResetReason reason) {
        requireManaged(feature);
        Objects.requireNonNull(reason, "reason");
        if (!feature.isEnabled() && !feature.isActive()) {
            return;
        }

        boolean wasEnabled = feature.isEnabled();
        if (feature.isActive()) {
            deactivate(feature, reason, true);
        }
        feature.setEnabledFromManager(false);
        if (feature.status().state() != FeatureState.ERROR
                && feature.status().state() != FeatureState.COMPLETED) {
            feature.setStatusFromManager(FeatureStatus.idle());
        }
        if (wasEnabled) {
            announce(feature, false);
        }
    }

    /** Restores desired state without running in-world hooks during config parsing. */
    public void restoreEnabledState(QOLFeature feature, boolean enabled) {
        requireManaged(feature);
        feature.setEnabledFromManager(enabled);
        feature.setStatusFromManager(enabled
                ? FeatureStatus.of(FeatureState.WAITING, "Waiting for a playable world")
                : FeatureStatus.idle());
    }

    public void disableAll(ResetReason reason) {
        for (QOLFeature feature : List.copyOf(features.values())) {
            if (feature.isEnabled() || feature.isActive()) {
                disable(feature, reason);
            }
        }
    }

    public void shutdown() {
        for (QOLFeature feature : List.copyOf(features.values())) {
            if (feature.isActive()) {
                deactivate(feature, ResetReason.CLIENT_SHUTDOWN, true);
            }
        }
        for (QOLFeature feature : features.values()) {
            feature.finishRegisteredLifecycle();
        }
        for (EventSubscription subscription : subscriptions) {
            subscription.close();
        }
    }

    private void onClientTick(ClientTickEvent event) {
        if (!isPlayerDead() && context.isPlayable()) {
            activateConfiguredFeatures();
        }
    }

    private void activateConfiguredFeatures() {
        if (!context.isPlayable() || isPlayerDead()) {
            return;
        }
        for (QOLFeature feature : features.values()) {
            if (feature.isEnabled() && !feature.isActive()) {
                activate(feature);
            }
        }
    }

    private void activate(QOLFeature feature) {
        try {
            feature.invokeEnable();
            if (feature.status().state() == FeatureState.IDLE || feature.status().state() == FeatureState.WAITING) {
                feature.setStatusFromManager(FeatureStatus.of(FeatureState.RUNNING, "Active"));
            }
        } catch (RuntimeException exception) {
            logger.error("Failed to enable feature '{}'.", feature.id(), exception);
            deactivate(feature, ResetReason.ERROR, true);
            feature.setEnabledFromManager(false);
            feature.setStatusFromManager(FeatureStatus.detailed(FeatureState.ERROR, "Enable failed", exception.getMessage()));
        }
    }

    private void resetActiveFeatures(ResetReason reason) {
        for (QOLFeature feature : List.copyOf(features.values())) {
            if (!feature.isActive()) {
                continue;
            }
            deactivate(feature, reason, false);
            if (feature.isEnabled()) {
                feature.setStatusFromManager(FeatureStatus.of(FeatureState.WAITING, waitingActivity(reason)));
            }
        }
    }

    private void deactivate(QOLFeature feature, ResetReason reason, boolean disabling) {
        try {
            feature.invokeReset(reason);
        } catch (RuntimeException exception) {
            recordLifecycleError(feature, "reset", exception);
        }

        if (disabling) {
            try {
                feature.invokeDisable();
            } catch (RuntimeException exception) {
                recordLifecycleError(feature, "disable", exception);
            }
        }
        feature.finishActiveLifecycle();
    }

    private void recordLifecycleError(QOLFeature feature, String callback, RuntimeException exception) {
        logger.error("Feature '{}' failed during {} lifecycle callback.", feature.id(), callback, exception);
        feature.setStatusFromManager(FeatureStatus.detailed(
                FeatureState.ERROR,
                "Lifecycle error",
                callback + ": " + exception.getMessage()
        ));
    }

    private void announce(QOLFeature feature, boolean enabled) {
        String state = enabled ? "enabled" : "disabled";
        String message = feature.name() + " " + state;
        logger.info(message);
        if (context.player() != null) {
            Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
            context.player().sendMessage(Text.literal("[QOLmod] ").formatted(Formatting.GRAY)
                    .append(Text.literal(message).formatted(color)), false);
        }
    }

    private boolean isPlayerDead() {
        return context.player() != null && context.player().isDead();
    }

    private String waitingActivity(ResetReason reason) {
        return switch (reason) {
            case WORLD_LEFT -> "Waiting for a world";
            case PLAYER_DIED -> "Waiting for respawn";
            default -> "Waiting to resume";
        };
    }

    private void requireManaged(QOLFeature feature) {
        Objects.requireNonNull(feature, "feature");
        if (features.get(feature.id()) != feature) {
            throw new IllegalArgumentException("Feature is not managed by this FeatureManager: " + feature.id());
        }
    }
}

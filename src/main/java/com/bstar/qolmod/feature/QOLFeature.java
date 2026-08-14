package com.bstar.qolmod.feature;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.EventSubscription;
import com.bstar.qolmod.event.QOLEvent;
import com.bstar.qolmod.event.QOLEventBus;
import com.bstar.qolmod.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Base class for feature identity, settings, status, and manager-owned lifecycle. */
public abstract class QOLFeature {
    private final String id;
    private final String name;
    private final String description;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<EventSubscription> activeSubscriptions = new ArrayList<>();
    private final List<EventSubscription> lifetimeSubscriptions = new ArrayList<>();
    private FeatureManager featureManager;
    private QOLContext context;
    private QOLEventBus eventBus;
    private boolean enabled;
    private boolean active;
    private FeatureStatus status = FeatureStatus.idle();

    protected QOLFeature(String id, String name, String description) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNull(description, "description");
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final boolean isActive() {
        return active;
    }

    public final FeatureStatus status() {
        return status;
    }

    public final List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    public final boolean hasSettings() {
        return !settings.isEmpty();
    }

    protected final <T extends Setting<?>> T registerSetting(T setting) {
        Objects.requireNonNull(setting, "setting");
        if (settings.stream().anyMatch(existing -> existing.id().equals(setting.id()))) {
            throw new IllegalArgumentException("Duplicate setting id '" + setting.id() + "' in feature '" + id + "'");
        }
        settings.add(setting);
        return setting;
    }

    protected final QOLContext context() {
        ensureRegistered();
        return context;
    }

    protected final FeatureManager featureManager() {
        ensureRegistered();
        return featureManager;
    }

    protected final <E extends QOLEvent> EventSubscription listen(Class<E> eventType, Consumer<? super E> listener) {
        ensureRegistered();
        if (!active) {
            throw new IllegalStateException("Active event listeners may only be registered during an active feature lifecycle");
        }
        EventSubscription subscription = eventBus.subscribe(eventType, listener);
        activeSubscriptions.add(subscription);
        return subscription;
    }

    protected final <E extends QOLEvent> EventSubscription listenForLifetime(
            Class<E> eventType,
            Consumer<? super E> listener
    ) {
        ensureRegistered();
        EventSubscription subscription = eventBus.subscribe(eventType, listener);
        lifetimeSubscriptions.add(subscription);
        return subscription;
    }

    protected final void updateStatus(FeatureStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    protected void onRegister() {
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    protected void onReset(ResetReason reason) {
    }

    final void attach(FeatureManager featureManager, QOLContext context, QOLEventBus eventBus) {
        if (this.featureManager != null) {
            throw new IllegalStateException("Feature is already registered: " + id);
        }
        this.featureManager = Objects.requireNonNull(featureManager, "featureManager");
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    final void invokeRegister() {
        onRegister();
    }

    final void invokeEnable() {
        active = true;
        onEnable();
    }

    final void invokeReset(ResetReason reason) {
        onReset(reason);
    }

    final void invokeDisable() {
        onDisable();
    }

    final void setEnabledFromManager(boolean enabled) {
        this.enabled = enabled;
    }

    final void setStatusFromManager(FeatureStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    final void finishActiveLifecycle() {
        closeAll(activeSubscriptions);
        active = false;
    }

    final void finishRegisteredLifecycle() {
        finishActiveLifecycle();
        closeAll(lifetimeSubscriptions);
    }

    private void closeAll(List<EventSubscription> subscriptions) {
        for (EventSubscription subscription : List.copyOf(subscriptions)) {
            subscription.close();
        }
        subscriptions.clear();
    }

    private void ensureRegistered() {
        if (featureManager == null || context == null || eventBus == null) {
            throw new IllegalStateException("Feature has not been registered: " + id);
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}

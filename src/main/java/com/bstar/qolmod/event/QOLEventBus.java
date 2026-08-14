package com.bstar.qolmod.event;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;

/** A small typed event bus with insertion-ordered listeners and explicit subscriptions. */
public final class QOLEventBus {
    private final Logger logger;
    private final Map<Class<? extends QOLEvent>, CopyOnWriteArrayList<Listener<? extends QOLEvent>>> listeners =
            new ConcurrentHashMap<>();

    public QOLEventBus(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public <E extends QOLEvent> EventSubscription subscribe(Class<E> eventType, Consumer<? super E> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");

        Listener<E> registration = new Listener<>(eventType, listener);
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        return new Subscription(registration);
    }

    public <E extends QOLEvent> E post(E event) {
        Objects.requireNonNull(event, "event");
        CopyOnWriteArrayList<Listener<? extends QOLEvent>> registrations = listeners.get(event.getClass());
        if (registrations == null) {
            return event;
        }

        for (Listener<? extends QOLEvent> registration : registrations) {
            dispatch(registration, event);
        }
        return event;
    }

    private void dispatch(Listener<? extends QOLEvent> registration, QOLEvent event) {
        try {
            registration.accept(event);
        } catch (RuntimeException exception) {
            logger.error("QOLmod event listener failed while handling {}.", event.getClass().getSimpleName(), exception);
        }
    }

    private record Listener<E extends QOLEvent>(Class<E> eventType, Consumer<? super E> consumer) {
        private void accept(QOLEvent event) {
            consumer.accept(eventType.cast(event));
        }
    }

    private final class Subscription implements EventSubscription {
        private final Listener<? extends QOLEvent> registration;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Subscription(Listener<? extends QOLEvent> registration) {
            this.registration = registration;
        }

        @Override
        public void close() {
            if (!active.compareAndSet(true, false)) {
                return;
            }

            CopyOnWriteArrayList<Listener<? extends QOLEvent>> registrations = listeners.get(registration.eventType());
            if (registrations != null) {
                registrations.remove(registration);
            }
        }

        @Override
        public boolean isActive() {
            return active.get();
        }
    }
}

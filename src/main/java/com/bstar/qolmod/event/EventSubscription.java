package com.bstar.qolmod.event;

/** A registration handle that may be closed repeatedly and safely. */
public interface EventSubscription extends AutoCloseable {
    @Override
    void close();

    boolean isActive();
}

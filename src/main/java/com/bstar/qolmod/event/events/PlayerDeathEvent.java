package com.bstar.qolmod.event.events;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.QOLEvent;

public record PlayerDeathEvent(QOLContext context) implements QOLEvent {
}

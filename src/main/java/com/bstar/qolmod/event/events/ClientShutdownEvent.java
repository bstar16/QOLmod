package com.bstar.qolmod.event.events;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.QOLEvent;

public record ClientShutdownEvent(QOLContext context) implements QOLEvent {
}

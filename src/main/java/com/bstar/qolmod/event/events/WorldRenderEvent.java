package com.bstar.qolmod.event.events;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.QOLEvent;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public record WorldRenderEvent(QOLContext context, WorldRenderContext renderContext) implements QOLEvent {
}

package com.bstar.qolmod.event;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.events.BlockUseEvent;
import com.bstar.qolmod.event.events.ClientShutdownEvent;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.event.events.PlayerDeathEvent;
import com.bstar.qolmod.event.events.WorldJoinEvent;
import com.bstar.qolmod.event.events.WorldLeaveEvent;
import com.bstar.qolmod.event.events.WorldRenderEvent;
import java.util.Objects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;

/** The Fabric backend: callbacks enter here and are translated to internal QOLmod events. */
public final class FabricEventBridge {
    private final QOLContext context;
    private final QOLEventBus eventBus;
    private boolean registered;
    private boolean playerWasDead;

    public FabricEventBridge(QOLContext context, QOLEventBus eventBus) {
        this.context = Objects.requireNonNull(context, "context");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException("FabricEventBridge is already registered");
        }
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean playerDead = context.player() != null && context.player().isDead();
            if (playerDead && !playerWasDead) {
                eventBus.post(new PlayerDeathEvent(context));
            }
            playerWasDead = playerDead;
            eventBus.post(new ClientTickEvent(context));
        });
        WorldRenderEvents.END_MAIN.register(renderContext ->
                eventBus.post(new WorldRenderEvent(context, renderContext)));
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) {
                return ActionResult.PASS;
            }
            return eventBus.post(new BlockUseEvent(context, player, world, hand, hitResult)).result();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            playerWasDead = false;
            eventBus.post(new WorldJoinEvent(context));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            playerWasDead = false;
            eventBus.post(new WorldLeaveEvent(context));
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                eventBus.post(new ClientShutdownEvent(context)));
    }
}

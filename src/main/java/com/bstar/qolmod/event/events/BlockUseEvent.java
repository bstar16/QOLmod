package com.bstar.qolmod.event.events;

import com.bstar.qolmod.core.QOLContext;
import com.bstar.qolmod.event.QOLEvent;
import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public final class BlockUseEvent implements QOLEvent {
    private final QOLContext context;
    private final PlayerEntity player;
    private final World world;
    private final Hand hand;
    private final BlockHitResult hitResult;
    private ActionResult result = ActionResult.PASS;

    public BlockUseEvent(QOLContext context, PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        this.context = Objects.requireNonNull(context, "context");
        this.player = Objects.requireNonNull(player, "player");
        this.world = Objects.requireNonNull(world, "world");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.hitResult = Objects.requireNonNull(hitResult, "hitResult");
    }

    public QOLContext context() {
        return context;
    }

    public PlayerEntity player() {
        return player;
    }

    public World world() {
        return world;
    }

    public Hand hand() {
        return hand;
    }

    public BlockHitResult hitResult() {
        return hitResult;
    }

    public ActionResult result() {
        return result;
    }

    public boolean isHandled() {
        return result != ActionResult.PASS;
    }

    public void setResult(ActionResult result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public void cancel() {
        setResult(ActionResult.FAIL);
    }
}

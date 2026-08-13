package com.bstar.qolmod.render;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.feature.impl.StorageLabelsFeature;
import com.bstar.qolmod.feature.labels.StorageLabel;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class StorageLabelRenderer {
    private static final float VANILLA_NAMEPLATE_SCALE = 0.025F;
    private static final double FACE_OFFSET = 0.62;
    private static final int WRAP_WIDTH = 160;
    private static final int LINE_SPACING = 10;
    private static final int ICON_PIXELS = 16;
    private static final int ICON_GAP = 4;

    private final StorageLabelsFeature feature;
    private long renderCalls;

    public StorageLabelRenderer(StorageLabelsFeature feature) {
        this.feature = feature;
    }

    public void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!feature.isEnabled() || client.world == null || client.player == null) {
            return;
        }

        Camera camera = context.gameRenderer().getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        String dimensionId = feature.dimensionId(client);
        double maxDistanceSquared = feature.maxRenderDistance() * feature.maxRenderDistance();
        int renderedLabels = 0;

        for (StorageLabel label : feature.labelManager().labelsInDimension(dimensionId)) {
            Vec3d renderPos = new Vec3d(
                    label.pos().getX() + 0.5 + FACE_OFFSET * label.face().getOffsetX(),
                    label.pos().getY() + 0.5 + FACE_OFFSET * label.face().getOffsetY() + feature.yOffset(),
                    label.pos().getZ() + 0.5 + FACE_OFFSET * label.face().getOffsetZ()
            );
            double distanceSquared = renderPos.squaredDistanceTo(cameraPos);
            if (distanceSquared > maxDistanceSquared) {
                continue;
            }

            renderLabel(context, camera, cameraPos, renderPos, label);
            renderedLabels++;
        }

        renderCalls++;
        if (renderedLabels > 0 && renderCalls % 600 == 0) {
            QOLmodClient.LOGGER.debug("Rendered {} storage label(s) in {}.", renderedLabels, dimensionId);
        }
    }

    private void renderLabel(WorldRenderContext context, Camera camera, Vec3d cameraPos, Vec3d renderPos, StorageLabel label) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        MatrixStack matrices = context.matrices();
        float scale = (float) (VANILLA_NAMEPLATE_SCALE * feature.textScale());
        int backgroundColor = feature.background() ? backgroundColor(client) : 0;
        TextRenderer.TextLayerType layerType = feature.renderThroughWalls()
                ? TextRenderer.TextLayerType.SEE_THROUGH
                : TextRenderer.TextLayerType.NORMAL;
        List<OrderedText> lines = wrappedLines(textRenderer, label.text());
        int textColor = 0xFF000000 | label.textColor();
        ItemStack iconStack = iconStack(label.iconItemId());
        double x = renderPos.x - cameraPos.x;
        double y = renderPos.y - cameraPos.y;
        double z = renderPos.z - cameraPos.z;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        matrices.scale(scale, -scale, scale);

        int textHeight = Math.max(textRenderer.fontHeight, (lines.size() - 1) * LINE_SPACING + textRenderer.fontHeight);
        int totalHeight = textHeight + (iconStack.isEmpty() ? 0 : ICON_PIXELS + ICON_GAP);
        float top = -totalHeight / 2.0F;
        float lineStartY = top;
        float iconY = top;
        if (!iconStack.isEmpty()) {
            if (label.iconPosition() == StorageLabel.IconPosition.ABOVE) {
                iconY = top;
                lineStartY = top + ICON_PIXELS + ICON_GAP;
            } else {
                lineStartY = top;
                iconY = top + textHeight + ICON_GAP;
            }
        }

        if (!iconStack.isEmpty()) {
            renderIcon(context, client, matrices, iconStack, iconY);
        }

        for (int index = 0; index < lines.size(); index++) {
            OrderedText line = lines.get(index);
            int textWidth = textRenderer.getWidth(line);
            context.commandQueue().submitText(
                    matrices,
                    -textWidth / 2.0F,
                    lineStartY + index * LINE_SPACING,
                    line,
                    false,
                    layerType,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    textColor,
                    backgroundColor,
                    0
            );
        }
        matrices.pop();
    }

    private void renderIcon(WorldRenderContext context, MinecraftClient client, MatrixStack matrices, ItemStack stack, float y) {
        ItemModelManager modelManager = client.getItemModelManager();
        ItemRenderState renderState = new ItemRenderState();
        modelManager.updateForNonLivingEntity(renderState, stack, ItemDisplayContext.GUI, client.player);

        // TODO: Fabric 1.21.11 item render states choose their own item layers; text uses SEE_THROUGH above.
        matrices.push();
        matrices.translate(-ICON_PIXELS / 2.0F, y + ICON_PIXELS, 0.0F);
        matrices.scale(ICON_PIXELS, -ICON_PIXELS, ICON_PIXELS);
        renderState.render(
                matrices,
                context.commandQueue(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                0
        );
        matrices.pop();
    }

    private List<OrderedText> wrappedLines(TextRenderer textRenderer, String text) {
        List<OrderedText> lines = new ArrayList<>();
        String[] rawLines = text.split("\\R", -1);
        for (String rawLine : rawLines) {
            if (rawLine.isBlank()) {
                lines.add(Text.literal(" ").asOrderedText());
                continue;
            }
            lines.addAll(textRenderer.wrapLines(Text.literal(rawLine), WRAP_WIDTH));
        }
        return lines.isEmpty() ? List.of(Text.literal("").asOrderedText()) : lines;
    }

    private ItemStack iconStack(String iconItemId) {
        if (iconItemId == null || iconItemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        Identifier id = Identifier.tryParse(iconItemId);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : item.getDefaultStack();
    }

    private int backgroundColor(MinecraftClient client) {
        int alpha = (int) (client.options.getTextBackgroundOpacity(0.25F) * 255.0F);
        return alpha << 24;
    }
}

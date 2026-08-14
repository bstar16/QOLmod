package com.bstar.qolmod.feature.impl;

import com.bstar.qolmod.command.QOLmodClientCommands;
import com.bstar.qolmod.event.events.BlockUseEvent;
import com.bstar.qolmod.event.events.ClientTickEvent;
import com.bstar.qolmod.event.events.WorldRenderEvent;
import com.bstar.qolmod.feature.FeatureState;
import com.bstar.qolmod.feature.FeatureStatus;
import com.bstar.qolmod.feature.QOLFeature;
import com.bstar.qolmod.feature.ResetReason;
import com.bstar.qolmod.feature.labels.StorageLabel;
import com.bstar.qolmod.feature.labels.StorageLabelManager;
import com.bstar.qolmod.gui.StorageIconPickerScreen;
import com.bstar.qolmod.gui.StorageLabelEditScreen;
import com.bstar.qolmod.render.StorageLabelRenderer;
import com.bstar.qolmod.setting.BooleanSetting;
import com.bstar.qolmod.setting.DoubleSetting;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class StorageLabelsFeature extends QOLFeature {
    private static final double REMOVE_FALLBACK_DISTANCE = 6.0;
    private static final double CLEAR_NEARBY_RADIUS = 16.0;
    private static final double MIN_RENDER_DISTANCE = 8.0;
    private static final double MIN_TEXT_SCALE = 0.5;
    private static final double MAX_TEXT_SCALE = 4.0;
    private static final double MIN_Y_OFFSET = 0.0;
    private static final double MAX_Y_OFFSET = 8.0;

    private final DoubleSetting maxRenderDistance = registerSetting(new DoubleSetting(
            "max-render-distance",
            "Max Render Distance",
            "Maximum distance in blocks to render labels.",
            64.0,
            4.0,
            256.0
    ));
    private final BooleanSetting renderThroughWalls = registerSetting(new BooleanSetting(
            "render-through-walls",
            "Render Through Walls",
            "Draw labels through blocks.",
            false
    ));
    private final DoubleSetting textScale = registerSetting(new DoubleSetting(
            "text-scale",
            "Text Scale",
            "Size of floating storage label text.",
            1.0,
            0.25,
            4.0
    ));
    private final DoubleSetting yOffset = registerSetting(new DoubleSetting(
            "y-offset",
            "Label Height / Y Offset",
            "Height above the selected block where labels render.",
            1.2,
            0.0,
            5.0
    ));
    private final BooleanSetting background = registerSetting(new BooleanSetting(
            "background",
            "Background",
            "Draw a readable background behind label text.",
            true
    ));

    private final StorageLabelManager labelManager = new StorageLabelManager();
    private final StorageLabelRenderer renderer = new StorageLabelRenderer(this);
    private String pendingLabelName;
    private boolean wasEscapePressed;

    public StorageLabelsFeature() {
        super("storage-labels", "Storage Labels", "Client-side floating labels anchored to selected block positions.");
    }

    public void loadLabels() {
        labelManager.load();
    }

    @Override
    protected void onRegister() {
        loadLabels();
        QOLmodClientCommands.register(this);
    }

    @Override
    protected void onEnable() {
        listen(ClientTickEvent.class, event -> tickPlacement(event.context().client()));
        listen(BlockUseEvent.class, this::onBlockUse);
        listen(WorldRenderEvent.class, event -> renderer.render(event.context(), event.renderContext()));
        updateStatus(FeatureStatus.of(FeatureState.RUNNING, "Rendering storage labels"));
    }

    @Override
    protected void onReset(ResetReason reason) {
        cancelPlacement(context().client(), false);
    }

    public void tickPlacement(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        boolean escapePressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_ESCAPE);
        if (pendingLabelName != null && escapePressed && !wasEscapePressed) {
            cancelPlacement(client, true);
        }
        wasEscapePressed = escapePressed;
    }

    public StorageLabelManager labelManager() {
        return labelManager;
    }

    public double maxRenderDistance() {
        return Math.max(MIN_RENDER_DISTANCE, maxRenderDistance.get());
    }

    public boolean renderThroughWalls() {
        return renderThroughWalls.get();
    }

    public double textScale() {
        return clamp(textScale.get(), MIN_TEXT_SCALE, MAX_TEXT_SCALE);
    }

    public double yOffset() {
        return clamp(yOffset.get(), MIN_Y_OFFSET, MAX_Y_OFFSET);
    }

    public boolean background() {
        return background.get();
    }

    public boolean hasPendingPlacement() {
        return pendingLabelName != null;
    }

    public void beginPlacement(MinecraftClient client, String labelName) {
        pendingLabelName = labelName.trim();
        if (!isEnabled()) {
            featureManager().enable(this);
            sendMessage(client, "Storage Labels enabled for placement and rendering.", Formatting.GREEN);
        }
        updateStatus(FeatureStatus.detailed(FeatureState.WAITING, "Right-click a block", pendingLabelName));
        sendMessage(client, "Right-click a block to place storage label: " + pendingLabelName, Formatting.YELLOW);
    }

    public boolean placePendingLabel(MinecraftClient client, BlockPos pos, Direction face) {
        if (pendingLabelName == null || client.world == null) {
            return false;
        }

        String dimensionId = dimensionId(client);
        labelManager.put(dimensionId, pos, pendingLabelName, face);
        sendMessage(client, "Placed storage label \"" + pendingLabelName + "\" in " + dimensionId + " at " + formatPos(pos), Formatting.GREEN);
        pendingLabelName = null;
        updateStatus(FeatureStatus.of(FeatureState.RUNNING, "Rendering storage labels"));
        return true;
    }

    public void cancelPlacement(MinecraftClient client, boolean notify) {
        if (pendingLabelName == null) {
            return;
        }

        String canceled = pendingLabelName;
        pendingLabelName = null;
        if (isEnabled()) {
            updateStatus(FeatureStatus.of(FeatureState.RUNNING, "Rendering storage labels"));
        }
        if (notify) {
            sendMessage(client, "Canceled storage label placement: " + canceled, Formatting.GRAY);
        }
    }

    public void removeLookedAt(MinecraftClient client) {
        if (!hasWorld(client)) {
            return;
        }

        String dimensionId = dimensionId(client);
        Optional<StorageLabel> removed = Optional.empty();
        if (client.crosshairTarget instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            removed = labelManager.removeAt(dimensionId, blockHit.getBlockPos());
        }

        if (removed.isEmpty()) {
            removed = labelManager.nearest(dimensionId, client.player.getEyePos(), REMOVE_FALLBACK_DISTANCE)
                    .flatMap(label -> labelManager.removeAt(dimensionId, label.pos()));
        }

        if (removed.isPresent()) {
            sendMessage(client, "Removed storage label \"" + removed.get().text() + "\".", Formatting.GREEN);
        } else {
            sendMessage(client, "No storage label found on the targeted block or nearby.", Formatting.RED);
        }
    }

    public void listNearby(MinecraftClient client) {
        if (!hasWorld(client)) {
            return;
        }

        String dimensionId = dimensionId(client);
        Vec3d origin = client.player.getEntityPos();
        List<StorageLabel> labels = labelManager.labelsInDimension(dimensionId);
        List<StorageLabel> nearby = labels.stream()
                .sorted(Comparator.comparingDouble(label -> Vec3d.ofCenter(label.pos()).squaredDistanceTo(origin)))
                .limit(20)
                .toList();

        if (nearby.isEmpty()) {
            sendMessage(client, "No storage labels saved in " + dimensionId + ".", Formatting.GRAY);
            return;
        }

        sendMessage(client, "Storage labels in " + dimensionId + " (" + labels.size() + " saved):", Formatting.YELLOW);
        for (StorageLabel label : nearby) {
            sendMessage(client, "- [" + label.id() + "] " + label.alias() + " at " + formatPos(label.pos()) + " - \"" + preview(label) + "\"", Formatting.GRAY);
        }
    }

    public void clearNearby(MinecraftClient client) {
        if (!hasWorld(client)) {
            return;
        }

        int removed = labelManager.clearNearby(dimensionId(client), client.player.getEntityPos(), CLEAR_NEARBY_RADIUS);
        sendMessage(client, "Cleared " + removed + " storage label(s) within " + (int) CLEAR_NEARBY_RADIUS + " blocks.", Formatting.GREEN);
    }

    public void openEditor(MinecraftClient client) {
        findLookedAtOrNearestLabel(client).ifPresent(label ->
                runOnClient(client, () -> client.setScreen(new StorageLabelEditScreen(client.currentScreen, this, label, false))));
    }

    public void openEditor(MinecraftClient client, String labelName, boolean focusColor) {
        findNamedLabel(client, labelName).ifPresent(label ->
                runOnClient(client, () -> client.setScreen(new StorageLabelEditScreen(client.currentScreen, this, label, focusColor))));
    }

    public void openIconPicker(MinecraftClient client, String labelName) {
        openEditor(client, labelName, false);
    }

    public void setColor(MinecraftClient client, String labelName, String colorValue) {
        Optional<Integer> parsedColor = parseColor(colorValue);
        if (parsedColor.isEmpty()) {
            sendMessage(client, "Unknown color \"" + colorValue + "\". Use a named color or hex like #FF8800.", Formatting.RED);
            return;
        }

        findNamedLabel(client, labelName).ifPresent(label -> {
            labelManager.update(label, label.withTextColorIcon(label.text(), parsedColor.get(), label.iconItemId()));
            sendMessage(client, "Set storage label \"" + label.text() + "\" color to " + formatColor(parsedColor.get()) + ".", Formatting.GREEN);
        });
    }

    public void updateLabel(
            StorageLabel original,
            List<String> lines,
            String alias,
            int textColor,
            String iconItemId,
            StorageLabel.IconPosition iconPosition
    ) {
        labelManager.update(original, original.withLinesAliasColorIconPosition(lines, alias, textColor, iconItemId, iconPosition));
    }

    public void setIcon(StorageLabel original, String iconItemId) {
        labelManager.update(original, original.withTextColorIcon(original.text(), original.textColor(), iconItemId));
    }

    public List<String> labelNamesInCurrentDimension(MinecraftClient client) {
        if (!hasWorld(client)) {
            return List.of();
        }

        return labelManager.labelsInDimension(dimensionId(client)).stream()
                .flatMap(label -> List.of(label.id(), label.alias(), label.firstLine()).stream())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    public void debug(MinecraftClient client) {
        if (!hasWorld(client)) {
            sendMessage(client, "Storage label debug is only available in-world.", Formatting.RED);
            return;
        }

        String dimensionId = dimensionId(client);
        Vec3d origin = client.player.getEntityPos();
        List<StorageLabel> labelsInDimension = labelManager.labelsInDimension(dimensionId);
        Optional<StorageLabel> nearest = labelsInDimension.stream()
                .min(Comparator.comparingDouble(label -> Vec3d.ofCenter(label.pos()).squaredDistanceTo(origin)));

        sendMessage(client, "Storage Labels debug:", Formatting.YELLOW);
        sendMessage(client, "- enabled: " + isEnabled(), Formatting.GRAY);
        sendMessage(client, "- current dimension: " + dimensionId, Formatting.GRAY);
        sendMessage(client, "- total saved labels: " + labelManager.totalLabels(), Formatting.GRAY);
        sendMessage(client, "- labels in current dimension: " + labelsInDimension.size(), Formatting.GRAY);
        sendMessage(client, "- nearest label: " + nearest
                .map(label -> "[" + label.id() + "] \"" + label.alias() + "\" at " + formatPos(label.pos()))
                .orElse("none"), Formatting.GRAY);
        sendMessage(client, "- max render distance: " + maxRenderDistance(), Formatting.GRAY);
        sendMessage(client, "- text scale: " + textScale(), Formatting.GRAY);
        sendMessage(client, "- y offset: " + yOffset(), Formatting.GRAY);
        sendMessage(client, "- render through walls: " + renderThroughWalls(), Formatting.GRAY);
        sendMessage(client, "- background: " + background(), Formatting.GRAY);
    }

    public String dimensionId(MinecraftClient client) {
        return StorageLabelManager.normalizeDimensionId(client.world.getRegistryKey().getValue().toString());
    }

    private boolean hasWorld(MinecraftClient client) {
        return client != null && client.player != null && client.world != null;
    }

    private void onBlockUse(BlockUseEvent event) {
        if (!event.world().isClient() || !hasPendingPlacement()) {
            return;
        }

        if (placePendingLabel(context().client(), event.hitResult().getBlockPos(), event.hitResult().getSide())) {
            event.cancel();
        }
    }

    private Optional<StorageLabel> findNamedLabel(MinecraftClient client, String labelName) {
        if (!hasWorld(client)) {
            sendMessage(client, "Storage label editing is only available in-world.", Formatting.RED);
            return Optional.empty();
        }

        String trimmedName = labelName == null ? "" : labelName.trim();
        if (trimmedName.isEmpty()) {
            sendMessage(client, "Storage label name cannot be empty.", Formatting.RED);
            return Optional.empty();
        }

        Optional<StorageLabel> label = labelManager.findNearestByIdentifier(dimensionId(client), trimmedName, client.player.getEntityPos());
        if (label.isEmpty()) {
            sendMessage(client, "No storage label found for \"" + trimmedName + "\".", Formatting.RED);
            List<StorageLabel> nearby = labelManager.labelsInDimension(dimensionId(client)).stream()
                    .sorted(Comparator.comparingDouble(candidate -> Vec3d.ofCenter(candidate.pos()).squaredDistanceTo(client.player.getEntityPos())))
                    .limit(5)
                    .toList();
            if (!nearby.isEmpty()) {
                sendMessage(client, "Nearby labels: " + nearby.stream().map(labelItem -> "[" + labelItem.id() + "] " + labelItem.alias()).distinct().limit(5).toList(), Formatting.GRAY);
            }
            sendMessage(client, "Use /qol storage list or look at a label/block and run /qol storage edit.", Formatting.GRAY);
        }
        return label;
    }

    private Optional<StorageLabel> findLookedAtOrNearestLabel(MinecraftClient client) {
        if (!hasWorld(client)) {
            sendMessage(client, "Storage label editing is only available in-world.", Formatting.RED);
            return Optional.empty();
        }

        String dimensionId = dimensionId(client);
        if (client.crosshairTarget instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            Optional<StorageLabel> labelAtBlock = labelManager.findAt(dimensionId, blockHit.getBlockPos());
            if (labelAtBlock.isPresent()) {
                return labelAtBlock;
            }
        }

        Optional<StorageLabel> nearest = labelManager.nearest(dimensionId, client.player.getEyePos(), REMOVE_FALLBACK_DISTANCE);
        if (nearest.isEmpty()) {
            sendMessage(client, "No storage label found. Use /qol storage list or look at a label/block and run /qol storage edit.", Formatting.RED);
        }
        return nearest;
    }

    private String preview(StorageLabel label) {
        String preview = String.join(" / ", label.lines());
        return preview.length() <= 60 ? preview : preview.substring(0, 57) + "...";
    }

    private Optional<Integer> parseColor(String colorValue) {
        if (colorValue == null) {
            return Optional.empty();
        }

        String normalized = colorValue.trim().toLowerCase(Locale.ROOT);
        int namedColor = switch (normalized) {
            case "white" -> 0xFFFFFF;
            case "yellow" -> 0xFFFF55;
            case "aqua" -> 0x55FFFF;
            case "green" -> 0x55FF55;
            case "red" -> 0xFF5555;
            case "purple" -> 0xAA00AA;
            case "gold" -> 0xFFAA00;
            case "gray", "grey" -> 0xAAAAAA;
            case "blue" -> 0x5555FF;
            default -> -1;
        };
        if (namedColor >= 0) {
            return Optional.of(namedColor);
        }

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }

        if (!normalized.matches("[0-9a-f]{6}")) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(normalized, 16) & 0xFFFFFF);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private void runOnClient(MinecraftClient client, Runnable runnable) {
        if (client == null) {
            return;
        }

        client.execute(runnable);
    }

    private String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sendMessage(MinecraftClient client, String message, Formatting formatting) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[QOLmod] ").formatted(Formatting.GRAY)
                    .append(Text.literal(message).formatted(formatting)), false);
        }
    }
}

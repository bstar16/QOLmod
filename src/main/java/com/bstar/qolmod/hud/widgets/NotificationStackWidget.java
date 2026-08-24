package com.bstar.qolmod.hud.widgets;

import com.bstar.qolmod.hud.HudContext;
import com.bstar.qolmod.hud.HudPlacement;
import com.bstar.qolmod.hud.HudPosition;
import com.bstar.qolmod.hud.HudWidgetConfig;
import com.bstar.qolmod.hud.editor.EditableHudWidget;
import com.bstar.qolmod.hud.editor.HudEditorMetadata;
import com.bstar.qolmod.hud.editor.HudGeometry;
import com.bstar.qolmod.hud.editor.HudRect;
import com.bstar.qolmod.hud.editor.HudSize;
import com.bstar.qolmod.hud.notification.NotificationData;
import com.bstar.qolmod.hud.notification.NotificationEntry;
import com.bstar.qolmod.hud.notification.NotificationManager;
import com.bstar.qolmod.hud.notification.NotificationType;
import com.bstar.qolmod.hud.render.GlassHudSurface;
import com.bstar.qolmod.hud.render.HudSurface;
import com.bstar.qolmod.ui.animation.AnimatedValue;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Feature-neutral transient notification renderer with stable stack reflow. */
public final class NotificationStackWidget implements EditableHudWidget {
    private static final int CARD_WIDTH = 184;
    private static final int CARD_GAP = 5;
    private static final int HORIZONTAL_PADDING = 10;
    private static final int TYPE_GAP = 8;
    private static final long ENTRANCE_MILLIS = 200;
    private static final long REFLOW_MILLIS = 180;
    private static final List<NotificationData> EDITOR_PREVIEW = List.of(
            NotificationData.unkeyed(NotificationType.SUCCESS, "Auto Duper", "Completed 20 cycles"),
            NotificationData.unkeyed(
                    NotificationType.WARNING,
                    "Storage Labels",
                    "This example warning has longer detail text so wrapping and stack spacing are easy to judge."
            )
    );

    private final NotificationManager manager;
    private final HudWidgetConfig config;
    private final HudSurface surface;
    private final Map<Long, AnimatedValue> verticalPositions = new HashMap<>();
    private final HudEditorMetadata editorMetadata;

    public NotificationStackWidget(NotificationManager manager, HudWidgetConfig config) {
        this(manager, config, new GlassHudSurface());
    }

    NotificationStackWidget(NotificationManager manager, HudWidgetConfig config, HudSurface surface) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.config = Objects.requireNonNull(config, "config");
        this.surface = Objects.requireNonNull(surface, "surface");
        editorMetadata = new HudEditorMetadata(id(), "Notifications", "notifications", config);
    }

    @Override
    public String id() {
        return "notifications";
    }

    @Override
    public HudWidgetConfig config() {
        return config;
    }

    @Override
    public HudEditorMetadata editorMetadata() {
        return editorMetadata;
    }

    @Override
    public HudSize previewSize(net.minecraft.client.MinecraftClient client) {
        return new HudSize(CARD_WIDTH, previewLayout(client.textRenderer).totalHeight());
    }

    @Override
    public void renderPreview(DrawContext context, net.minecraft.client.MinecraftClient client, double opacity) {
        List<MeasuredData> measured = measureData(client.textRenderer, EDITOR_PREVIEW);
        NotificationStackLayout stack = NotificationStackLayout.of(
                measured.stream().map(item -> item.layout().height()).toList(),
                CARD_GAP
        );
        for (int index = 0; index < measured.size(); index++) {
            MeasuredData item = measured.get(index);
            renderCard(
                    context,
                    client.textRenderer,
                    item.data(),
                    item.layout(),
                    0,
                    stack.offsets().get(index),
                    opacity
            );
        }
    }

    @Override
    public void render(HudContext hudContext) {
        if (!config.enabled()) {
            return;
        }
        List<NotificationEntry> entries = manager.getVisibleEntries();
        if (entries.isEmpty()) {
            verticalPositions.clear();
            return;
        }

        TextRenderer textRenderer = hudContext.client().textRenderer;
        List<MeasuredNotification> measured = measureEntries(textRenderer, entries);
        NotificationStackLayout stackLayout = NotificationStackLayout.of(
                measured.stream().map(item -> item.layout().height()).toList(),
                CARD_GAP
        );
        HudPosition position = config.position();
        HudRect placement;
        if (position.equals(config.defaultPosition())) {
            HudPlacement allocated = hudContext.layout().place(
                    position,
                    CARD_WIDTH,
                    stackLayout.totalHeight(),
                    8
            );
            placement = HudGeometry.clampRect(
                    allocated.x(),
                    allocated.y(),
                    (int) Math.ceil(CARD_WIDTH * position.scale()),
                    (int) Math.ceil(stackLayout.totalHeight() * position.scale()),
                    hudContext.screenWidth(),
                    hudContext.screenHeight(),
                    4
            );
        } else {
            placement = HudGeometry.clampedScreenRect(
                    position,
                    new HudSize(CARD_WIDTH, stackLayout.totalHeight()),
                    hudContext.screenWidth(),
                    hudContext.screenHeight(),
                    4
            );
        }
        DrawContext context = hudContext.drawContext();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(placement.x(), placement.y());
        context.getMatrices().scale((float) position.scale(), (float) position.scale());
        try {
            renderEntries(context, textRenderer, measured, stackLayout, position.opacity());
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private void renderEntries(
            DrawContext context,
            TextRenderer textRenderer,
            List<MeasuredNotification> measured,
            NotificationStackLayout stackLayout,
            double configuredOpacity
    ) {
        long now = System.currentTimeMillis();
        Set<Long> visibleIds = new HashSet<>();
        for (int index = 0; index < measured.size(); index++) {
            MeasuredNotification item = measured.get(index);
            NotificationEntry entry = item.entry();
            visibleIds.add(entry.instanceId());
            double targetY = stackLayout.offsets().get(index);
            AnimatedValue y = verticalPositions.computeIfAbsent(
                    entry.instanceId(),
                    ignored -> new AnimatedValue(targetY, REFLOW_MILLIS, AnimatedValue.Easing.SMOOTH_STEP)
            );
            if (targetY > y.value()) {
                // Growing cards move following entries away immediately so text surfaces never overlap.
                y.snap(targetY);
            } else {
                y.setTarget(targetY);
            }
            double alpha = configuredOpacity * lifecycleAlpha(entry, now);
            int slide = (int) Math.round(slideOffset(entry, now));
            renderCard(
                    context,
                    textRenderer,
                    entry.data(),
                    item.layout(),
                    slide,
                    (int) Math.round(y.value()),
                    alpha
            );
        }
        verticalPositions.keySet().removeIf(id -> !visibleIds.contains(id));
    }

    private void renderCard(
            DrawContext context,
            TextRenderer renderer,
            NotificationData data,
            NotificationCardLayout layout,
            int x,
            int y,
            double opacity
    ) {
        ColorPalette colors = ThemeManager.active().colors();
        int accent = typeColor(data.type(), colors);
        surface.draw(context, x, y, CARD_WIDTH, layout.height(), opacity, accent);
        context.fill(x + 4, y + 1, x + 29, y + 2, color(accent, opacity));

        String type = data.type().name();
        int typeWidth = renderer.getWidth(type);
        context.drawText(renderer, layout.title(),
                x + HORIZONTAL_PADDING, y + layout.titleY(), color(colors.primaryText(), opacity), true);
        context.drawText(renderer, type, x + CARD_WIDTH - typeWidth - 8, y + layout.titleY(),
                color(accent, opacity), false);
        int detailColor = color(
                data.type() == NotificationType.ERROR ? colors.secondaryText() : colors.mutedText(),
                opacity
        );
        for (int index = 0; index < layout.detailLines().size(); index++) {
            context.drawText(
                    renderer,
                    layout.detailLines().get(index),
                    x + HORIZONTAL_PADDING,
                    y + layout.detailY() + index * layout.lineHeight(),
                    detailColor,
                    false
            );
        }
    }

    private List<MeasuredNotification> measureEntries(
            TextRenderer renderer,
            List<NotificationEntry> entries
    ) {
        NotificationTextLayout.TextMeasurer measurer = new NotificationTextLayout.TextMeasurer() {
            @Override
            public int width(String text) {
                return renderer.getWidth(text);
            }

            @Override
            public String trimToWidth(String text, int width) {
                return renderer.trimToWidth(text, width);
            }
        };
        return entries.stream().map(entry -> {
            int typeWidth = renderer.getWidth(entry.data().type().name());
            int titleWidth = Math.max(1,
                    CARD_WIDTH - HORIZONTAL_PADDING * 2 - typeWidth - TYPE_GAP);
            NotificationCardLayout layout = NotificationTextLayout.measure(
                    entry.data().title(),
                    entry.data().detail().orElse(null),
                    measurer,
                    titleWidth,
                    CARD_WIDTH - HORIZONTAL_PADDING * 2,
                    renderer.fontHeight
            );
            return new MeasuredNotification(entry, layout);
        }).toList();
    }

    private NotificationStackLayout previewLayout(TextRenderer renderer) {
        List<MeasuredData> measured = measureData(renderer, EDITOR_PREVIEW);
        return NotificationStackLayout.of(
                measured.stream().map(item -> item.layout().height()).toList(),
                CARD_GAP
        );
    }

    private List<MeasuredData> measureData(TextRenderer renderer, List<NotificationData> entries) {
        NotificationTextLayout.TextMeasurer measurer = new NotificationTextLayout.TextMeasurer() {
            @Override
            public int width(String text) {
                return renderer.getWidth(text);
            }

            @Override
            public String trimToWidth(String text, int width) {
                return renderer.trimToWidth(text, width);
            }
        };
        return entries.stream().map(data -> {
            int typeWidth = renderer.getWidth(data.type().name());
            int titleWidth = Math.max(1,
                    CARD_WIDTH - HORIZONTAL_PADDING * 2 - typeWidth - TYPE_GAP);
            NotificationCardLayout layout = NotificationTextLayout.measure(
                    data.title(),
                    data.detail().orElse(null),
                    measurer,
                    titleWidth,
                    CARD_WIDTH - HORIZONTAL_PADDING * 2,
                    renderer.fontHeight
            );
            return new MeasuredData(data, layout);
        }).toList();
    }

    private double lifecycleAlpha(NotificationEntry entry, long now) {
        double entrance = clamp((now - entry.createdAtMillis()) / (double) ENTRANCE_MILLIS);
        if (now <= entry.fadeAtMillis()) {
            return entrance;
        }
        return Math.min(entrance, clamp((entry.expiresAtMillis() - now)
                / (double) NotificationManager.EXIT_ANIMATION.toMillis()));
    }

    private double slideOffset(NotificationEntry entry, long now) {
        double entrance = clamp((now - entry.createdAtMillis()) / (double) ENTRANCE_MILLIS);
        if (now > entry.fadeAtMillis()) {
            double exit = 1.0 - clamp((entry.expiresAtMillis() - now)
                    / (double) NotificationManager.EXIT_ANIMATION.toMillis());
            return 8.0 * exit;
        }
        return 10.0 * (1.0 - entrance);
    }

    private int typeColor(NotificationType type, ColorPalette colors) {
        return switch (type) {
            case INFO -> colors.accent();
            case SUCCESS -> colors.success();
            case WARNING -> colors.waiting();
            case ERROR -> colors.error();
        };
    }

    private int color(int color, double opacity) {
        int alpha = (color >>> 24) & 0xFF;
        return ((int) Math.round(alpha * clamp(opacity)) << 24) | (color & 0x00FFFFFF);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record MeasuredNotification(NotificationEntry entry, NotificationCardLayout layout) {
    }

    private record MeasuredData(NotificationData data, NotificationCardLayout layout) {
    }
}

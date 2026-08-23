package com.bstar.qolmod.hud.widgets;

import com.bstar.qolmod.hud.HudAnchor;
import com.bstar.qolmod.hud.HudContext;
import com.bstar.qolmod.hud.HudPosition;
import com.bstar.qolmod.hud.HudWidget;
import com.bstar.qolmod.hud.HudWidgetConfig;
import com.bstar.qolmod.hud.render.GlassHudSurface;
import com.bstar.qolmod.hud.render.HudSurface;
import com.bstar.qolmod.hud.status.StatusCardData;
import com.bstar.qolmod.hud.status.StatusProgress;
import com.bstar.qolmod.hud.status.StatusRegistry;
import com.bstar.qolmod.hud.status.StatusState;
import com.bstar.qolmod.hud.status.VisibleStatus;
import com.bstar.qolmod.ui.theme.ColorPalette;
import com.bstar.qolmod.ui.theme.ThemeManager;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Feature-neutral renderer for an ordered vertical stack of contextual status cards. */
public final class StatusStackWidget implements HudWidget {
    private static final int CARD_WIDTH = 184;
    private static final int CARD_HEIGHT = 54;
    private static final int CARD_GAP = 6;
    private static final long ENTRANCE_MILLIS = 180;

    private final StatusRegistry registry;
    private final HudWidgetConfig config;
    private final HudSurface surface;

    public StatusStackWidget(StatusRegistry registry, HudWidgetConfig config) {
        this(registry, config, new GlassHudSurface());
    }

    StatusStackWidget(StatusRegistry registry, HudWidgetConfig config, HudSurface surface) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.config = Objects.requireNonNull(config, "config");
        this.surface = Objects.requireNonNull(surface, "surface");
    }

    @Override
    public String id() {
        return "contextual-status";
    }

    @Override
    public HudWidgetConfig config() {
        return config;
    }

    @Override
    public void render(HudContext hudContext) {
        if (!config.enabled()) {
            return;
        }
        List<VisibleStatus> statuses = registry.getVisibleStatuses();
        if (statuses.isEmpty()) {
            return;
        }

        HudPosition position = config.position();
        double scale = position.scale();
        int stackHeight = statuses.size() * CARD_HEIGHT + (statuses.size() - 1) * CARD_GAP;
        int originX = horizontalOrigin(position, hudContext.screenWidth(), scale);
        int originY = verticalOrigin(position, hudContext.screenHeight(), stackHeight, scale);
        DrawContext context = hudContext.drawContext();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(originX, originY);
        context.getMatrices().scale((float) scale, (float) scale);
        try {
            long now = System.currentTimeMillis();
            for (int index = 0; index < statuses.size(); index++) {
                VisibleStatus status = statuses.get(index);
                double lifecycleAlpha = lifecycleAlpha(status, now);
                double opacity = position.opacity() * lifecycleAlpha;
                int slide = (int) Math.round(slideOffset(status, now));
                renderCard(context, hudContext.client().textRenderer, status.data(), slide,
                        index * (CARD_HEIGHT + CARD_GAP), opacity);
            }
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private void renderCard(
            DrawContext context,
            TextRenderer textRenderer,
            StatusCardData data,
            int x,
            int y,
            double opacity
    ) {
        ColorPalette colors = ThemeManager.active().colors();
        int stateColor = stateColor(data.state(), colors);
        surface.draw(context, x, y, CARD_WIDTH, CARD_HEIGHT, opacity, stateColor);

        String state = stateLabel(data.state());
        int stateWidth = textRenderer.getWidth(state);
        context.drawText(textRenderer, trim(textRenderer, data.title(), CARD_WIDTH - stateWidth - 27),
                x + 10, y + 7, color(colors.primaryText(), opacity), true);
        context.drawText(textRenderer, state, x + CARD_WIDTH - stateWidth - 8, y + 7,
                color(stateColor, opacity), false);

        data.activity().ifPresent(activity -> context.drawText(
                textRenderer,
                trim(textRenderer, activity, CARD_WIDTH - 20),
                x + 10,
                y + 21,
                color(colors.secondaryText(), opacity),
                false
        ));

        if (data.progress().visible()) {
            renderProgress(context, textRenderer, data.progress(), x, y, opacity, colors);
        } else {
            data.detail().ifPresent(detail -> context.drawText(
                    textRenderer,
                    trim(textRenderer, detail, CARD_WIDTH - 20),
                    x + 10,
                    y + 38,
                    color(data.state() == StatusState.ERROR ? colors.error() : colors.mutedText(), opacity),
                    false
            ));
        }
    }

    private void renderProgress(
            DrawContext context,
            TextRenderer renderer,
            StatusProgress progress,
            int x,
            int y,
            double opacity,
            ColorPalette colors
    ) {
        String text = progressText(progress);
        context.drawText(renderer, trim(renderer, text, CARD_WIDTH - 20), x + 10, y + 37,
                color(colors.mutedText(), opacity), false);
        if (progress.kind() == StatusProgress.Kind.FINITE) {
            int barLeft = x + 10;
            int barRight = x + CARD_WIDTH - 10;
            int barY = y + CARD_HEIGHT - 5;
            context.fill(barLeft, barY, barRight, barY + 1, color(colors.subtleDivider(), opacity));
            int filled = (int) Math.round((barRight - barLeft) * progress.percentage().orElseThrow());
            context.fill(barLeft, barY, barLeft + filled, barY + 1, color(colors.accent(), opacity));
        }
    }

    private String progressText(StatusProgress progress) {
        return switch (progress.kind()) {
            case FINITE -> prefix(progress.label()) + progress.current() + " / " + progress.total().orElseThrow();
            case COUNT_ONLY -> prefix(progress.label()) + progress.current();
            case INDETERMINATE -> progress.label().isBlank() ? "Working..." : progress.label();
            case NONE -> "";
        };
    }

    private String prefix(String label) {
        return label.isBlank() ? "" : label + " ";
    }

    private int horizontalOrigin(HudPosition position, int screenWidth, double scale) {
        return switch (position.anchor()) {
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - position.xOffset() - (int) Math.ceil(CARD_WIDTH * scale);
            case TOP_LEFT, BOTTOM_LEFT -> position.xOffset();
        };
    }

    private int verticalOrigin(HudPosition position, int screenHeight, int stackHeight, double scale) {
        return switch (position.anchor()) {
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - position.yOffset() - (int) Math.ceil(stackHeight * scale);
            case TOP_LEFT, TOP_RIGHT -> position.yOffset();
        };
    }

    private double lifecycleAlpha(VisibleStatus status, long now) {
        double entrance = clamp((now - status.firstPublishedAtMillis()) / (double) ENTRANCE_MILLIS);
        if (!status.terminal() || now <= status.fadeAtMillis()) {
            return entrance;
        }
        return Math.min(entrance, clamp((status.expiresAtMillis() - now)
                / (double) StatusRegistry.EXIT_ANIMATION.toMillis()));
    }

    private double slideOffset(VisibleStatus status, long now) {
        double entrance = clamp((now - status.firstPublishedAtMillis()) / (double) ENTRANCE_MILLIS);
        if (status.terminal() && now > status.fadeAtMillis()) {
            double exit = 1.0 - clamp((status.expiresAtMillis() - now)
                    / (double) StatusRegistry.EXIT_ANIMATION.toMillis());
            return 6.0 * exit;
        }
        return 8.0 * (1.0 - entrance);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String stateLabel(StatusState state) {
        return switch (state) {
            case ACTIVE -> "ACTIVE";
            case WAITING -> "WAIT";
            case COMPLETED -> "DONE";
            case ERROR -> "ERROR";
            case CANCELLED -> "CANCELLED";
        };
    }

    private int stateColor(StatusState state, ColorPalette colors) {
        return switch (state) {
            case ACTIVE -> colors.active();
            case WAITING -> colors.waiting();
            case COMPLETED -> colors.completed();
            case ERROR, CANCELLED -> colors.error();
        };
    }

    private String trim(TextRenderer renderer, String value, int maxWidth) {
        if (renderer.getWidth(value) <= maxWidth) {
            return value;
        }
        return renderer.trimToWidth(value, Math.max(0, maxWidth - renderer.getWidth("..."))) + "...";
    }

    private int color(int color, double opacity) {
        int alpha = (color >>> 24) & 0xFF;
        return ((int) Math.round(alpha * clamp(opacity)) << 24) | (color & 0x00FFFFFF);
    }
}

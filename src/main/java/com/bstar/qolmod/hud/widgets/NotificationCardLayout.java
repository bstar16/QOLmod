package com.bstar.qolmod.hud.widgets;

import java.util.List;
import java.util.Objects;

/** Measured logical-space text and dimensions for one notification card. */
record NotificationCardLayout(
        String title,
        List<String> detailLines,
        int height,
        int titleY,
        int detailY,
        int lineHeight
) {
    NotificationCardLayout {
        title = Objects.requireNonNull(title, "title");
        detailLines = List.copyOf(detailLines);
        if (height <= 0 || titleY < 0 || detailY < titleY || lineHeight <= 0) {
            throw new IllegalArgumentException("Invalid notification card layout");
        }
    }
}

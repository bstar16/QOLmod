package com.bstar.qolmod.hud.widgets;

import java.util.ArrayList;
import java.util.List;

/** Computes mixed-height notification offsets in logical widget coordinates. */
record NotificationStackLayout(List<Integer> offsets, int totalHeight) {
    NotificationStackLayout {
        offsets = List.copyOf(offsets);
        if (totalHeight < 0) {
            throw new IllegalArgumentException("Notification stack height cannot be negative");
        }
    }

    static NotificationStackLayout of(List<Integer> heights, int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("Notification gap cannot be negative");
        }
        List<Integer> offsets = new ArrayList<>(heights.size());
        int cursor = 0;
        for (int index = 0; index < heights.size(); index++) {
            int height = heights.get(index);
            if (height <= 0) {
                throw new IllegalArgumentException("Notification card height must be positive");
            }
            offsets.add(cursor);
            cursor += height;
            if (index < heights.size() - 1) {
                cursor += gap;
            }
        }
        return new NotificationStackLayout(offsets, cursor);
    }
}

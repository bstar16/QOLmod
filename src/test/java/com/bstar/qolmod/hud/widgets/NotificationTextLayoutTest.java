package com.bstar.qolmod.hud.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTextLayoutTest {
    private static final NotificationTextLayout.TextMeasurer MONOSPACE =
            new NotificationTextLayout.TextMeasurer() {
                @Override
                public int width(String text) {
                    return text.length();
                }

                @Override
                public String trimToWidth(String text, int width) {
                    return text.substring(0, Math.min(text.length(), Math.max(0, width)));
                }
            };

    @Test
    void shortNotificationRemainsCompact() {
        NotificationCardLayout layout = NotificationTextLayout.measure(
                "Auto Duper", "Completed 20 cycles", MONOSPACE, 30, 30, 9
        );

        assertEquals(NotificationTextLayout.MIN_CARD_HEIGHT, layout.height());
        assertEquals(List.of("Completed 20 cycles"), layout.detailLines());
    }

    @Test
    void longDetailWrapsOnWordsAndGrowsCard() {
        NotificationCardLayout layout = NotificationTextLayout.measure(
                "Storage Labels",
                "The container this label was attached to could no longer be found in the world.",
                MONOSPACE,
                30,
                25,
                9
        );

        assertTrue(layout.detailLines().size() > 1);
        assertTrue(layout.height() > NotificationTextLayout.MIN_CARD_HEIGHT);
        assertTrue(layout.detailLines().stream().allMatch(line -> MONOSPACE.width(line) <= 25));
        assertEquals("The container this label", layout.detailLines().getFirst());
    }

    @Test
    void veryLongTitleIsContainedWithEllipsis() {
        NotificationCardLayout layout = NotificationTextLayout.measure(
                "An exceptionally long notification title that cannot fit",
                null,
                MONOSPACE,
                18,
                30,
                9
        );

        assertTrue(layout.title().endsWith("..."));
        assertTrue(MONOSPACE.width(layout.title()) <= 18);
        assertEquals(NotificationTextLayout.MIN_CARD_HEIGHT, layout.height());
    }

    @Test
    void detailLineLimitUsesDeliberateEllipsis() {
        NotificationCardLayout layout = NotificationTextLayout.measure(
                "Title",
                "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen",
                MONOSPACE,
                30,
                12,
                9
        );

        assertEquals(NotificationTextLayout.MAX_DETAIL_LINES, layout.detailLines().size());
        assertEquals("seven...", layout.detailLines().getLast());
        assertTrue(layout.detailLines().stream().allMatch(line -> MONOSPACE.width(line) <= 12));
    }

    @Test
    void mixedHeightStackUsesEachMeasuredHeightAndPreservesOrder() {
        NotificationStackLayout stack = NotificationStackLayout.of(List.of(42, 60, 49), 5);

        assertEquals(List.of(0, 47, 112), stack.offsets());
        assertEquals(161, stack.totalHeight());
    }
}

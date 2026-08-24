package com.bstar.qolmod.hud.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Width-aware notification text layout independent of Minecraft rendering state. */
final class NotificationTextLayout {
    static final int MIN_CARD_HEIGHT = 42;
    static final int MAX_DETAIL_LINES = 4;
    static final int TITLE_Y = 7;
    private static final int TITLE_DETAIL_GAP = 5;
    private static final int BOTTOM_PADDING = 7;
    private static final String ELLIPSIS = "...";

    private NotificationTextLayout() {
    }

    static NotificationCardLayout measure(
            String title,
            String detail,
            TextMeasurer measurer,
            int titleWidth,
            int detailWidth,
            int fontHeight
    ) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(measurer, "measurer");
        if (titleWidth <= 0 || detailWidth <= 0 || fontHeight <= 0) {
            throw new IllegalArgumentException("Notification text bounds must be positive");
        }

        String measuredTitle = ellipsize(title, titleWidth, measurer, false);
        List<String> detailLines = detail == null || detail.isBlank()
                ? List.of()
                : wrap(detail, detailWidth, MAX_DETAIL_LINES, measurer);
        int lineHeight = fontHeight + 2;
        int detailY = TITLE_Y + fontHeight + TITLE_DETAIL_GAP;
        int contentBottom = detailLines.isEmpty()
                ? TITLE_Y + fontHeight
                : detailY + detailLines.size() * lineHeight - 2;
        int height = Math.max(MIN_CARD_HEIGHT, contentBottom + BOTTOM_PADDING);
        return new NotificationCardLayout(
                measuredTitle,
                detailLines,
                height,
                TITLE_Y,
                detailY,
                lineHeight
        );
    }

    private static List<String> wrap(
            String detail,
            int maxWidth,
            int maxLines,
            TextMeasurer measurer
    ) {
        List<String> wrapped = new ArrayList<>();
        String[] paragraphs = detail.strip().split("\\R", -1);
        for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++) {
            String paragraph = paragraphs[paragraphIndex].strip();
            if (paragraph.isEmpty()) {
                if (!wrapped.isEmpty() && paragraphIndex < paragraphs.length - 1) {
                    wrapped.add("");
                }
                continue;
            }
            wrapParagraph(paragraph, maxWidth, measurer, wrapped);
        }

        if (wrapped.size() <= maxLines) {
            return List.copyOf(wrapped);
        }
        List<String> limited = new ArrayList<>(wrapped.subList(0, maxLines));
        int last = limited.size() - 1;
        limited.set(last, addOverflowMarker(limited.get(last), maxWidth, measurer));
        return List.copyOf(limited);
    }

    private static void wrapParagraph(
            String paragraph,
            int maxWidth,
            TextMeasurer measurer,
            List<String> output
    ) {
        String current = "";
        for (String word : paragraph.split("\\s+")) {
            if (measurer.width(word) > maxWidth) {
                if (!current.isEmpty()) {
                    output.add(current);
                    current = "";
                }
                output.add(ellipsize(word, maxWidth, measurer, false));
                continue;
            }

            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measurer.width(candidate) <= maxWidth) {
                current = candidate;
            } else {
                output.add(current);
                current = word;
            }
        }
        if (!current.isEmpty()) {
            output.add(current);
        }
    }

    private static String ellipsize(
            String value,
            int maxWidth,
            TextMeasurer measurer,
            boolean forceMarker
    ) {
        if (!forceMarker && measurer.width(value) <= maxWidth) {
            return value;
        }
        int markerWidth = measurer.width(ELLIPSIS);
        if (markerWidth >= maxWidth) {
            return measurer.trimToWidth(ELLIPSIS, maxWidth);
        }
        String prefix = measurer.trimToWidth(value, maxWidth - markerWidth).stripTrailing();
        return prefix + ELLIPSIS;
    }

    private static String addOverflowMarker(String value, int maxWidth, TextMeasurer measurer) {
        String candidate = value.stripTrailing();
        while (!candidate.isEmpty()) {
            if (measurer.width(candidate + ELLIPSIS) <= maxWidth) {
                return candidate + ELLIPSIS;
            }
            int lastSpace = candidate.lastIndexOf(' ');
            if (lastSpace < 0) {
                break;
            }
            candidate = candidate.substring(0, lastSpace).stripTrailing();
        }
        return ellipsize(value, maxWidth, measurer, true);
    }

    interface TextMeasurer {
        int width(String text);

        String trimToWidth(String text, int width);
    }
}

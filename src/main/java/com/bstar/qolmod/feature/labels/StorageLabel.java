package com.bstar.qolmod.feature.labels;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record StorageLabel(
        String id,
        String dimensionId,
        BlockPos pos,
        String alias,
        List<String> lines,
        Direction face,
        String iconItemId,
        IconPosition iconPosition,
        int textColor,
        long createdAt,
        long updatedAt
) {
    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFF;

    public StorageLabel {
        id = normalizeId(id);
        face = face == null ? Direction.UP : face;
        lines = normalizeLines(lines);
        alias = normalizeAlias(alias, lines);
        iconItemId = iconItemId == null || iconItemId.isBlank() ? null : iconItemId;
        iconPosition = iconPosition == null ? IconPosition.ABOVE : iconPosition;
        textColor = textColor & 0xFFFFFF;
    }

    public String text() {
        return String.join("\n", lines);
    }

    public String firstLine() {
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public StorageLabel withText(String newText) {
        return withLinesAliasColorIconPosition(splitText(newText), alias, textColor, iconItemId, iconPosition);
    }

    public StorageLabel withTextColorIcon(String newText, int newTextColor, String newIconItemId) {
        return withLinesAliasColorIconPosition(splitText(newText), alias, newTextColor, newIconItemId, iconPosition);
    }

    public StorageLabel withLinesAliasColorIconPosition(
            List<String> newLines,
            String newAlias,
            int newTextColor,
            String newIconItemId,
            IconPosition newIconPosition
    ) {
        long now = System.currentTimeMillis();
        return new StorageLabel(id, dimensionId, pos, newAlias, newLines, face, newIconItemId, newIconPosition, newTextColor, createdAt, now);
    }

    public static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static List<String> splitText(String text) {
        String normalized = normalizeControlCharacters(text == null ? "" : text);
        String[] split = normalized.split("\\R", -1);
        List<String> result = new ArrayList<>();
        for (String line : split) {
            result.add(line);
        }
        return result.isEmpty() ? List.of("") : result;
    }

    public static String normalizeAlias(String alias, List<String> lines) {
        String normalized = normalizeControlCharacters(alias == null ? "" : alias).replace('\n', ' ').replace('\r', ' ').trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }

        return lines == null || lines.isEmpty() ? "" : normalizeControlCharacters(lines.get(0)).replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String normalizeId(String id) {
        return id == null || id.isBlank() ? generateId() : normalizeControlCharacters(id).trim();
    }

    private static List<String> normalizeLines(List<String> sourceLines) {
        if (sourceLines == null || sourceLines.isEmpty()) {
            return List.of("");
        }

        List<String> normalized = new ArrayList<>();
        for (String line : sourceLines) {
            normalized.add(normalizeControlCharacters(line == null ? "" : line));
        }
        return List.copyOf(normalized);
    }

    private static String normalizeControlCharacters(String value) {
        return value.replace('\u240A', ' ').replace('\u000B', ' ');
    }

    public enum IconPosition {
        ABOVE,
        BELOW;

        public IconPosition toggle() {
            return this == ABOVE ? BELOW : ABOVE;
        }
    }
}

package com.bstar.qolmod.ui.theme;

/** Semantic colours used by every QOLmod UI surface and component. */
public record ColorPalette(
        int background,
        int surface,
        int elevatedSurface,
        int accent,
        int accentHover,
        int outerBorder,
        int structuralDivider,
        int subtleDivider,
        int primaryText,
        int secondaryText,
        int mutedText,
        int active,
        int waiting,
        int completed,
        int error,
        int shadow
) {
}

package com.bstar.qolmod.hud.editor;

import com.bstar.qolmod.hud.HudWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A normal HUD widget that also supplies isolated editor-only preview presentation. */
public interface EditableHudWidget extends HudWidget {
    HudEditorMetadata editorMetadata();

    HudSize previewSize(MinecraftClient client);

    void renderPreview(DrawContext context, MinecraftClient client, double opacity);
}

package com.bstar.qolmod.hud.editor;

import java.util.List;

public record HudSnapResult(int x, int y, List<HudSnapGuide> guides) {
    public HudSnapResult {
        guides = List.copyOf(guides);
    }
}

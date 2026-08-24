package com.bstar.qolmod.hud;

public interface HudWidget {
    String id();

    HudWidgetConfig config();

    void render(HudContext context);
}

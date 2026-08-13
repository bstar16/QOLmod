package com.bstar.qolmod.feature;

import com.bstar.qolmod.feature.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractFeature implements Feature {
    private final String id;
    private final String name;
    private final String description;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    protected AbstractFeature(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final String description() {
        return description;
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public final List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    protected final <T extends Setting<?>> T registerSetting(T setting) {
        settings.add(setting);
        return setting;
    }
}

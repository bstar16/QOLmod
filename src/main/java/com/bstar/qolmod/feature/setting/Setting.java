package com.bstar.qolmod.feature.setting;

import com.google.gson.JsonElement;

public abstract class Setting<T> {
    private final String id;
    private final String displayName;
    private final String description;
    private final T defaultValue;
    private T value;

    protected Setting(String id, String displayName, String description, T defaultValue) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final String id() {
        return id;
    }

    public final String displayName() {
        return displayName;
    }

    public final String description() {
        return description;
    }

    public final T get() {
        return value;
    }

    public final void set(T value) {
        this.value = sanitize(value);
    }

    public final T defaultValue() {
        return defaultValue;
    }

    public final void reset() {
        value = defaultValue;
    }

    public abstract JsonElement toJson();

    public abstract void load(JsonElement element);

    protected T sanitize(T value) {
        return value;
    }
}

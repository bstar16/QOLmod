package com.bstar.qolmod.setting;

import com.google.gson.JsonElement;
import java.util.Objects;

/** A typed value with generic JSON persistence metadata for config and UI consumers. */
public abstract class Setting<T> {
    private final String id;
    private final String displayName;
    private final String description;
    private final T defaultValue;
    private T value;

    protected Setting(String id, String displayName, String description, T defaultValue) {
        this.id = requireText(id, "id");
        this.displayName = requireText(displayName, "displayName");
        this.description = Objects.requireNonNull(description, "description");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
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
        this.value = sanitize(Objects.requireNonNull(value, "value"));
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

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}

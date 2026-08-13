package com.bstar.qolmod.feature.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;

    public IntSetting(String id, String displayName, String description, int defaultValue, int min, int max) {
        super(id, displayName, description, defaultValue);
        this.min = min;
        this.max = max;
        set(defaultValue);
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            set(element.getAsInt());
        }
    }

    @Override
    protected Integer sanitize(Integer value) {
        return Math.max(min, Math.min(max, value));
    }
}

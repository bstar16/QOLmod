package com.bstar.qolmod.feature.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;

    public DoubleSetting(String id, String displayName, String description, double defaultValue, double min, double max) {
        super(id, displayName, description, defaultValue);
        this.min = min;
        this.max = max;
        set(defaultValue);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            set(element.getAsDouble());
        }
    }

    @Override
    protected Double sanitize(Double value) {
        return Math.max(min, Math.min(max, value));
    }
}

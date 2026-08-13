package com.bstar.qolmod.feature.labels;

import com.bstar.qolmod.QOLmodClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class StorageLabelManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<StorageLabel> labels = new ArrayList<>();
    private final Path labelsPath = FabricLoader.getInstance().getConfigDir().resolve(QOLmodClient.MOD_ID + "-storage-labels.json");
    private boolean dirtyAfterLoad;

    public void load() {
        labels.clear();
        dirtyAfterLoad = false;

        if (!Files.exists(labelsPath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(labelsPath)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonArray labelArray = getArray(rootElement.getAsJsonObject(), "labels");
            if (labelArray == null) {
                return;
            }

            for (JsonElement element : labelArray) {
                readLabel(element).ifPresent(labels::add);
            }
            if (dirtyAfterLoad) {
                save();
            }
        } catch (RuntimeException | IOException exception) {
            labels.clear();
            QOLmodClient.LOGGER.warn("Failed to load storage labels, using an empty label list.", exception);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonArray labelArray = new JsonArray();
        root.add("labels", labelArray);

        for (StorageLabel label : labels) {
            JsonObject object = new JsonObject();
            object.addProperty("id", label.id());
            object.addProperty("dimension", label.dimensionId());
            object.addProperty("x", label.pos().getX());
            object.addProperty("y", label.pos().getY());
            object.addProperty("z", label.pos().getZ());
            object.addProperty("alias", label.alias());
            JsonArray lines = new JsonArray();
            for (String line : label.lines()) {
                lines.add(line);
            }
            object.add("lines", lines);
            object.addProperty("face", label.face().asString().toUpperCase());
            if (label.iconItemId() != null) {
                object.addProperty("iconItemId", label.iconItemId());
            }
            object.addProperty("iconPosition", label.iconPosition().name());
            if (label.textColor() != StorageLabel.DEFAULT_TEXT_COLOR) {
                object.addProperty("textColor", String.format("#%06X", label.textColor()));
            }
            object.addProperty("createdAt", label.createdAt());
            object.addProperty("updatedAt", label.updatedAt());
            labelArray.add(object);
        }

        try {
            Files.createDirectories(labelsPath.getParent());
            try (Writer writer = Files.newBufferedWriter(labelsPath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            QOLmodClient.LOGGER.warn("Failed to save storage labels.", exception);
        }
    }

    public List<StorageLabel> labelsInDimension(String dimensionId) {
        return labels.stream()
                .filter(label -> matchesDimension(label.dimensionId(), dimensionId))
                .toList();
    }

    public List<StorageLabel> allLabels() {
        return List.copyOf(labels);
    }

    public int totalLabels() {
        return labels.size();
    }

    public void put(String dimensionId, BlockPos pos, String text, Direction face) {
        long now = System.currentTimeMillis();
        removeAt(dimensionId, pos, false);
        List<String> lines = StorageLabel.splitText(text);
        labels.add(new StorageLabel(
                StorageLabel.generateId(),
                dimensionId,
                pos.toImmutable(),
                StorageLabel.normalizeAlias(null, lines),
                lines,
                face,
                null,
                StorageLabel.IconPosition.ABOVE,
                StorageLabel.DEFAULT_TEXT_COLOR,
                now,
                now
        ));
        save();
    }

    public void update(StorageLabel original, StorageLabel updated) {
        int index = labels.indexOf(original);
        if (index >= 0) {
            labels.set(index, updated);
            save();
        }
    }

    public Optional<StorageLabel> findNearestByText(String dimensionId, String text, Vec3d origin) {
        return labelsInDimension(dimensionId).stream()
                .filter(label -> label.text().equals(text))
                .min(Comparator.comparingDouble(label -> labelCenter(label).squaredDistanceTo(origin)));
    }

    public Optional<StorageLabel> findNearestByIdentifier(String dimensionId, String identifier, Vec3d origin) {
        String normalized = StorageLabel.normalizeAlias(identifier, List.of(""));
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        Optional<StorageLabel> byId = labelsInDimension(dimensionId).stream()
                .filter(label -> label.id().equalsIgnoreCase(normalized))
                .min(Comparator.comparingDouble(label -> labelCenter(label).squaredDistanceTo(origin)));
        if (byId.isPresent()) {
            return byId;
        }

        Optional<StorageLabel> byAlias = labelsInDimension(dimensionId).stream()
                .filter(label -> label.alias().equals(normalized))
                .min(Comparator.comparingDouble(label -> labelCenter(label).squaredDistanceTo(origin)));
        if (byAlias.isPresent()) {
            return byAlias;
        }

        return labelsInDimension(dimensionId).stream()
                .filter(label -> label.firstLine().equals(normalized))
                .min(Comparator.comparingDouble(label -> labelCenter(label).squaredDistanceTo(origin)));
    }

    public Optional<StorageLabel> findAt(String dimensionId, BlockPos pos) {
        return labelsInDimension(dimensionId).stream()
                .filter(label -> label.pos().equals(pos))
                .findFirst();
    }

    public Optional<StorageLabel> removeAt(String dimensionId, BlockPos pos) {
        return removeAt(dimensionId, pos, true);
    }

    public Optional<StorageLabel> nearest(String dimensionId, Vec3d origin, double maxDistance) {
        double maxDistanceSquared = maxDistance * maxDistance;
        return labelsInDimension(dimensionId).stream()
                .filter(label -> labelCenter(label).squaredDistanceTo(origin) <= maxDistanceSquared)
                .min(Comparator.comparingDouble(label -> labelCenter(label).squaredDistanceTo(origin)));
    }

    public int clearNearby(String dimensionId, Vec3d origin, double radius) {
        double radiusSquared = radius * radius;
        int removed = 0;
        Iterator<StorageLabel> iterator = labels.iterator();
        while (iterator.hasNext()) {
            StorageLabel label = iterator.next();
            if (matchesDimension(label.dimensionId(), dimensionId) && labelCenter(label).squaredDistanceTo(origin) <= radiusSquared) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            save();
        }

        return removed;
    }

    private Optional<StorageLabel> removeAt(String dimensionId, BlockPos pos, boolean shouldSave) {
        Iterator<StorageLabel> iterator = labels.iterator();
        while (iterator.hasNext()) {
            StorageLabel label = iterator.next();
            if (matchesDimension(label.dimensionId(), dimensionId) && label.pos().equals(pos)) {
                iterator.remove();
                if (shouldSave) {
                    save();
                }
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    private Optional<StorageLabel> readLabel(JsonElement element) {
        if (!element.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject object = element.getAsJsonObject();
        String dimension = getString(object, "dimension");
        String oldText = getString(object, "text");
        JsonArray linesArray = getArray(object, "lines");
        Integer x = getInt(object, "x");
        Integer y = getInt(object, "y");
        Integer z = getInt(object, "z");

        if (dimension == null || (oldText == null && linesArray == null) || x == null || y == null || z == null) {
            return Optional.empty();
        }

        String id = getString(object, "id");
        String alias = getString(object, "alias");
        List<String> lines = linesArray == null ? StorageLabel.splitText(oldText) : readLines(linesArray);
        long createdAt = getLong(object, "createdAt", System.currentTimeMillis());
        long updatedAt = getLong(object, "updatedAt", createdAt);
        Direction face = getDirection(object, "face", Direction.UP);
        String iconItemId = getString(object, "iconItemId");
        StorageLabel.IconPosition iconPosition = getIconPosition(object, "iconPosition", StorageLabel.IconPosition.ABOVE);
        int textColor = getColor(object, "textColor", StorageLabel.DEFAULT_TEXT_COLOR);
        if (id == null || alias == null || linesArray == null || getString(object, "iconPosition") == null) {
            dirtyAfterLoad = true;
        }
        return Optional.of(new StorageLabel(id, dimension, new BlockPos(x, y, z), alias, lines, face, iconItemId, iconPosition, textColor, createdAt, updatedAt));
    }

    private Vec3d labelCenter(StorageLabel label) {
        return Vec3d.ofCenter(label.pos());
    }

    public static boolean matchesDimension(String storedDimensionId, String currentDimensionId) {
        return normalizeDimensionId(storedDimensionId).equals(normalizeDimensionId(currentDimensionId));
    }

    public static String normalizeDimensionId(String dimensionId) {
        if (dimensionId == null) {
            return "";
        }

        String normalized = dimensionId.trim().toLowerCase();
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString()
                : null;
    }

    private Integer getInt(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : null;
    }

    private long getLong(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsLong()
                : fallback;
    }

    private List<String> readLines(JsonArray linesArray) {
        List<String> lines = new ArrayList<>();
        for (JsonElement element : linesArray) {
            if (element != null && element.isJsonPrimitive()) {
                lines.add(element.getAsString());
            }
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private int getColor(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private Direction getDirection(JsonObject object, String key, Direction fallback) {
        String value = getString(object, key);
        if (value == null) {
            return fallback;
        }

        try {
            return Direction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private StorageLabel.IconPosition getIconPosition(JsonObject object, String key, StorageLabel.IconPosition fallback) {
        String value = getString(object, key);
        if (value == null) {
            return fallback;
        }

        try {
            return StorageLabel.IconPosition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}

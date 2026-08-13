package com.bstar.qolmod.gui;

import com.bstar.qolmod.feature.impl.StorageLabelsFeature;
import com.bstar.qolmod.feature.labels.StorageLabel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

public final class StorageIconPickerScreen extends Screen {
    private static final int CELL_SIZE = 24;
    private static final int ICON_SIZE = 16;

    private final Screen parent;
    private final StorageLabelsFeature feature;
    private final StorageLabel label;
    private final List<ItemEntry> allItems = new ArrayList<>();
    private final List<ItemEntry> filteredItems = new ArrayList<>();
    private TextFieldWidget searchField;
    private int scrollOffset;

    public StorageIconPickerScreen(Screen parent, StorageLabelsFeature feature, StorageLabel label) {
        super(Text.literal("Storage Label Icon"));
        this.parent = parent;
        this.feature = feature;
        this.label = label;
    }

    @Override
    protected void init() {
        if (allItems.isEmpty()) {
            loadItems();
        }

        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        searchField = new TextFieldWidget(textRenderer, left, 34, panelWidth, 20, Text.literal("Search items"));
        searchField.setMaxLength(80);
        searchField.setPlaceholder(Text.literal("Search item/block ids").formatted(Formatting.GRAY));
        searchField.setChangedListener(value -> {
            scrollOffset = 0;
            refreshFilter();
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(width / 2 - 156, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 - 50, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Icon"), button -> {
            chooseIcon(null);
            close();
        }).dimensions(width / 2 + 56, height - 28, 100, 20).build());

        refreshFilter();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);

        Grid grid = grid();
        context.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        int columns = grid.columns();
        int firstRow = scrollOffset / CELL_SIZE;
        int yOffset = -(scrollOffset % CELL_SIZE);
        int maxVisibleRows = (grid.height() / CELL_SIZE) + 2;
        for (int row = 0; row < maxVisibleRows; row++) {
            int itemRow = firstRow + row;
            for (int column = 0; column < columns; column++) {
                int index = itemRow * columns + column;
                if (index >= filteredItems.size()) {
                    continue;
                }

                int x = grid.left() + column * CELL_SIZE;
                int y = grid.top() + yOffset + row * CELL_SIZE;
                boolean hovered = mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
                context.fill(x, y, x + CELL_SIZE - 2, y + CELL_SIZE - 2, hovered ? 0x66888888 : 0x44333333);
                context.drawItemWithoutEntity(filteredItems.get(index).stack(), x + 4, y + 4);
            }
        }
        context.disableScissor();

        if (filteredItems.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No matching items"), width / 2, grid.top() + 20, 0xAAAAAA);
        }

        ItemEntry hovered = hoveredItem(mouseX, mouseY);
        if (hovered != null) {
            context.drawTooltip(textRenderer, hovered.stack().getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ItemEntry entry = hoveredItem((int) click.x(), (int) click.y());
        if (entry != null) {
            chooseIcon(entry.id().toString());
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Grid grid = grid();
        int rows = Math.max(0, (filteredItems.size() + grid.columns() - 1) / grid.columns());
        int maxScroll = Math.max(0, rows * CELL_SIZE - grid.height());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * CELL_SIZE)));
        return true;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void loadItems() {
        for (Item item : Registries.ITEM) {
            ItemStack stack = item.getDefaultStack();
            if (!stack.isEmpty()) {
                Identifier id = Registries.ITEM.getId(item);
                allItems.add(new ItemEntry(id, stack));
            }
        }
        allItems.sort(Comparator.comparing(entry -> entry.id().toString()));
    }

    private void refreshFilter() {
        filteredItems.clear();
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        for (ItemEntry entry : allItems) {
            String id = entry.id().toString();
            String name = entry.stack().getName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || id.contains(query) || name.contains(query)) {
                filteredItems.add(entry);
            }
        }
    }

    private ItemEntry hoveredItem(int mouseX, int mouseY) {
        Grid grid = grid();
        if (mouseX < grid.left() || mouseX >= grid.right() || mouseY < grid.top() || mouseY >= grid.bottom()) {
            return null;
        }

        int column = (mouseX - grid.left()) / CELL_SIZE;
        int row = (mouseY - grid.top() + scrollOffset) / CELL_SIZE;
        int index = row * grid.columns() + column;
        return index >= 0 && index < filteredItems.size() ? filteredItems.get(index) : null;
    }

    private void chooseIcon(String iconItemId) {
        if (parent instanceof StorageLabelEditScreen editScreen) {
            editScreen.setIconItemId(iconItemId);
        } else {
            feature.setIcon(label, iconItemId);
        }
    }

    private Grid grid() {
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        int top = 64;
        int bottom = height - 38;
        int columns = Math.max(1, panelWidth / CELL_SIZE);
        return new Grid(left, top, left + columns * CELL_SIZE, bottom, columns);
    }

    private record ItemEntry(Identifier id, ItemStack stack) {
    }

    private record Grid(int left, int top, int right, int bottom, int columns) {
        int height() {
            return bottom - top;
        }
    }
}

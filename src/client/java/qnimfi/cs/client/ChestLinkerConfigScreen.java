package qnimfi.cs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import qnimfi.cs.menu.ChestLinkerConfigMenu;
import qnimfi.cs.menu.ChestLinkerGuiLayout;
import qnimfi.cs.network.AdjustFilterMaxPayload;
import qnimfi.cs.network.SetFilterPayload;

public class ChestLinkerConfigScreen extends AbstractContainerScreen<ChestLinkerConfigMenu> {

    public ChestLinkerConfigScreen(ChestLinkerConfigMenu menu, Inventory inventory, Component title) {
        super(
                menu,
                inventory,
                title,
                ChestLinkerGuiLayout.GUI_WIDTH,
                ChestLinkerGuiLayout.getTotalGuiHeight(menu.getSlotCount())
        );

        this.inventoryLabelX = 8;
        this.inventoryLabelY = ChestLinkerGuiLayout.getInventoryY(menu.getSlotCount()) + 5 - topPos;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
    }

    private boolean isShiftPressed() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        this.inventoryLabelY = ChestLinkerGuiLayout.getInventoryY(menu.getSlotCount()) + 5;
        this.titleLabelY = 5;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int slot = getFilterSlotAt(mouseX, mouseY);
        if (slot >= 0) {
            int step = isShiftPressed() ? 16 : 1;
            int delta = scrollY > 0 ? step : -step;
            ClientPlayNetworking.send(new AdjustFilterMaxPayload(slot, delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 || event.button() == 1) {
            int slot = getFilterSlotAt(event.x(), event.y());
            if (slot >= 0) {
                ItemStack carried = menu.getCarried();
                int itemId = carried.isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(carried.getItem());
                ClientPlayNetworking.send(new SetFilterPayload(slot, itemId));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int getFilterSlotAt(double mouseX, double mouseY) {
        int columns = ChestLinkerGuiLayout.FILTER_COLUMNS;
        int filterY = topPos + ChestLinkerGuiLayout.FILTER_ROW_Y;

        for (int i = 0; i < menu.getSlotCount(); i++) {
            int row = i / columns;
            int col = i % columns;
            int slotsInRow = Math.min(columns, menu.getSlotCount() - row * columns);
            int rowOffset = (columns - slotsInRow) * 9;

            int x = leftPos + 8 + rowOffset + col * 18;
            int y = filterY + row * 18;

            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int slotCount = menu.getSlotCount();
        int filterPanelHeight = ChestLinkerGuiLayout.getFilterPanelHeight(slotCount);
        int inventoryTop = topPos + ChestLinkerGuiLayout.getInventoryY(slotCount);
        int inventoryHeight = ChestLinkerGuiLayout.getInventoryHeight();

        // Top Filter Background Card
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + filterPanelHeight, 0xFF202020);

        // Bottom Inventory Background Card
        graphics.fill(leftPos, inventoryTop, leftPos + imageWidth, inventoryTop + inventoryHeight, 0xFF202020);

        // Slot backgrounds
        for (Slot slot : this.menu.slots) {
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF101010);
            graphics.fill(x, y, x + 16, y + 16, 0xFF383838);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int columns = ChestLinkerGuiLayout.FILTER_COLUMNS;
        int filterY = ChestLinkerGuiLayout.FILTER_ROW_Y;

        for (int i = 0; i < menu.getSlotCount(); i++) {
            if (menu.getFilterItem(i) == Items.AIR) continue;

            int max = menu.getMaxCount(i);
            if (max <= 0) max = 64; // Fallback display default

            int row = i / columns;
            int col = i % columns;
            int slotsInRow = Math.min(columns, menu.getSlotCount() - row * columns);
            int rowOffset = (columns - slotsInRow) * 9;

            // Calculate absolute screen position
            int screenX = leftPos + 8 + rowOffset + col * 18;
            int screenY = topPos + filterY + row * 18;

            String text = String.valueOf(max);
            int textWidth = font.width(text);

            // Render opaque white text with drop shadow in bottom-right corner of the slot
            graphics.text(
                    font,
                    text,
                    screenX + 17 - textWidth,
                    screenY + 9,
                    0xFFFFFFFF, // Fully opaque white
                    true
            );
        }
    }
}
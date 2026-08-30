package qnimfi.cs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
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

import java.util.List;

public class ChestLinkerConfigScreen extends AbstractContainerScreen<ChestLinkerConfigMenu> {

    private int typingSlotIndex = -1;
    private String typingBuffer = "";

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
        Slot cachedSlot = this.hoveredSlot;
        if (cachedSlot != null && cachedSlot.index >= 0 && cachedSlot.index < menu.getSlotCount()) {
            this.hoveredSlot = null; // Hide from vanilla tooltip system completely
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        this.hoveredSlot = cachedSlot; // Restore for click handling

        int columns = ChestLinkerGuiLayout.FILTER_COLUMNS;
        int filterY = ChestLinkerGuiLayout.FILTER_ROW_Y;

        for (int i = 0; i < menu.getSlotCount(); i++) {
            if (menu.getFilterItem(i) == Items.AIR) continue;

            int max = menu.getMaxCount(i);
            if (max <= 0) max = 64;

            int row = i / columns;
            int col = i % columns;
            int slotsInRow = Math.min(columns, menu.getSlotCount() - row * columns);
            int rowOffset = (columns - slotsInRow) * 9;

            int screenX = leftPos + 8 + rowOffset + col * 18;
            int screenY = topPos + filterY + row * 18;

            // SHOW TYPING BUFFER INSTEAD OF SAVED VALUE IF APPLICABLE
            String text = (i == typingSlotIndex && !typingBuffer.isEmpty()) ? typingBuffer : String.valueOf(max);

            // 0xFFFFFF00 is yellow to visually indicate they are editing the number
            int textColor = (i == typingSlotIndex && !typingBuffer.isEmpty()) ? 0xFFFFFF00 : 0xFFFFFFFF;

            int textWidth = font.width(text);
            graphics.text(
                    font,
                    text,
                    screenX + 17 - textWidth,
                    screenY + 9,
                    textColor,
                    true
            );
        }
    }

    @Override
    protected void extractTooltip(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.index >= 0 && this.hoveredSlot.index < menu.getSlotCount()) {
            if (this.hoveredSlot.hasItem()) {
                ItemStack item = this.hoveredSlot.getItem();
                List<Component> tooltip = new java.util.ArrayList<>();

                // Header line
                tooltip.add(Component.translatable("menu.chestsorter.tooltip.filtered_item").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

                // Item name
                tooltip.add(item.getHoverName());

                // Eye-catching instructions in different colors
                tooltip.add(Component.translatable("menu.chestsorter.tooltip.scroll").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.translatable("menu.chestsorter.tooltip.shift_scroll").withStyle(ChatFormatting.AQUA));

                graphics.setTooltipForNextFrame(
                        this.font,
                        tooltip,
                        item.getTooltipImage(),
                        mouseX,
                        mouseY,
                        item.get(DataComponents.TOOLTIP_STYLE)
                );
            }
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.@NonNull KeyEvent event) {
        double mouseX = minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();

        int slot = getFilterSlotAt(mouseX, mouseY);
        if (slot >= 0 && slot < menu.getSlotCount() && menu.getFilterItem(slot) != Items.AIR) {
            int key = event.key();

            // Commit on Enter or Numpad Enter if we are actively typing in this slot
            if ((key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) && typingSlotIndex == slot) {
                commitTypedAmount();
                return true;
            }

            char digit = '\0';

            // Check standard number row
            if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_0 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_9) {
                digit = (char) ('0' + (key - org.lwjgl.glfw.GLFW.GLFW_KEY_0));
            }
            // Check numpad
            else if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_KP_9) {
                digit = (char) ('0' + (key - org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0));
            }

            if (digit != '\0') {
                if (typingSlotIndex != slot) {
                    commitTypedAmount();
                    typingSlotIndex = slot;
                    typingBuffer = "";
                }

                if (typingBuffer.length() < 4) {
                    typingBuffer += digit;
                }
                return true;
            }

            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && typingSlotIndex == slot && !typingBuffer.isEmpty()) {
                typingBuffer = typingBuffer.substring(0, typingBuffer.length() - 1);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void commitTypedAmount() {
        if (typingSlotIndex != -1 && !typingBuffer.isEmpty()) {
            try {
                int newValue = Integer.parseInt(typingBuffer);
                int currentValue = menu.getMaxCount(typingSlotIndex);
                int delta = newValue - currentValue;

                if (delta != 0) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new qnimfi.cs.network.AdjustFilterMaxPayload(typingSlotIndex, delta)
                    );
                }
            } catch (NumberFormatException ignored) {}

            typingBuffer = "";
            typingSlotIndex = -1;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (typingSlotIndex != -1) {
            double mouseX = minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();

            if (getFilterSlotAt(mouseX, mouseY) != typingSlotIndex) {
                commitTypedAmount();
            }
        }
    }

    @Override
    public void onClose() {
        commitTypedAmount();
        super.onClose();
    }
}
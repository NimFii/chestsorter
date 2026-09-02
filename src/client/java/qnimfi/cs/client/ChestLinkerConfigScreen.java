package qnimfi.cs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import qnimfi.cs.AuthorityPermission;
import qnimfi.cs.FilterType;
import qnimfi.cs.menu.ChestLinkerConfigMenu;
import qnimfi.cs.menu.ChestLinkerGuiLayout;
import qnimfi.cs.network.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChestLinkerConfigScreen extends AbstractContainerScreen<ChestLinkerConfigMenu> {

    private boolean viewingSettings = false;
    private Button authorityButton;
    private boolean viewingAuthority = false;
    private java.util.List<AuthorityDataPayload.PlayerData> authorityPlayers = new java.util.ArrayList<>();
    private java.util.UUID selectedAuthorityPlayer = null;
    private boolean authorityDataReceived = false;

    public ChestLinkerConfigScreen(ChestLinkerConfigMenu menu, Inventory inventory, Component title) {
        super(
                menu,
                inventory,
                title,
                ChestLinkerGuiLayout.GUI_WIDTH,
                ChestLinkerGuiLayout.getTotalGuiHeight(menu.getSlotCount(), false)
        );
    }

    private int typingSlotIndex = -1;
    private String typingBuffer = "";

    // BURN Mode Warning State
    private int pendingBurnSlot = -1;
    private Button confirmBurnBtn;
    private Button cancelBurnBtn;

    private boolean isShiftPressed() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isCtrlPressed() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private int getMaxCapacityForSlot(int slot) {
        Item filterItem = menu.getFilterItem(slot);
        if (filterItem == null || filterItem == Items.AIR) return 64;

        int maxStack = new ItemStack(filterItem).getMaxStackSize();
        int containerSlots = 27;

        if (minecraft.level != null && menu.getReceiverPos() != null) {
            var blockPos = menu.getReceiverPos();
            var blockState = minecraft.level.getBlockState(blockPos);
            var blockEntity = minecraft.level.getBlockEntity(blockPos);

            if (blockEntity instanceof Container container) {
                containerSlots = container.getContainerSize();

                if (blockState.getBlock() instanceof ChestBlock) {
                    ChestType chestType = blockState.getValue(ChestBlock.TYPE);
                    if (chestType != ChestType.SINGLE) {
                        containerSlots = 54;
                    }
                }
            }
        }

        int maxCap = containerSlots * maxStack;

        int currentMax = menu.getMaxCount(slot);
        if (currentMax != -1 && currentMax > maxCap) {
            int delta = maxCap - currentMax;
            ClientPlayNetworking.send(new AdjustFilterMaxPayload(slot, delta));
        }

        return maxCap;
    }

    private int getDynamicHeight() {
        boolean hideEverything = viewingSettings || viewingAuthority;
        int activeSlotCount = ChestLinkerGuiLayout.getActiveSlotCount(menu.getSlotCount(), hideEverything);
        return viewingAuthority ? 160 : ChestLinkerGuiLayout.getTotalGuiHeight(activeSlotCount, hideEverything);
    }

    @Override
    protected void init() {
        super.init();

        boolean hideEverything = viewingSettings || viewingAuthority;
        int activeSlotCount = ChestLinkerGuiLayout.getActiveSlotCount(menu.getSlotCount(), hideEverything);
        int dynamicHeight = getDynamicHeight();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - dynamicHeight) / 2;

        this.inventoryLabelY = hideEverything ? -999 : (ChestLinkerGuiLayout.getInventoryY(activeSlotCount) + 5);
        this.titleLabelY = 5;

        this.menu.updateSlotPositions(hideEverything);

        if (viewingAuthority) {
            initAuthorityWidgets();
            return;
        }

        initNormalWidgets();
    }

    private void initNormalWidgets() {
        int dynamicHeight = getDynamicHeight();
        int boxHeight = 86;
        int boxY = topPos + (dynamicHeight - boxHeight) / 2;

        this.confirmBurnBtn = Button.builder(
                Component.translatable(  "gui.chestsorter.confirm").withStyle(ChatFormatting.RED),
                _ -> {
                    ClientPlayNetworking.send(new CycleFilterTypePayload(pendingBurnSlot));
                    pendingBurnSlot = -1;
                }
        ).bounds((this.width / 2) - 105, boxY + 58, 100, 20).build();

        this.cancelBurnBtn = Button.builder(
                Component.translatable(  "gui.chestsorter.cancel"),
                _ -> pendingBurnSlot = -1
        ).bounds((this.width / 2) + 5, boxY + 58, 100, 20).build();

        confirmBurnBtn.visible = false;
        confirmBurnBtn.active = false;
        cancelBurnBtn.visible = false;
        cancelBurnBtn.active = false;

        addRenderableWidget(confirmBurnBtn);
        addRenderableWidget(cancelBurnBtn);

        Button infoBtn = Button.builder(
                Component.literal("ⓘ").withStyle(ChatFormatting.AQUA),
                _ -> {}
        ).bounds(leftPos + imageWidth - 22, topPos + 4, 14, 14).build();

        addRenderableWidget(Button.builder(
                Component.literal(viewingSettings ? "↩" : "⚙"),
                _ -> {
                    viewingSettings = !viewingSettings;
                    rebuildWidgets();
                }
        ).bounds(leftPos + imageWidth - 40, topPos + 4, 14, 14).build());

        addRenderableWidget(infoBtn);

        if (viewingSettings) {
            int btnWidth = 20;
            int btnHeight = 20;
            int btnX = leftPos + (imageWidth - btnWidth) / 2;
            int btnY = topPos + 32;

            authorityButton = Button.builder(
                    Component.empty(),
                    _ -> openAuthority()
            ).bounds(btnX, btnY, btnWidth, btnHeight).build();

            addRenderableWidget(authorityButton);
        } else {
            authorityButton = null;
        }
    }

    private void initAuthorityWidgets() {
        addRenderableWidget(Button.builder(
                Component.literal("↩"),
                _ -> {
                    if (selectedAuthorityPlayer != null) {
                        selectedAuthorityPlayer = null;
                    } else {
                        viewingAuthority = false;
                        viewingSettings = true;
                    }
                    rebuildWidgets();
                }
        ).bounds(leftPos + imageWidth - 40, topPos + 4, 14, 14).build());

        Button infoBtn = Button.builder(
                Component.literal("ⓘ").withStyle(ChatFormatting.AQUA),
                _ -> {}
        ).bounds(leftPos + imageWidth - 22, topPos + 4, 14, 14).build();
        addRenderableWidget(infoBtn);

        if (selectedAuthorityPlayer != null) {
            addPermissionButton("gui.chestsorter.permission.gizmos", AuthorityPermission.GIZMOS, 36);
            addPermissionButton("gui.chestsorter.permission.filter", AuthorityPermission.FILTER, 58);
            addPermissionButton("gui.chestsorter.permission.settings", AuthorityPermission.SETTINGS, 80);
            addPermissionButton("gui.chestsorter.permission.authority", AuthorityPermission.AUTHORITY, 102);
            addPermissionButton("gui.chestsorter.permission.connect", AuthorityPermission.CONNECT, 124);
        } else {
            for (int i = 0; i < authorityPlayers.size(); i++) {
                AuthorityDataPayload.PlayerData player = authorityPlayers.get(i);

                addRenderableWidget(Button.builder(
                        Component.literal("   " + player.name()),
                        _ -> {
                            selectedAuthorityPlayer = player.uuid();
                            rebuildWidgets();
                        }
                ).bounds(leftPos + 8, topPos + 32 + i * 22, imageWidth - 16, 20).build());
            }
        }
    }

    private void addPermissionButton(String translationKey, AuthorityPermission permission, int y) {
        AuthorityDataPayload.PlayerData player = getSelectedAuthorityPlayer();
        if (player == null) return;

        boolean enabled = switch (permission) {
            case GIZMOS -> player.gizmos();
            case FILTER -> player.filter();
            case SETTINGS -> player.settings();
            case AUTHORITY -> player.authority();
            case CONNECT -> player.connect();
        };

        Component stateComponent = Component.translatable(enabled ? "gui.chestsorter.state.on" : "gui.chestsorter.state.off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);

        Component buttonMessage = Component.translatable(translationKey)
                .append(": ")
                .append(stateComponent);

        addRenderableWidget(Button.builder(
                buttonMessage,
                _ -> togglePermission(permission)
        ).bounds(leftPos + 8, topPos + y, imageWidth - 16, 20).build());
    }

    private void openAuthority() {
        viewingSettings = false;
        viewingAuthority = true;
        selectedAuthorityPlayer = null;
        authorityDataReceived = false;
        rebuildWidgets();

        ClientPlayNetworking.send(new RequestAuthorityDataPayload(menu.getReceiverPos()));
    }

    public void setAuthorityPlayers(List<AuthorityDataPayload.PlayerData> players) {
        authorityPlayers = new ArrayList<>(players);
        authorityDataReceived = true;
        rebuildWidgets();
    }

    private AuthorityDataPayload.PlayerData getSelectedAuthorityPlayer() {
        if (selectedAuthorityPlayer == null) return null;

        return authorityPlayers.stream()
                .filter(p -> p.uuid().equals(selectedAuthorityPlayer))
                .findFirst()
                .orElse(null);
    }

    private void togglePermission(AuthorityPermission permission) {
        AuthorityDataPayload.PlayerData player = getSelectedAuthorityPlayer();
        if (player == null) return;

        boolean current = switch (permission) {
            case GIZMOS -> player.gizmos();
            case FILTER -> player.filter();
            case SETTINGS -> player.settings();
            case AUTHORITY -> player.authority();
            case CONNECT -> player.connect();
        };

        ClientPlayNetworking.send(new SetAuthorityPermissionPayload(
                menu.getReceiverPos(),
                player.uuid(),
                permission.ordinal(),
                !current
        ));
    }

    @Override
    public @NonNull Component getTitle() {
        if (viewingAuthority) {
            return Component.translatable("gui.chestsorter.authority");
        }

        return viewingSettings
                ? Component.translatable("chest_interaction.chestsorter.receiver_config_open")
                : super.getTitle();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (viewingSettings || viewingAuthority || pendingBurnSlot != -1) return true;

        int slot = getFilterSlotAt(mouseX, mouseY);
        if (slot >= 0 && menu.getFilterType(slot) == FilterType.ONLY) {
            int current = menu.getMaxCount(slot);
            int delta = getDelta(slot, scrollY, current);
            ClientPlayNetworking.send(new AdjustFilterMaxPayload(slot, delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getDelta(int slot, double scrollY, int current) {
        int step = isShiftPressed() ? 16 : 1;
        int maxCap = getMaxCapacityForSlot(slot);
        int delta;

        if (current == -1) {
            delta = scrollY > 0 ? (1 - (-1)) : (maxCap - (-1));
        } else {
            int nextVal = current + (scrollY > 0 ? step : -step);
            if (nextVal > maxCap) {
                delta = -1 - current;
            } else if (nextVal < 1) {
                delta = maxCap - current;
            } else {
                delta = scrollY > 0 ? step : -step;
            }
        }
        return delta;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (viewingSettings || viewingAuthority) {
            return super.mouseClicked(event, doubleClick);
        }

        if (pendingBurnSlot != -1) {
            if (this.confirmBurnBtn.mouseClicked(event, doubleClick)) return true;
            if (this.cancelBurnBtn.mouseClicked(event, doubleClick)) return true;
            return true;
        }

        int slot = getFilterSlotAt(event.x(), event.y());
        if (slot >= 0) {
            if (event.button() == 0 && isCtrlPressed()) {
                if (menu.getFilterItem(slot) != Items.AIR && menu.getFilterType(slot) == FilterType.EXCEPT) {
                    pendingBurnSlot = slot;
                    return true;
                }

                ClientPlayNetworking.send(new CycleFilterTypePayload(slot));
                return true;
            }

            if (event.button() == 0 || event.button() == 1) {
                ItemStack carried = menu.getCarried();
                int itemId = carried.isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(carried.getItem());
                ClientPlayNetworking.send(new SetFilterPayload(slot, itemId));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int getFilterSlotAt(double mouseX, double mouseY) {
        if (viewingSettings || viewingAuthority) return -1;

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
        // Deep full-screen Gaussian-style backdrop dim blur simulation overlay
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);

        boolean hideEverything = viewingSettings || viewingAuthority;
        int activeSlotCount = ChestLinkerGuiLayout.getActiveSlotCount(menu.getSlotCount(), hideEverything);
        int dynamicHeight = getDynamicHeight();

        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + dynamicHeight, 0xFF202020);

        if (viewingAuthority) {
            if (!authorityDataReceived && selectedAuthorityPlayer == null) {
                graphics.centeredText(
                        font,
                        Component.translatable("gui.chestsorter.loading").withStyle(ChatFormatting.GRAY),
                        leftPos + imageWidth / 2,
                        topPos + 50,
                        0xFFFFFFFF
                );
            }
            return;
        }

        if (viewingSettings) {
            return;
        }

        int filterPanelHeight = ChestLinkerGuiLayout.getFilterPanelHeight(activeSlotCount);
        int inventoryTop = topPos + ChestLinkerGuiLayout.getInventoryY(activeSlotCount);
        int inventoryHeight = ChestLinkerGuiLayout.getInventoryHeight();

        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + filterPanelHeight, 0xFF202020);
        graphics.fill(leftPos, inventoryTop, leftPos + imageWidth, inventoryTop + inventoryHeight, 0xFF202020);

        for (int i = 0; i < menu.getSlotCount(); i++) {
            Slot slot = this.menu.slots.get(i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;

            boolean isEmpty = menu.getFilterItem(i) == Items.AIR;
            FilterType type = menu.getFilterType(i);
            int colorOverlay = isEmpty
                    ? 0xFF383838
                    : (type != null ? type.getColorArgb() : 0xFF383838);

            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF101010);
            graphics.fill(x, y, x + 16, y + 16, colorOverlay);
        }

        for (int i = menu.getSlotCount(); i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF101010);
            graphics.fill(x, y, x + 16, y + 16, 0xFF383838);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Slot cachedSlot = this.hoveredSlot;

        if (viewingSettings || viewingAuthority || pendingBurnSlot != -1 || (cachedSlot != null && cachedSlot.index >= 0 && cachedSlot.index < menu.getSlotCount())) {
            this.hoveredSlot = null;
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (viewingAuthority) {
            if (selectedAuthorityPlayer == null) {
                for (int i = 0; i < authorityPlayers.size(); i++) {
                    AuthorityDataPayload.PlayerData player = authorityPlayers.get(i);
                    int x = leftPos + 12;
                    int y = topPos + 34 + i * 22;

                    ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                    var minecraftPlayer = Minecraft.getInstance().player;

                    if (minecraftPlayer != null && player.uuid().equals(minecraftPlayer.getUUID())) {
                        ResolvableProfile profile = ResolvableProfile.createResolved(minecraftPlayer.getGameProfile());
                        head.set(DataComponents.PROFILE, profile);
                    }
                    graphics.item(head, x, y);
                }
            }
            return;
        }

        if (viewingSettings) {
            if (authorityButton != null) {
                int x = authorityButton.getX() + 2;
                int y = authorityButton.getY() + 2;

                ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);
                var player = Minecraft.getInstance().player;

                if (player != null) {
                    ResolvableProfile profileComponent = ResolvableProfile.createResolved(player.getGameProfile());
                    headStack.set(DataComponents.PROFILE, profileComponent);
                }
                graphics.item(headStack, x, y);
            }
            return;
        }

        this.hoveredSlot = cachedSlot;
        int columns = ChestLinkerGuiLayout.FILTER_COLUMNS;
        int filterY = ChestLinkerGuiLayout.FILTER_ROW_Y;

        for (int i = 0; i < menu.getSlotCount(); i++) {
            if (menu.getFilterItem(i) == Items.AIR) continue;

            FilterType type = menu.getFilterType(i);
            int row = i / columns;
            int col = i % columns;
            int slotsInRow = Math.min(columns, menu.getSlotCount() - row * columns);
            int rowOffset = (columns - slotsInRow) * 9;

            int screenX = leftPos + 8 + rowOffset + col * 18;
            int screenY = topPos + filterY + row * 18;

            if (type == FilterType.ONLY) {
                int max = menu.getMaxCount(i);
                String text = (i == typingSlotIndex && !typingBuffer.isEmpty()) ? typingBuffer : (max == -1 ? "∞" : String.valueOf(max));
                int textColor = (i == typingSlotIndex && !typingBuffer.isEmpty()) ? 0xFFFFFF00 : 0xFFFFFFFF;
                int textWidth = font.width(text);
                graphics.text(font, text, screenX + 17 - textWidth, screenY + 9, textColor, true);
            } else if (type == FilterType.EXCEPT) {
                graphics.text(font, "EX", screenX + 2, screenY + 2, 0xFF55FF55, true);
            } else if (type == FilterType.BURN) {
                graphics.text(font, "🔥", screenX + 2, screenY + 2, 0xFFFF5555, true);
            }
        }

        boolean showModal = (pendingBurnSlot != -1);
        this.confirmBurnBtn.visible = showModal;
        this.confirmBurnBtn.active = showModal;
        this.cancelBurnBtn.visible = showModal;
        this.cancelBurnBtn.active = showModal;

        if (showModal) {
            graphics.fill(0, 0, this.width, this.height, 0xCC000000);

            int boxWidth = 240;
            int boxHeight = 86;
            int boxX = (this.width - boxWidth) / 2;
            int boxY = (this.topPos + (getDynamicHeight() - boxHeight) / 2);

            graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF202020);
            graphics.outline(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, 0xFFFF5555);

            Component titleComp = Component.translatable("gui.chestsorter.burn.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            Component msgComp = Component.translatable("gui.chestsorter.burn.message");
            Component subComp = Component.translatable("gui.chestsorter.burn.confirm_question").withStyle(ChatFormatting.ITALIC);

            graphics.centeredText(this.font, titleComp, boxX + boxWidth / 2, boxY + 14, 0xFFFFFFFF);
            graphics.centeredText(this.font, msgComp, boxX + boxWidth / 2, boxY + 32, 0xFFAAAAAA);
            graphics.centeredText(this.font, subComp, boxX + boxWidth / 2, boxY + 46, 0xFF888888);

            this.confirmBurnBtn.setY(boxY + 58);
            this.cancelBurnBtn.setY(boxY + 58);

            this.confirmBurnBtn.extractRenderState(graphics, mouseX, mouseY, delta);
            this.cancelBurnBtn.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    protected void extractTooltip(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (pendingBurnSlot != -1) return;

        int settingsBtnX = leftPos + imageWidth - 40;
        int settingsBtnY = topPos + 4;
        if (mouseX >= settingsBtnX && mouseX < settingsBtnX + 14 && mouseY >= settingsBtnY && mouseY < settingsBtnY + 14) {
            String label = viewingAuthority || viewingSettings ? "gui.chestsorter.return" : "gui.chestsorter.settings";
            graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable(label).withStyle(ChatFormatting.WHITE)), Optional.empty(), mouseX, mouseY, null);
            return;
        }

        int infoBtnX = leftPos + imageWidth - 22;
        int infoBtnY = topPos + 4;
        if (mouseX >= infoBtnX && mouseX < infoBtnX + 14 && mouseY >= infoBtnY && mouseY < infoBtnY + 14) {
            List<Component> infoTooltip = viewingSettings
                    ? List.of(
                    Component.translatable("gui.chestsorter.tooltip.settings_header").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Component.translatable("gui.chestsorter.tooltip.settings_authority_slot").withStyle(ChatFormatting.GRAY)
            )
                    : viewingAuthority
                    ? List.of(
                    Component.translatable("gui.chestsorter.tooltip.authority_header").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Component.translatable("gui.chestsorter.tooltip.authority_desc").withStyle(ChatFormatting.GRAY)
            )
                    : List.of(
                    Component.translatable("gui.chestsorter.tooltip.guide_header").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Component.translatable("gui.chestsorter.tooltip.guide_only").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.chestsorter.tooltip.guide_except").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.chestsorter.tooltip.guide_burn").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.chestsorter.tooltip.guide_scroll").withStyle(ChatFormatting.DARK_GRAY)
            );
            graphics.setTooltipForNextFrame(this.font, infoTooltip, Optional.empty(), mouseX, mouseY, null);
            return;
        }

        if (viewingSettings && authorityButton != null) {
            int x = authorityButton.getX();
            int y = authorityButton.getY();

            if (mouseX >= x && mouseX < x + authorityButton.getWidth()
                    && mouseY >= y && mouseY < y + authorityButton.getHeight()) {

                List<Component> authorityTooltip = List.of(
                        Component.translatable("gui.chestsorter.tooltip.authority_slot_header").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                        Component.translatable( "gui.chestsorter.tooltip.authority_slot_desc").withStyle(ChatFormatting.GRAY)
                );

                graphics.setTooltipForNextFrame(
                        this.font,
                        authorityTooltip,
                        Optional.empty(),
                        mouseX,
                        mouseY,
                        null
                );

                return;
            }
        } else if (this.hoveredSlot != null && this.hoveredSlot.index >= 0 && this.hoveredSlot.index < menu.getSlotCount()) {
            if (this.hoveredSlot.hasItem()) {
                ItemStack item = this.hoveredSlot.getItem();
                FilterType type = menu.getFilterType(this.hoveredSlot.index);

                List<Component> tooltip = new java.util.ArrayList<>();

                ChatFormatting typeColor = switch (type) {
                    case ONLY -> ChatFormatting.BLUE;
                    case EXCEPT -> ChatFormatting.GREEN;
                    case BURN -> ChatFormatting.RED;
                };

                Component typeHeader = Component.literal("[" + type.name() + "] ").withStyle(typeColor, ChatFormatting.BOLD)
                        .append(item.getHoverName().copy().withStyle(item.getRarity().color()));
                tooltip.add(typeHeader);

                tooltip.add(Component.translatable("gui.chestsorter.tooltip.cycle_mode").withStyle(ChatFormatting.GRAY));

                if (type == FilterType.ONLY) {
                    tooltip.add(Component.translatable("menu.chestsorter.tooltip.scroll").withStyle(ChatFormatting.YELLOW));
                    tooltip.add(Component.translatable("menu.chestsorter.tooltip.shift_scroll").withStyle(ChatFormatting.AQUA));
                } else if (type == FilterType.EXCEPT) {
                    boolean hasOnly = false;
                    for (int i = 0; i < menu.getSlotCount(); i++) {
                        if (menu.getFilterItem(i) != Items.AIR && menu.getFilterType(i) == FilterType.ONLY) {
                            hasOnly = true;
                            break;
                        }
                    }
                    if (hasOnly) {
                        tooltip.add(Component.translatable("gui.chestsorter.tooltip.except_warning").withStyle(ChatFormatting.YELLOW));
                    }
                } else {
                    tooltip.add(Component.translatable("gui.chestsorter.tooltip.destroys_warning").withStyle(ChatFormatting.RED));
                }

                graphics.setTooltipForNextFrame(this.font, tooltip, item.getTooltipImage(), mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE));
            }
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.@NonNull KeyEvent event) {
        if (pendingBurnSlot != -1) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                pendingBurnSlot = -1;
                return true;
            }
            return true;
        }

        double mouseX = minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();

        int slot = getFilterSlotAt(mouseX, mouseY);
        if (slot >= 0 && slot < menu.getSlotCount() && menu.getFilterItem(slot) != Items.AIR && menu.getFilterType(slot) == FilterType.ONLY) {
            int key = event.key();

            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && typingSlotIndex == slot) {
                commitTypedAmount();
                return true;
            }

            char digit = '\0';
            if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
                digit = (char) ('0' + (key - GLFW.GLFW_KEY_0));
            } else if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
                digit = (char) ('0' + (key - GLFW.GLFW_KEY_KP_0));
            }

            if (digit != '\0') {
                if (typingSlotIndex != slot) {
                    commitTypedAmount();
                    typingSlotIndex = slot;
                    typingBuffer = "";
                }

                if (typingBuffer.length() < 6) {
                    typingBuffer += digit;
                }
                return true;
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE && typingSlotIndex == slot && !typingBuffer.isEmpty()) {
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
                int maxCap = getMaxCapacityForSlot(typingSlotIndex);
                if (newValue > maxCap) newValue = maxCap;

                int currentValue = menu.getMaxCount(typingSlotIndex);
                int delta = newValue - currentValue;

                if (delta != 0) {
                    ClientPlayNetworking.send(new AdjustFilterMaxPayload(typingSlotIndex, delta));
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
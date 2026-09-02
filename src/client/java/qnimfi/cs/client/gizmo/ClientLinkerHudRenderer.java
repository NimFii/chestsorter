package qnimfi.cs.client.gizmo;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.NodeLinkData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ClientLinkerHudRenderer {

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("chestsorter", "linker_hud");
    private static BlockPos currentHoveredChest = null;

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, HUD_ID, ClientLinkerHudRenderer::renderHud);
    }

    public static List<LinkerArrowPathGizmo> getHoverGizmos(Level level) {
        List<LinkerArrowPathGizmo> gizmos = new ArrayList<>();
        if (currentHoveredChest == null || level == null) return gizmos;

        Set<BlockPos> targetPositions = new HashSet<>();
        targetPositions.add(currentHoveredChest);
        BlockState state = level.getBlockState(currentHoveredChest);

        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            targetPositions.add(currentHoveredChest.relative(ChestBlock.getConnectedDirection(state)));
        }

        int outboundColor = 0xCCFF8800;
        int inboundColor = 0xCC00AAFF;

        for (NodeLinkData node : ClientGizmoManager.getStoredNodes()) {
            boolean isHoveredSender = targetPositions.contains(node.sender());

            for (BlockPos receiver : node.receivers()) {
                boolean isHoveredReceiver = targetPositions.contains(receiver);

                if (isHoveredSender) {
                    gizmos.add(new LinkerArrowPathGizmo(node.sender(), receiver, outboundColor));
                } else if (isHoveredReceiver) {
                    gizmos.add(new LinkerArrowPathGizmo(node.sender(), receiver, inboundColor));
                }
            }
        }
        return gizmos;
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        currentHoveredChest = null;

        if (client.player == null || client.level == null) return;
        if (!client.player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.CHEST_LINKER)) return;

        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos targetPos = blockHit.getBlockPos();
        if (!(client.level.getBlockEntity(targetPos) instanceof ChestBlockEntity)) return;

        currentHoveredChest = targetPos;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        Optional<BlockPos> activeSenderOpt = ClientGizmoManager.getStoredActiveSender();

        if (activeSenderOpt.isPresent()) {
            BlockPos activeSender = activeSenderOpt.get();
            Component instruction = Component.translatable("hud.chestsorter.connection_mode");
            int textWidth = client.font.width(instruction);

            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight - 48;

            graphics.fill(x - 4, y - 4, x + textWidth + 4, y + 12, 0x80000000);
            graphics.text(client.font, instruction.getVisualOrderText(), x, y, 0xFFFFFFFF, false);

            BlockEntity blockEntity = client.level.getBlockEntity(targetPos);
            if (blockEntity instanceof ChestBlockEntity) {
                Component actionText;

                if (isTargetActiveSender(activeSender, targetPos, client.level)) {
                    actionText = Component.translatable("hud.chestsorter.action.finish");
                } else {
                    boolean isConnected = isConnectedToActiveSender(activeSender, targetPos, client.level);
                    actionText = isConnected ? Component.translatable("hud.chestsorter.action.disconnect") : Component.translatable("hud.chestsorter.action.connect");
                }

                int actionWidth = client.font.width(actionText);
                int crosshairX = (screenWidth - actionWidth) / 2;
                int crosshairY = (screenHeight / 2) + 12;

                graphics.fill(crosshairX - 4, crosshairY - 4, crosshairX + actionWidth + 4, crosshairY + 12, 0x80000000);
                graphics.text(client.font, actionText.getVisualOrderText(), crosshairX, crosshairY, 0xFFFFFFFF, false);
            }
        } else {
            ChestInfo info = getChestInfo(targetPos, client.level);
            List<Component> lines = info.getTooltipLines();
            List<LinkerSyncPayload.ClientFilterEntry> filters = getFiltersForChest(targetPos);

            boolean isCrouching = client.player.isCrouching();
            int maxWidth = 0;

            for (Component line : lines) {
                maxWidth = Math.max(maxWidth, client.font.width(line));
            }

            int rows = 1;
            if (!filters.isEmpty()) {
                if (isCrouching) {
                    rows = (int) Math.ceil((double) filters.size() / 9.0);
                    int iconsWidth = Math.min(filters.size(), 9) * 18;
                    maxWidth = Math.max(maxWidth, iconsWidth);
                } else {
                    int renderCount = Math.min(filters.size(), 3);
                    int iconsWidth = renderCount * 18 + (filters.size() > 3 ? 24 : 0);
                    maxWidth = Math.max(maxWidth, iconsWidth);
                }
            }

            int crosshairX = (screenWidth - maxWidth) / 2;
            int crosshairY = (screenHeight / 2) + 12;

            int totalHeight = lines.size() * 10;
            if (!filters.isEmpty()) {
                totalHeight += 4 + (isCrouching ? rows * 18 : 18);
            }

            graphics.fill(crosshairX - 6, crosshairY - 4, crosshairX + maxWidth + 6, crosshairY + totalHeight + 4, 0x80000000);

            int currentY = crosshairY;
            for (Component line : lines) {
                graphics.text(client.font, line.getVisualOrderText(), crosshairX, currentY, 0xFFFFFFFF, false);
                currentY += 10;
            }

            // Render Filter Visual Feedback
            if (!filters.isEmpty()) {
                currentY += 4;
                if (isCrouching) {
                    int col = 0;
                    int rowY = currentY;
                    for (LinkerSyncPayload.ClientFilterEntry filter : filters) {
                        if (col >= 9) {
                            col = 0;
                            rowY += 18;
                        }
                        int iconX = crosshairX + (col * 18);
                        int bgColor = getFilterTypeColor(filter.type());
                        graphics.fill(iconX, rowY, iconX + 16, rowY + 16, bgColor);
                        graphics.item(new ItemStack(filter.item()), iconX, rowY);
                        col++;
                    }
                } else {
                    int renderCount = Math.min(filters.size(), 3);
                    int iconX = crosshairX;
                    for (int i = 0; i < renderCount; i++) {
                        LinkerSyncPayload.ClientFilterEntry f = filters.get(i);
                        graphics.item(new ItemStack(f.item()), iconX, currentY);
                        iconX += 18;
                    }
                    if (filters.size() > 3) {
                        String overflowText = "+" + (filters.size() - 3);
                        graphics.text(client.font, overflowText, iconX + 2, currentY + 4, 0xFFFFAA00, false);
                    }
                }
            }
        }
    }

    private static int getFilterTypeColor(qnimfi.cs.FilterType type) {
        return switch (type) {
            case ONLY -> 0x555555FF;   // Semi-transparent Blue
            case EXCEPT -> 0x5555FF55; // Semi-transparent Green
            case BURN -> 0x55FF5555;   // Semi-transparent Red
        };
    }

    private static List<LinkerSyncPayload.ClientFilterEntry> getFiltersForChest(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return List.of();

        Set<BlockPos> targetPositions = new HashSet<>();
        targetPositions.add(pos);

        BlockState state = client.level.getBlockState(pos);
        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                targetPositions.add(pos.relative(ChestBlock.getConnectedDirection(state)));
            }
        }

        for (BlockPos targetPos : targetPositions) {
            List<LinkerSyncPayload.ClientFilterEntry> entries = ClientGizmoManager.getFilters(targetPos);
            if (entries != null && !entries.isEmpty()) {
                return entries;
            }
        }
        return List.of();
    }

    private static boolean isTargetActiveSender(BlockPos senderPos, BlockPos targetPos, net.minecraft.world.level.Level level) {
        if (senderPos.equals(targetPos)) return true;
        BlockState state = level.getBlockState(senderPos);
        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                BlockPos companionPos = senderPos.relative(ChestBlock.getConnectedDirection(state));
                return companionPos.equals(targetPos);
            }
        }
        return false;
    }

    private static boolean isConnectedToActiveSender(BlockPos senderPos, BlockPos targetPos, net.minecraft.world.level.Level level) {
        for (NodeLinkData node : ClientGizmoManager.getStoredNodes()) {
            if (node.sender().equals(senderPos)) {
                if (node.receivers().contains(targetPos)) return true;

                BlockState state = level.getBlockState(targetPos);
                if (state.hasProperty(ChestBlock.TYPE)) {
                    ChestType type = state.getValue(ChestBlock.TYPE);
                    if (type != ChestType.SINGLE) {
                        BlockPos companionPos = targetPos.relative(ChestBlock.getConnectedDirection(state));
                        if (node.receivers().contains(companionPos)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static ChestInfo getChestInfo(BlockPos targetPos, net.minecraft.world.level.Level level) {
        Set<BlockPos> targetPositions = new HashSet<>();
        targetPositions.add(targetPos);

        BlockState state = level.getBlockState(targetPos);
        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                targetPositions.add(targetPos.relative(ChestBlock.getConnectedDirection(state)));
            }
        }

        boolean isSender = false;
        boolean isReceiver = false;
        int receiverCount = 0;
        int senderCount = 0;

        List<NodeLinkData> nodes = ClientGizmoManager.getStoredNodes();
        for (NodeLinkData node : nodes) {
            if (targetPositions.contains(node.sender())) {
                isSender = true;
                receiverCount += node.receivers().size();
            }
            for (BlockPos rec : node.receivers()) {
                if (targetPositions.contains(rec)) {
                    isReceiver = true;
                    senderCount++;
                }
            }
        }

        return new ChestInfo(isSender, isReceiver, receiverCount, senderCount);
    }

    private record ChestInfo(boolean isSender, boolean isReceiver, int receiverCount, int senderCount) {
        public List<Component> getTooltipLines() {
            List<Component> lines = new ArrayList<>();

            if (!isSender && !isReceiver) {
                lines.add(Component.translatable("hud.chestsorter.type.unlinked"));
                lines.add(Component.translatable("hud.chestsorter.action.make_sender"));
                return lines;
            }

            if (isSender && isReceiver) {
                lines.add(Component.translatable("hud.chestsorter.type.both"));
            } else if (isSender) {
                lines.add(Component.translatable("hud.chestsorter.type.sender"));
            } else {
                lines.add(Component.translatable("hud.chestsorter.type.receiver"));
            }

            if (isSender) {
                lines.add(Component.translatable("hud.chestsorter.receivers_count", receiverCount));
            }
            if (isReceiver) {
                lines.add(Component.translatable("hud.chestsorter.senders_count", senderCount));
            }
            return lines;
        }
    }
}
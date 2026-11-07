package dev.autobuilder.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryHelper {
    private InventoryHelper() {}

    public static boolean hasItemForBlock(Block block) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return false;

        Item targetItem = block.asItem();
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                return true;
            }
        }
        return false;
    }

    public static int getAvailableCount(Block block) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return 0;

        Item targetItem = block.asItem();
        PlayerInventory inv = player.getInventory();
        int count = 0;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean prepareItemInHand(Block block) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return false;

        Item targetItem = block.asItem();
        PlayerInventory inv = player.getInventory();

        // 1. Check if already holding it
        if (player.getMainHandStack().getItem() == targetItem) {
            return true;
        }

        // 2. Check hotbar slots (0 to 8)
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                selectHotbarSlot(slot);
                return true;
            }
        }

        // 3. Check inventory slots (9 to 35) and swap to current hotbar slot
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                if (client.interactionManager != null) {
                    int currentHotbar = inv.getSelectedSlot();
                    client.interactionManager.clickSlot(
                            player.playerScreenHandler.syncId,
                            slot,
                            currentHotbar,
                            SlotActionType.SWAP,
                            player
                    );
                    return true;
                }
            }
        }

        return false;
    }

    public static void selectHotbarSlot(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || slot < 0 || slot >= 9) return;

        player.getInventory().setSelectedSlot(slot);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    public static int findBestToolSlot(BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return -1;

        PlayerInventory inv = player.getInventory();
        float bestSpeed = 1.0f;
        int bestSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty()) {
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = slot;
                }
            }
        }

        return bestSlot;
    }
}

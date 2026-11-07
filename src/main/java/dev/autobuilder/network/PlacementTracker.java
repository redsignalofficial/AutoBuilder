package dev.autobuilder.network;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PlacementTracker {
    private static final PlacementTracker INSTANCE = new PlacementTracker();

    public enum Status {
        PENDING,
        CONFIRMED,
        REJECTED
    }

    private static class PendingEntry {
        final BlockPos pos;
        final BlockState desiredState;
        int ageTicks;
        int retries;

        PendingEntry(BlockPos pos, BlockState desiredState) {
            this.pos = pos;
            this.desiredState = desiredState;
            this.ageTicks = 0;
            this.retries = 0;
        }
    }

    private final Map<BlockPos, PendingEntry> pendingPlacements = new HashMap<>();

    private PlacementTracker() {}

    public static PlacementTracker getInstance() {
        return INSTANCE;
    }

    public void reset() {
        pendingPlacements.clear();
    }

    public void recordPlacement(BlockPos pos, BlockState state) {
        PendingEntry entry = pendingPlacements.get(pos);
        if (entry == null) {
            entry = new PendingEntry(pos, state);
            pendingPlacements.put(pos, entry);
        } else {
            entry.retries++;
            entry.ageTicks = 0;
        }
    }

    public Status checkStatus(BlockPos pos, BlockState desiredState) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return Status.PENDING;

        BlockState currentState = world.getBlockState(pos);
        if (currentState.getBlock() == desiredState.getBlock()) {
            pendingPlacements.remove(pos);
            return Status.CONFIRMED;
        }

        PendingEntry entry = pendingPlacements.get(pos);
        if (entry != null) {
            if (entry.retries >= 3) {
                pendingPlacements.remove(pos);
                return Status.REJECTED;
            }
        }

        return Status.PENDING;
    }

    public void tick() {
        Iterator<Map.Entry<BlockPos, PendingEntry>> it = pendingPlacements.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, PendingEntry> e = it.next();
            e.getValue().ageTicks++;
            if (e.getValue().ageTicks > 40) {
                // Timeout after 2 seconds
                it.remove();
            }
        }
    }
}

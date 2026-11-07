package dev.autobuilder.building;

import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.util.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BuildOrderCalculator {

    public static List<BuildTask> calculateBuildOrder(Schematic schematic, BlockPos origin) {
        List<BuildTask> tasks = new ArrayList<>();
        if (schematic == null) return tasks;

        int width = schematic.getWidth();
        int height = schematic.getHeight();
        int length = schematic.getLength();

        // 1. Collect all non-air tasks
        for (int y = 0; y < height; y++) {
            List<BuildTask> layerNormal = new ArrayList<>();
            List<BuildTask> layerDependent = new ArrayList<>();
            List<BuildTask> layerGravity = new ArrayList<>();

            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = schematic.getBlockState(x, y, z);
                    if (state.isAir()) continue;

                    BlockPos worldPos = origin.add(x, y, z);
                    BuildTask task = new BuildTask(worldPos, state, y);

                    if (task.isGravityBlock()) {
                        layerGravity.add(task);
                    } else if (task.isRequiresSupport()) {
                        layerDependent.add(task);
                    } else {
                        layerNormal.add(task);
                    }
                }
            }

            // Normal solid blocks first, then gravity blocks, then support-dependent blocks (torches, etc.)
            tasks.addAll(layerNormal);
            tasks.addAll(layerGravity);
            tasks.addAll(layerDependent);
        }

        // Sort by distance from player within each layer to make camera and movement natural
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null) {
            BlockPos playerPos = player.getBlockPos();
            tasks.sort(Comparator.comparingInt(BuildTask::getLayerY)
                    .thenComparingDouble(task -> task.getWorldPos().getSquaredDistance(playerPos)));
        }

        return tasks;
    }
}

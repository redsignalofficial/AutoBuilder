package dev.autobuilder.building;

import dev.autobuilder.schematic.Schematic;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BuildOrderCalculator {

    public static List<BuildTask> calculateBuildOrder(Schematic schematic, BlockPos origin) {
        List<BuildTask> fullTaskList = new ArrayList<>();
        if (schematic == null || origin == null) return fullTaskList;

        int width = schematic.getWidth();
        int height = schematic.getHeight();
        int length = schematic.getLength();

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        BlockPos currentRefPos = player != null ? player.getBlockPos() : origin;

        // Process layer by layer from bottom to top
        for (int y = 0; y < height; y++) {
            List<BuildTask> layerTasks = new ArrayList<>();

            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = schematic.getBlockState(x, y, z);
                    if (state.isAir()) continue;

                    BlockPos worldPos = origin.add(x, y, z);
                    layerTasks.add(new BuildTask(worldPos, state, y));
                }
            }

            if (layerTasks.isEmpty()) continue;

            // U2: Reach-Cluster ordering
            // Group blocks into spatial bins of 4x4 (reach radius ~ 3.5 blocks)
            Map<Long, List<BuildTask>> clusterMap = new HashMap<>();
            for (BuildTask task : layerTasks) {
                int binX = task.getWorldPos().getX() >> 2;
                int binZ = task.getWorldPos().getZ() >> 2;
                long key = (((long) binX) << 32) | (binZ & 0xFFFFFFFFL);
                clusterMap.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
            }

            // Order clusters by greedy nearest-neighbor to minimize player movement
            List<List<BuildTask>> orderedClusters = new ArrayList<>();
            List<List<BuildTask>> remainingClusters = new ArrayList<>(clusterMap.values());

            BlockPos activePos = currentRefPos;
            while (!remainingClusters.isEmpty()) {
                final BlockPos ref = activePos;
                remainingClusters.sort(Comparator.comparingDouble(c -> getClusterCentroid(c).getSquaredDistance(ref)));
                List<BuildTask> nearest = remainingClusters.remove(0);
                orderedClusters.add(nearest);
                activePos = getClusterCentroid(nearest);
            }

            // Within each cluster:
            // 1. Calculate recommended stand position (U1)
            // 2. Sort: solid base blocks first, gravity second, support-dependent third
            int layerWorldY = origin.getY() + y;
            int recommendedY = Math.max(-64, layerWorldY - 1);

            for (List<BuildTask> cluster : orderedClusters) {
                BlockPos centroid = getClusterCentroid(cluster);
                BlockPos standPos = new BlockPos(centroid.getX(), recommendedY, centroid.getZ());

                List<BuildTask> solid = new ArrayList<>();
                List<BuildTask> gravity = new ArrayList<>();
                List<BuildTask> dependent = new ArrayList<>();

                for (BuildTask task : cluster) {
                    task.setRecommendedStandPos(standPos);
                    if (task.isGravityBlock()) {
                        gravity.add(task);
                    } else if (task.isRequiresSupport()) {
                        dependent.add(task);
                    } else {
                        solid.add(task);
                    }
                }

                // Sort solid by distance to standPos
                solid.sort(Comparator.comparingDouble(t -> t.getWorldPos().getSquaredDistance(standPos)));

                fullTaskList.addAll(solid);
                fullTaskList.addAll(gravity);
                fullTaskList.addAll(dependent);
            }

            if (!orderedClusters.isEmpty()) {
                currentRefPos = activePos;
            }
        }

        return fullTaskList;
    }

    private static BlockPos getClusterCentroid(List<BuildTask> cluster) {
        if (cluster.isEmpty()) return BlockPos.ORIGIN;
        long sumX = 0;
        long sumY = 0;
        long sumZ = 0;
        for (BuildTask t : cluster) {
            sumX += t.getWorldPos().getX();
            sumY += t.getWorldPos().getY();
            sumZ += t.getWorldPos().getZ();
        }
        int count = cluster.size();
        return new BlockPos((int) (sumX / count), (int) (sumY / count), (int) (sumZ / count));
    }
}

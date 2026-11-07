package dev.autobuilder.building;

import dev.autobuilder.interaction.InventoryHelper;
import dev.autobuilder.interaction.RaycastHelper;
import dev.autobuilder.obstacle.ObstacleDetector;
import dev.autobuilder.obstacle.ObstaclePolicy;
import dev.autobuilder.util.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class PlacementValidator {

    public enum ValidationResult {
        VALID,
        ALREADY_PLACED,
        NEEDS_OBSTACLE_REMOVAL,
        OBSTACLE_FORBIDDEN,
        MISSING_SUPPORT,
        OUT_OF_REACH,
        MISSING_MATERIAL,
        CHUNK_NOT_LOADED
    }

    public static ValidationResult validate(BuildTask task) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return ValidationResult.OUT_OF_REACH;

        BlockPos pos = task.getWorldPos();

        // U4: Check if chunk is loaded
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return ValidationResult.CHUNK_NOT_LOADED;
        }

        BlockState currentState = world.getBlockState(pos);
        BlockState desiredState = task.getDesiredState();

        // Check if already placed
        if (currentState.getBlock() == desiredState.getBlock()) {
            return ValidationResult.ALREADY_PLACED;
        }

        // Check obstacle
        if (ObstacleDetector.isObstacle(world, pos, desiredState)) {
            if (ObstaclePolicy.canBreakObstacle(world, pos)) {
                return ValidationResult.NEEDS_OBSTACLE_REMOVAL;
            } else {
                return ValidationResult.OBSTACLE_FORBIDDEN;
            }
        }

        // Check gravity block support
        if (task.isGravityBlock()) {
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);
            if (belowState.isAir() || belowState.isLiquid() || belowState.isReplaceable()) {
                return ValidationResult.MISSING_SUPPORT;
            }
        }

        // Check support for dependent blocks
        if (task.isRequiresSupport()) {
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);
            if (belowState.isAir()) {
                return ValidationResult.MISSING_SUPPORT;
            }
        }

        // Check inventory
        if (!InventoryHelper.hasItemForBlock(desiredState.getBlock())) {
            return ValidationResult.MISSING_MATERIAL;
        }

        // Check reach & raycast
        BlockHitResult hit = RaycastHelper.findPlacementHitResult(pos);
        if (hit == null) {
            return ValidationResult.OUT_OF_REACH;
        }

        return ValidationResult.VALID;
    }
}

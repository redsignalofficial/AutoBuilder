package dev.autobuilder.interaction;

import dev.autobuilder.building.BuildTask;
import dev.autobuilder.camera.CameraController;
import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.config.ModConfig;
import dev.autobuilder.network.PlacementTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles automated and manual climbing using pillar/scaffolding blocks,
 * keeping a climb log for automatic cleanup.
 */
public class ClimbController {
    private static final ClimbController INSTANCE = new ClimbController();

    private final List<BlockPos> climbLog = new ArrayList<>();
    private int climbCooldown = 0;
    private int cleanupCooldown = 0;
    private BlockPos currentMiningPos = null;

    private ClimbController() {}

    public static ClimbController getInstance() {
        return INSTANCE;
    }

    public List<BlockPos> getClimbLog() {
        return climbLog;
    }

    public void setClimbLog(List<BlockPos> log) {
        this.climbLog.clear();
        if (log != null) {
            this.climbLog.addAll(log);
        }
    }

    public void clearClimbLog() {
        this.climbLog.clear();
        this.currentMiningPos = null;
    }

    public boolean isClimbNeeded(BuildTask task, ClientPlayerEntity player) {
        if (task == null || player == null) return false;
        double eyeY = player.getEyeY();
        double taskY = task.getWorldPos().getY() + 0.5;
        // Reach limit check: if target is more than 3.0 blocks above eye level
        return (taskY - eyeY) > 3.0;
    }

    public boolean tickClimb(BuildTask targetTask) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (player == null || world == null || interactionManager == null) {
            return false;
        }

        if (climbCooldown > 0) {
            climbCooldown--;
            return false;
        }

        ModConfig config = ConfigManager.get();
        Block climbBlock = InventoryHelper.findClimbingBlock(config.climbBlock);
        if (climbBlock == null) {
            return false;
        }

        if (!InventoryHelper.prepareItemInHand(climbBlock)) {
            return false;
        }

        BlockPos playerPos = player.getBlockPos();
        BlockPos belowPos = playerPos.down();

        // Direct camera straight down to place under feet
        Vec3d lookTarget = Vec3d.ofCenter(belowPos);
        CameraController.getInstance().setTarget(lookTarget);

        if (!CameraController.getInstance().isAligned()) {
            return false;
        }

        // Handle Jump
        if (player.isOnGround()) {
            if (config.autoJump) {
                player.jump();
            }
            return false;
        }

        // When in air and clearance below is sufficient
        if (player.getY() > playerPos.getY() + 0.25) {
            BlockPos placePos = playerPos;
            BlockState placeState = world.getBlockState(placePos);

            if (placeState.isAir() || placeState.isReplaceable()) {
                Vec3d hitVec = new Vec3d(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
                BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

                ActionResult result = interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                player.swingHand(Hand.MAIN_HAND);

                if (result.isAccepted()) {
                    climbLog.add(placePos);
                    PlacementTracker.getInstance().recordPlacement(placePos, climbBlock.getDefaultState());
                    climbCooldown = config.placementDelayTicks + 2;
                    return true;
                }
            }
        }

        return false;
    }

    public boolean tickCleanup() {
        if (climbLog.isEmpty()) {
            currentMiningPos = null;
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (player == null || world == null || interactionManager == null) {
            return false;
        }

        if (cleanupCooldown > 0) {
            cleanupCooldown--;
            return false;
        }

        // Clean from top down (last in log)
        BlockPos targetPos = climbLog.get(climbLog.size() - 1);
        BlockState state = world.getBlockState(targetPos);

        if (state.isAir() || state.isReplaceable()) {
            climbLog.remove(climbLog.size() - 1);
            currentMiningPos = null;
            return climbLog.isEmpty();
        }

        // Equip tool
        int bestSlot = InventoryHelper.findBestToolSlot(state);
        if (bestSlot != -1) {
            InventoryHelper.selectHotbarSlot(bestSlot);
        }

        // Look at block
        CameraController.getInstance().setTarget(Vec3d.ofCenter(targetPos));
        if (!CameraController.getInstance().isAligned()) {
            return false;
        }

        Direction side = Direction.UP;
        if (currentMiningPos == null || !currentMiningPos.equals(targetPos)) {
            currentMiningPos = targetPos;
            interactionManager.attackBlock(targetPos, side);
        } else {
            player.swingHand(Hand.MAIN_HAND);
            boolean broken = interactionManager.updateBlockBreakingProgress(targetPos, side);
            if (broken || world.getBlockState(targetPos).isAir()) {
                climbLog.remove(climbLog.size() - 1);
                currentMiningPos = null;
                cleanupCooldown = ConfigManager.get().miningDelayTicks;
            }
        }

        return climbLog.isEmpty();
    }
}

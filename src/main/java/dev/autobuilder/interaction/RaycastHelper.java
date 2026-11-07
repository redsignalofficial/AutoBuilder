package dev.autobuilder.interaction;

import dev.autobuilder.config.ConfigManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class RaycastHelper {
    private RaycastHelper() {}

    public static BlockHitResult findPlacementHitResult(BlockPos targetPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return null;

        Vec3d eyePos = player.getEyePos();
        double maxReach = ConfigManager.get().maxBuildReach;
        double maxReachSq = maxReach * maxReach;

        // Try adjacent directions to find a solid supporting neighbor block
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = targetPos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            if (!neighborState.isAir() && !neighborState.isLiquid() && !neighborState.isReplaceable()) {
                Direction faceToClick = dir.getOpposite();
                Vec3d hitVec = getFaceCenter(neighborPos, faceToClick);

                if (eyePos.squaredDistanceTo(hitVec) <= maxReachSq) {
                    // Check line of sight
                    RaycastContext context = new RaycastContext(
                            eyePos,
                            hitVec,
                            RaycastContext.ShapeType.OUTLINE,
                            RaycastContext.FluidHandling.NONE,
                            player
                    );
                    BlockHitResult hit = world.raycast(context);
                    if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(neighborPos)) {
                        return new BlockHitResult(hitVec, faceToClick, neighborPos, false);
                    }
                }
            }
        }

        // Direct raycast check if target itself can be reached
        Vec3d center = Vec3d.ofCenter(targetPos);
        if (eyePos.squaredDistanceTo(center) <= maxReachSq) {
            return new BlockHitResult(center, Direction.UP, targetPos, false);
        }

        return null;
    }

    public static Direction findBestMiningSide(ClientPlayerEntity player, BlockPos pos) {
        Vec3d eyePos = player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d diff = eyePos.subtract(blockCenter);

        Direction best = Direction.UP;
        double maxDot = -Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            Vec3d normal = Vec3d.of(dir.getVector());
            double dot = diff.normalize().dotProduct(normal);
            if (dot > maxDot) {
                maxDot = dot;
                best = dir;
            }
        }
        return best;
    }

    private static Vec3d getFaceCenter(BlockPos pos, Direction face) {
        Vec3d center = Vec3d.ofCenter(pos);
        return center.add(Vec3d.of(face.getVector()).multiply(0.5));
    }
}

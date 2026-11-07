package dev.autobuilder.obstacle;

import dev.autobuilder.util.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ObstacleDetector {

    public static boolean isObstacle(World world, BlockPos pos, BlockState desiredState) {
        BlockState currentState = world.getBlockState(pos);

        if (currentState.isAir() || currentState.isReplaceable()) {
            return false;
        }

        if (currentState.equals(desiredState)) {
            return false;
        }

        return true;
    }
}

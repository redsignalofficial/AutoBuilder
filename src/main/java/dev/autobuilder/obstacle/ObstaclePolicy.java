package dev.autobuilder.obstacle;

import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.config.ModConfig;
import dev.autobuilder.util.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class ObstaclePolicy {

    public static boolean canBreakObstacle(World world, BlockPos pos) {
        ModConfig config = ConfigManager.get();
        if (!config.breakObstacles) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }

        if (BlockUtils.isUnbreakable(state)) {
            return false;
        }

        if (BlockUtils.isValuableContainer(state, world, pos)) {
            if (!config.breakContainers) {
                return false;
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return false;
        }

        if (client.interactionManager != null) {
            GameMode gameMode = client.interactionManager.getCurrentGameMode();
            if (gameMode == GameMode.ADVENTURE || gameMode == GameMode.SPECTATOR) {
                return false;
            }
        }

        return true;
    }
}

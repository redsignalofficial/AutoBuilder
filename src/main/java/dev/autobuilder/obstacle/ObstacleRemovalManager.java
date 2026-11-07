package dev.autobuilder.obstacle;

import dev.autobuilder.camera.CameraController;
import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.interaction.InventoryHelper;
import dev.autobuilder.interaction.RaycastHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ObstacleRemovalManager {
    private static final ObstacleRemovalManager INSTANCE = new ObstacleRemovalManager();

    private BlockPos currentMiningPos = null;
    private int miningTicks = 0;
    private int cooldownTicks = 0;

    private ObstacleRemovalManager() {}

    public static ObstacleRemovalManager getInstance() {
        return INSTANCE;
    }

    public void reset() {
        this.currentMiningPos = null;
        this.miningTicks = 0;
        this.cooldownTicks = 0;
    }

    public boolean tickMining(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (player == null || world == null || interactionManager == null) {
            return false;
        }

        if (world.getBlockState(pos).isAir() || world.getBlockState(pos).isReplaceable()) {
            reset();
            return true;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        BlockState state = world.getBlockState(pos);
        if (!ObstaclePolicy.canBreakObstacle(world, pos)) {
            return false;
        }

        int bestSlot = InventoryHelper.findBestToolSlot(state);
        if (bestSlot != -1) {
            InventoryHelper.selectHotbarSlot(bestSlot);
        }

        Direction side = RaycastHelper.findBestMiningSide(player, pos);
        Vec3d targetHit = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5));
        CameraController.getInstance().setTarget(targetHit);

        if (!CameraController.getInstance().isAligned()) {
            return false;
        }

        if (currentMiningPos == null || !currentMiningPos.equals(pos)) {
            currentMiningPos = pos;
            miningTicks = 0;
            interactionManager.attackBlock(pos, side);
        } else {
            miningTicks++;
            player.swingHand(Hand.MAIN_HAND);
            boolean broken = interactionManager.updateBlockBreakingProgress(pos, side);
            if (broken || world.getBlockState(pos).isAir()) {
                cooldownTicks = ConfigManager.get().miningDelayTicks;
                reset();
                return true;
            }
        }

        return false;
    }
}

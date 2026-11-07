package dev.autobuilder.building;

import dev.autobuilder.util.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BuildTask {
    private final BlockPos worldPos;
    private final BlockState desiredState;
    private final int layerY;
    private final boolean gravityBlock;
    private final boolean requiresSupport;
    private BlockPos recommendedStandPos;

    public BuildTask(BlockPos worldPos, BlockState desiredState, int layerY) {
        this.worldPos = worldPos;
        this.desiredState = desiredState;
        this.layerY = layerY;
        this.gravityBlock = BlockUtils.isGravityBlock(desiredState);
        this.requiresSupport = BlockUtils.requiresSupport(desiredState);
    }

    public BlockPos getWorldPos() {
        return worldPos;
    }

    public BlockState getDesiredState() {
        return desiredState;
    }

    public int getLayerY() {
        return layerY;
    }

    public boolean isGravityBlock() {
        return gravityBlock;
    }

    public boolean isRequiresSupport() {
        return requiresSupport;
    }

    public BlockPos getRecommendedStandPos() {
        return recommendedStandPos;
    }

    public void setRecommendedStandPos(BlockPos recommendedStandPos) {
        this.recommendedStandPos = recommendedStandPos;
    }
}

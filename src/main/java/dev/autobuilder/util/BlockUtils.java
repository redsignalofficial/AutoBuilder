package dev.autobuilder.util;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockUtils {
    private BlockUtils() {}

    public static boolean isAirOrReplaceable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.isLiquid() || state.isReplaceable();
    }

    public static boolean isGravityBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FallingBlock;
    }

    public static boolean requiresSupport(BlockState state) {
        Block block = state.getBlock();
        return block instanceof TorchBlock
                || block instanceof WallTorchBlock
                || block instanceof RedstoneWireBlock
                || block instanceof RepeaterBlock
                || block instanceof ComparatorBlock
                || block instanceof CarpetBlock
                || block instanceof FlowerBlock
                || block instanceof SaplingBlock
                || block instanceof DoorBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof AbstractRailBlock
                || block instanceof AbstractPressurePlateBlock;
    }

    public static boolean isValuableContainer(BlockState state, World world, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof ChestBlock
                || block instanceof TrappedChestBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof BarrelBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof HopperBlock
                || block instanceof EnderChestBlock
                || block instanceof AbstractFurnaceBlock) {
            return true;
        }
        BlockEntity be = world.getBlockEntity(pos);
        return be != null;
    }

    public static boolean isUnbreakable(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BEDROCK
                || block == Blocks.BARRIER
                || block == Blocks.END_PORTAL_FRAME
                || block == Blocks.END_PORTAL
                || block == Blocks.NETHER_PORTAL
                || block == Blocks.COMMAND_BLOCK
                || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK
                || block == Blocks.STRUCTURE_BLOCK
                || block == Blocks.JIGSAW;
    }

    public static String getBlockName(BlockState state) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return id.getPath().replace('_', ' ');
    }
}

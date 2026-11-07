package dev.autobuilder.selection;

import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.schematic.Schematic;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class SelectionManager {
    private static final SelectionManager INSTANCE = new SelectionManager();

    private BlockPos pos1;
    private BlockPos pos2;
    private CuboidRegion region;

    private SelectionManager() {}

    public static SelectionManager getInstance() {
        return INSTANCE;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
        updateRegion();
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
        updateRegion();
    }

    public CuboidRegion getRegion() {
        return region;
    }

    public boolean hasCompleteSelection() {
        return pos1 != null && pos2 != null && region != null;
    }

    public void clear() {
        this.pos1 = null;
        this.pos2 = null;
        this.region = null;
    }

    private void updateRegion() {
        if (pos1 != null && pos2 != null) {
            CuboidRegion newRegion = new CuboidRegion(pos1, pos2);
            long volume = newRegion.getVolume();
            int maxBlocks = ConfigManager.get().maxSelectionBlocks;
            if (volume <= maxBlocks) {
                this.region = newRegion;
            } else {
                this.region = null;
            }
        } else {
            this.region = null;
        }
    }

    public Schematic copySelection() {
        if (!hasCompleteSelection()) {
            return null;
        }
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        }

        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        Schematic schematic = new Schematic("selection", width, height, length);
        BlockPos min = region.getMin();

        for (BlockPos pos : region) {
            BlockState state = world.getBlockState(pos);
            int relX = pos.getX() - min.getX();
            int relY = pos.getY() - min.getY();
            int relZ = pos.getZ() - min.getZ();

            schematic.setBlockState(relX, relY, relZ, state);

            BlockEntity be = world.getBlockEntity(pos);
            if (be != null) {
                try {
                    NbtCompound nbt = be.createNbt(world.getRegistryManager());
                    schematic.setBlockEntityNbt(relX, relY, relZ, nbt);
                } catch (Exception ignored) {}
            }
        }

        return schematic;
    }
}

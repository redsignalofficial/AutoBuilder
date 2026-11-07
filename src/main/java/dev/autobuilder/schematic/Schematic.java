package dev.autobuilder.schematic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class Schematic {
    private String name;
    private int width;
    private int height;
    private int length;
    private BlockState[] blocks;
    private final Map<BlockPos, NbtCompound> blockEntities;
    private BlockPos origin = BlockPos.ORIGIN;

    public Schematic(String name, int width, int height, int length) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = new BlockState[width * height * length];
        this.blockEntities = new HashMap<>();
        Arrays.fill(this.blocks, Blocks.AIR.getDefaultState());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public void setOrigin(BlockPos origin) {
        this.origin = origin;
    }

    private int getIndex(int x, int y, int z) {
        return (y * length + z) * width + x;
    }

    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < length;
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return Blocks.AIR.getDefaultState();
        }
        BlockState state = blocks[getIndex(x, y, z)];
        return state != null ? state : Blocks.AIR.getDefaultState();
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        if (isInBounds(x, y, z)) {
            blocks[getIndex(x, y, z)] = state != null ? state : Blocks.AIR.getDefaultState();
        }
    }

    public NbtCompound getBlockEntityNbt(int x, int y, int z) {
        return blockEntities.get(new BlockPos(x, y, z));
    }

    public void setBlockEntityNbt(int x, int y, int z, NbtCompound nbt) {
        if (isInBounds(x, y, z)) {
            if (nbt != null) {
                blockEntities.put(new BlockPos(x, y, z), nbt);
            } else {
                blockEntities.remove(new BlockPos(x, y, z));
            }
        }
    }

    public Map<BlockPos, NbtCompound> getBlockEntities() {
        return Collections.unmodifiableMap(blockEntities);
    }

    public int getNonAirBlockCount() {
        int count = 0;
        for (BlockState state : blocks) {
            if (state != null && !state.isAir()) {
                count++;
            }
        }
        return count;
    }

    public Map<Block, Integer> getMaterialCounts() {
        Map<Block, Integer> counts = new HashMap<>();
        for (BlockState state : blocks) {
            if (state != null && !state.isAir()) {
                counts.put(state.getBlock(), counts.getOrDefault(state.getBlock(), 0) + 1);
            }
        }
        return counts;
    }

    public void rotate(BlockRotation rotation) {
        if (rotation == BlockRotation.NONE) return;

        int newWidth = (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) ? length : width;
        int newLength = (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) ? width : length;
        BlockState[] newBlocks = new BlockState[newWidth * height * newLength];
        Arrays.fill(newBlocks, Blocks.AIR.getDefaultState());

        Map<BlockPos, NbtCompound> newEntities = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = getBlockState(x, y, z);
                    if (state.isAir()) continue;

                    BlockState rotatedState = state.rotate(rotation);
                    int newX;
                    int newZ;

                    switch (rotation) {
                        case CLOCKWISE_90 -> {
                            newX = length - 1 - z;
                            newZ = x;
                        }
                        case CLOCKWISE_180 -> {
                            newX = width - 1 - x;
                            newZ = length - 1 - z;
                        }
                        case COUNTERCLOCKWISE_90 -> {
                            newX = z;
                            newZ = width - 1 - x;
                        }
                        default -> {
                            newX = x;
                            newZ = z;
                        }
                    }

                    int newIdx = (y * newLength + newZ) * newWidth + newX;
                    newBlocks[newIdx] = rotatedState;

                    NbtCompound nbt = getBlockEntityNbt(x, y, z);
                    if (nbt != null) {
                        newEntities.put(new BlockPos(newX, y, newZ), nbt);
                    }
                }
            }
        }

        this.width = newWidth;
        this.length = newLength;
        this.blocks = newBlocks;
        this.blockEntities.clear();
        this.blockEntities.putAll(newEntities);
    }

    public void mirror(BlockMirror mirror) {
        if (mirror == BlockMirror.NONE) return;

        BlockState[] newBlocks = new BlockState[width * height * length];
        Arrays.fill(newBlocks, Blocks.AIR.getDefaultState());

        Map<BlockPos, NbtCompound> newEntities = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = getBlockState(x, y, z);
                    if (state.isAir()) continue;

                    BlockState mirroredState = state.mirror(mirror);
                    int newX = (mirror == BlockMirror.FRONT_BACK) ? width - 1 - x : x;
                    int newZ = (mirror == BlockMirror.LEFT_RIGHT) ? length - 1 - z : z;

                    int newIdx = (y * length + newZ) * width + newX;
                    newBlocks[newIdx] = mirroredState;

                    NbtCompound nbt = getBlockEntityNbt(x, y, z);
                    if (nbt != null) {
                        newEntities.put(new BlockPos(newX, y, newZ), nbt);
                    }
                }
            }
        }

        this.blocks = newBlocks;
        this.blockEntities.clear();
        this.blockEntities.putAll(newEntities);
    }
}

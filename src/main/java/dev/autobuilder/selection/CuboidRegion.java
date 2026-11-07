package dev.autobuilder.selection;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CuboidRegion implements Iterable<BlockPos> {
    private final BlockPos min;
    private final BlockPos max;

    public CuboidRegion(BlockPos p1, BlockPos p2) {
        int minX = Math.min(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());

        int maxX = Math.max(p1.getX(), p2.getX());
        int maxY = Math.max(p1.getY(), p2.getY());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        this.min = new BlockPos(minX, minY, minZ);
        this.max = new BlockPos(maxX, maxY, maxZ);
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public int getWidth() {
        return max.getX() - min.getX() + 1;
    }

    public int getHeight() {
        return max.getY() - min.getY() + 1;
    }

    public int getLength() {
        return max.getZ() - min.getZ() + 1;
    }

    public long getVolume() {
        return (long) getWidth() * getHeight() * getLength();
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public Box toBoundingBox() {
        return new Box(min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            private int x = min.getX();
            private int y = min.getY();
            private int z = min.getZ();

            @Override
            public boolean hasNext() {
                return y <= max.getY();
            }

            @Override
            public BlockPos next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                BlockPos current = new BlockPos(x, y, z);
                x++;
                if (x > max.getX()) {
                    x = min.getX();
                    z++;
                    if (z > max.getZ()) {
                        z = min.getZ();
                        y++;
                    }
                }
                return current;
            }
        };
    }
}

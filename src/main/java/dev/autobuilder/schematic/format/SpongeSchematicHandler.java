package dev.autobuilder.schematic.format;

import dev.autobuilder.AutoBuilder;
import dev.autobuilder.schematic.Schematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpongeSchematicHandler implements ISchematicHandler {

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".schem");
    }

    @Override
    public Schematic read(File file) throws IOException {
        NbtCompound root = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimited());
        NbtCompound data = root.contains("Schematic") ? root.getCompound("Schematic") : root;

        int width = data.getShort("Width") & 0xFFFF;
        int height = data.getShort("Height") & 0xFFFF;
        int length = data.getShort("Length") & 0xFFFF;

        String name = file.getName().replaceAll("(?i)\\.schem$", "");
        Schematic schematic = new Schematic(name, width, height, length);

        NbtCompound paletteCompound = data.getCompound("Palette");
        Map<Integer, BlockState> paletteMap = new HashMap<>();

        for (String key : paletteCompound.getKeys()) {
            int id = paletteCompound.getInt(key);
            BlockState state = parseBlockStateString(key);
            paletteMap.put(id, state);
        }

        byte[] blockData = data.getByteArray("BlockData");
        int blockIndex = 0;
        int byteIndex = 0;
        int totalBlocks = width * height * length;

        while (byteIndex < blockData.length && blockIndex < totalBlocks) {
            int value = 0;
            int shift = 0;
            while (true) {
                if (byteIndex >= blockData.length) break;
                byte b = blockData[byteIndex++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }

            int x = blockIndex % width;
            int z = (blockIndex / width) % length;
            int y = blockIndex / (width * length);

            BlockState state = paletteMap.getOrDefault(value, Blocks.AIR.getDefaultState());
            schematic.setBlockState(x, y, z, state);
            blockIndex++;
        }

        if (data.contains("BlockEntities", NbtElement.LIST_TYPE)) {
            NbtList list = data.getList("BlockEntities", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound beNbt = list.getCompound(i);
                if (beNbt.contains("Pos", NbtElement.INT_ARRAY_TYPE)) {
                    int[] pos = beNbt.getIntArray("Pos");
                    if (pos.length == 3) {
                        schematic.setBlockEntityNbt(pos[0], pos[1], pos[2], beNbt.copy());
                    }
                }
            }
        }

        return schematic;
    }

    @Override
    public void write(Schematic schematic, File file) throws IOException {
        NbtCompound root = new NbtCompound();
        NbtCompound data = new NbtCompound();

        data.putInt("Version", 2);
        data.putInt("DataVersion", 3955);
        data.putShort("Width", (short) schematic.getWidth());
        data.putShort("Height", (short) schematic.getHeight());
        data.putShort("Length", (short) schematic.getLength());

        Map<BlockState, Integer> stateToId = new LinkedHashMap<>();
        stateToId.put(Blocks.AIR.getDefaultState(), 0);

        int nextId = 1;
        int totalBlocks = schematic.getWidth() * schematic.getHeight() * schematic.getLength();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int z = 0; z < schematic.getLength(); z++) {
                for (int x = 0; x < schematic.getWidth(); x++) {
                    BlockState state = schematic.getBlockState(x, y, z);
                    Integer id = stateToId.get(state);
                    if (id == null) {
                        id = nextId++;
                        stateToId.put(state, id);
                    }
                    writeVarInt(byteOut, id);
                }
            }
        }

        NbtCompound paletteCompound = new NbtCompound();
        for (Map.Entry<BlockState, Integer> entry : stateToId.entrySet()) {
            paletteCompound.putInt(blockStateToString(entry.getKey()), entry.getValue());
        }

        data.put("Palette", paletteCompound);
        data.put("BlockData", new NbtByteArray(byteOut.toByteArray()));

        NbtList beList = new NbtList();
        for (Map.Entry<BlockPos, NbtCompound> entry : schematic.getBlockEntities().entrySet()) {
            NbtCompound beNbt = entry.getValue().copy();
            beNbt.putIntArray("Pos", new int[]{entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()});
            beList.add(beNbt);
        }
        data.put("BlockEntities", beList);

        root.put("Schematic", data);
        NbtIo.writeCompressed(root, file.toPath());
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & 0xFFFFFF80) != 0L) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value & 0x7F);
    }

    public static BlockState parseBlockStateString(String str) {
        try {
            int bracketIndex = str.indexOf('[');
            String blockId = bracketIndex == -1 ? str : str.substring(0, bracketIndex);
            Identifier id = Identifier.of(blockId);
            Block block = Registries.BLOCK.get(id);
            if (block == null || block == Blocks.AIR && !blockId.equals("minecraft:air")) {
                return Blocks.AIR.getDefaultState();
            }

            BlockState state = block.getDefaultState();
            if (bracketIndex != -1 && str.endsWith("]")) {
                String propertiesStr = str.substring(bracketIndex + 1, str.length() - 1);
                String[] pairs = propertiesStr.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        state = applyProperty(state, kv[0].trim(), kv[1].trim());
                    }
                }
            }
            return state;
        } catch (Exception e) {
            return Blocks.AIR.getDefaultState();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyProperty(BlockState state, String key, String value) {
        Property property = state.getBlock().getStateManager().getProperty(key);
        if (property != null) {
            Optional opt = property.parse(value);
            if (opt.isPresent()) {
                return (BlockState) state.with(property, (Comparable) opt.get());
            }
        }
        return state;
    }

    public static String blockStateToString(BlockState state) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        StringBuilder sb = new StringBuilder(id.toString());
        Collection<Property<?>> properties = state.getProperties();
        if (!properties.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Property<?> prop : properties) {
                if (!first) sb.append(',');
                sb.append(prop.getName()).append('=').append(getPropertyValueName(prop, state));
                first = false;
            }
            sb.append(']');
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyValueName(Property<T> prop, BlockState state) {
        return prop.name(state.get(prop));
    }
}

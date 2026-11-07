package dev.autobuilder.schematic.format;

import dev.autobuilder.schematic.Schematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VanillaStructureHandler implements ISchematicHandler {

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".nbt");
    }

    @Override
    public Schematic read(File file) throws IOException {
        NbtCompound root = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());

        NbtList sizeList = root.getListOrEmpty("size");
        int width = sizeList.getInt(0, 1);
        int height = sizeList.getInt(1, 1);
        int length = sizeList.getInt(2, 1);

        String name = file.getName().replaceAll("(?i)\\.nbt$", "");
        Schematic schematic = new Schematic(name, width, height, length);

        List<BlockState> palette = new ArrayList<>();
        NbtList paletteList = root.getListOrEmpty("palette");
        for (int i = 0; i < paletteList.size(); i++) {
            NbtCompound entry = paletteList.getCompoundOrEmpty(i);
            palette.add(parsePaletteEntry(entry));
        }

        NbtList blockList = root.getListOrEmpty("blocks");
        for (int i = 0; i < blockList.size(); i++) {
            NbtCompound blockEntry = blockList.getCompoundOrEmpty(i);
            int stateIndex = blockEntry.getInt("state", 0);
            NbtList posList = blockEntry.getListOrEmpty("pos");
            int x = posList.getInt(0, 0);
            int y = posList.getInt(1, 0);
            int z = posList.getInt(2, 0);

            if (stateIndex >= 0 && stateIndex < palette.size()) {
                schematic.setBlockState(x, y, z, palette.get(stateIndex));
            }

            if (blockEntry.contains("nbt")) {
                schematic.setBlockEntityNbt(x, y, z, blockEntry.getCompoundOrEmpty("nbt").copy());
            }
        }

        return schematic;
    }

    @Override
    public void write(Schematic schematic, File file) throws IOException {
        NbtCompound root = new NbtCompound();

        NbtList sizeList = new NbtList();
        sizeList.add(NbtInt.of(schematic.getWidth()));
        sizeList.add(NbtInt.of(schematic.getHeight()));
        sizeList.add(NbtInt.of(schematic.getLength()));
        root.put("size", sizeList);

        Map<BlockState, Integer> stateToId = new LinkedHashMap<>();
        NbtList paletteList = new NbtList();
        NbtList blocksList = new NbtList();

        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int z = 0; z < schematic.getLength(); z++) {
                for (int x = 0; x < schematic.getWidth(); x++) {
                    BlockState state = schematic.getBlockState(x, y, z);
                    Integer id = stateToId.get(state);
                    if (id == null) {
                        id = stateToId.size();
                        stateToId.put(state, id);
                        paletteList.add(createPaletteEntry(state));
                    }

                    NbtCompound blockEntry = new NbtCompound();
                    blockEntry.putInt("state", id);
                    NbtList posList = new NbtList();
                    posList.add(NbtInt.of(x));
                    posList.add(NbtInt.of(y));
                    posList.add(NbtInt.of(z));
                    blockEntry.put("pos", posList);

                    NbtCompound beNbt = schematic.getBlockEntityNbt(x, y, z);
                    if (beNbt != null) {
                        blockEntry.put("nbt", beNbt.copy());
                    }

                    blocksList.add(blockEntry);
                }
            }
        }

        root.put("palette", paletteList);
        root.put("blocks", blocksList);
        root.put("entities", new NbtList());
        root.putInt("DataVersion", 3955);

        NbtIo.writeCompressed(root, file.toPath());
    }

    private static BlockState parsePaletteEntry(NbtCompound compound) {
        String name = compound.getString("Name", "minecraft:air");
        Identifier id = Identifier.of(name);
        Block block = Registries.BLOCK.get(id);
        if (block == null || (block == Blocks.AIR && !name.equals("minecraft:air"))) {
            return Blocks.AIR.getDefaultState();
        }

        BlockState state = block.getDefaultState();
        if (compound.contains("Properties")) {
            NbtCompound props = compound.getCompoundOrEmpty("Properties");
            for (String key : props.getKeys()) {
                String val = props.getString(key, "");
                state = applyProperty(state, key, val);
            }
        }
        return state;
    }

    private static NbtCompound createPaletteEntry(BlockState state) {
        NbtCompound entry = new NbtCompound();
        entry.putString("Name", Registries.BLOCK.getId(state.getBlock()).toString());

        Collection<Property<?>> properties = state.getProperties();
        if (!properties.isEmpty()) {
            NbtCompound props = new NbtCompound();
            for (Property<?> prop : properties) {
                props.putString(prop.getName(), getPropertyValueName(prop, state));
            }
            entry.put("Properties", props);
        }
        return entry;
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

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyValueName(Property<T> prop, BlockState state) {
        return prop.name(state.get(prop));
    }
}

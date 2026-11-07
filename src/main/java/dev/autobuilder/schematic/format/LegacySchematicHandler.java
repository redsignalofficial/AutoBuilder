package dev.autobuilder.schematic.format;

import dev.autobuilder.schematic.Schematic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LegacySchematicHandler implements ISchematicHandler {
    private static final Map<Integer, BlockState> LEGACY_MAP = new HashMap<>();

    static {
        registerLegacy(0, Blocks.AIR.getDefaultState());
        registerLegacy(1, Blocks.STONE.getDefaultState());
        registerLegacy(2, Blocks.GRASS_BLOCK.getDefaultState());
        registerLegacy(3, Blocks.DIRT.getDefaultState());
        registerLegacy(4, Blocks.COBBLESTONE.getDefaultState());
        registerLegacy(5, Blocks.OAK_PLANKS.getDefaultState());
        registerLegacy(7, Blocks.BEDROCK.getDefaultState());
        registerLegacy(12, Blocks.SAND.getDefaultState());
        registerLegacy(13, Blocks.GRAVEL.getDefaultState());
        registerLegacy(14, Blocks.GOLD_ORE.getDefaultState());
        registerLegacy(15, Blocks.IRON_ORE.getDefaultState());
        registerLegacy(16, Blocks.COAL_ORE.getDefaultState());
        registerLegacy(17, Blocks.OAK_LOG.getDefaultState());
        registerLegacy(18, Blocks.OAK_LEAVES.getDefaultState());
        registerLegacy(19, Blocks.SPONGE.getDefaultState());
        registerLegacy(20, Blocks.GLASS.getDefaultState());
        registerLegacy(21, Blocks.LAPIS_ORE.getDefaultState());
        registerLegacy(22, Blocks.LAPIS_BLOCK.getDefaultState());
        registerLegacy(23, Blocks.DISPENSER.getDefaultState());
        registerLegacy(24, Blocks.SANDSTONE.getDefaultState());
        registerLegacy(25, Blocks.NOTE_BLOCK.getDefaultState());
        registerLegacy(35, Blocks.WHITE_WOOL.getDefaultState());
        registerLegacy(41, Blocks.GOLD_BLOCK.getDefaultState());
        registerLegacy(42, Blocks.IRON_BLOCK.getDefaultState());
        registerLegacy(43, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        registerLegacy(44, Blocks.STONE_SLAB.getDefaultState());
        registerLegacy(45, Blocks.BRICKS.getDefaultState());
        registerLegacy(46, Blocks.TNT.getDefaultState());
        registerLegacy(47, Blocks.BOOKSHELF.getDefaultState());
        registerLegacy(48, Blocks.MOSSY_COBBLESTONE.getDefaultState());
        registerLegacy(49, Blocks.OBSIDIAN.getDefaultState());
        registerLegacy(50, Blocks.TORCH.getDefaultState());
        registerLegacy(53, Blocks.OAK_STAIRS.getDefaultState());
        registerLegacy(54, Blocks.CHEST.getDefaultState());
        registerLegacy(56, Blocks.DIAMOND_ORE.getDefaultState());
        registerLegacy(57, Blocks.DIAMOND_BLOCK.getDefaultState());
        registerLegacy(58, Blocks.CRAFTING_TABLE.getDefaultState());
        registerLegacy(61, Blocks.FURNACE.getDefaultState());
        registerLegacy(65, Blocks.LADDER.getDefaultState());
        registerLegacy(66, Blocks.RAIL.getDefaultState());
        registerLegacy(67, Blocks.COBBLESTONE_STAIRS.getDefaultState());
        registerLegacy(69, Blocks.LEVER.getDefaultState());
        registerLegacy(70, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
        registerLegacy(72, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
        registerLegacy(73, Blocks.REDSTONE_ORE.getDefaultState());
        registerLegacy(76, Blocks.REDSTONE_TORCH.getDefaultState());
        registerLegacy(77, Blocks.STONE_BUTTON.getDefaultState());
        registerLegacy(78, Blocks.SNOW.getDefaultState());
        registerLegacy(79, Blocks.ICE.getDefaultState());
        registerLegacy(80, Blocks.SNOW_BLOCK.getDefaultState());
        registerLegacy(81, Blocks.CACTUS.getDefaultState());
        registerLegacy(82, Blocks.CLAY.getDefaultState());
        registerLegacy(84, Blocks.JUKEBOX.getDefaultState());
        registerLegacy(85, Blocks.OAK_FENCE.getDefaultState());
        registerLegacy(86, Blocks.PUMPKIN.getDefaultState());
        registerLegacy(87, Blocks.NETHERRACK.getDefaultState());
        registerLegacy(88, Blocks.SOUL_SAND.getDefaultState());
        registerLegacy(89, Blocks.GLOWSTONE.getDefaultState());
        registerLegacy(91, Blocks.JACK_O_LANTERN.getDefaultState());
        registerLegacy(98, Blocks.STONE_BRICKS.getDefaultState());
        registerLegacy(101, Blocks.IRON_BARS.getDefaultState());
        registerLegacy(102, Blocks.GLASS_PANE.getDefaultState());
        registerLegacy(103, Blocks.MELON.getDefaultState());
        registerLegacy(108, Blocks.BRICK_STAIRS.getDefaultState());
        registerLegacy(109, Blocks.STONE_BRICK_STAIRS.getDefaultState());
        registerLegacy(112, Blocks.NETHER_BRICKS.getDefaultState());
        registerLegacy(121, Blocks.END_STONE.getDefaultState());
        registerLegacy(123, Blocks.REDSTONE_LAMP.getDefaultState());
        registerLegacy(133, Blocks.EMERALD_BLOCK.getDefaultState());
        registerLegacy(138, Blocks.BEACON.getDefaultState());
        registerLegacy(143, Blocks.OAK_BUTTON.getDefaultState());
        registerLegacy(145, Blocks.ANVIL.getDefaultState());
        registerLegacy(146, Blocks.TRAPPED_CHEST.getDefaultState());
        registerLegacy(152, Blocks.REDSTONE_BLOCK.getDefaultState());
        registerLegacy(153, Blocks.NETHER_QUARTZ_ORE.getDefaultState());
        registerLegacy(154, Blocks.HOPPER.getDefaultState());
        registerLegacy(155, Blocks.QUARTZ_BLOCK.getDefaultState());
        registerLegacy(158, Blocks.DROPPER.getDefaultState());
        registerLegacy(159, Blocks.TERRACOTTA.getDefaultState());
        registerLegacy(165, Blocks.SLIME_BLOCK.getDefaultState());
        registerLegacy(168, Blocks.PRISMARINE.getDefaultState());
        registerLegacy(169, Blocks.SEA_LANTERN.getDefaultState());
        registerLegacy(170, Blocks.HAY_BLOCK.getDefaultState());
        registerLegacy(173, Blocks.COAL_BLOCK.getDefaultState());
    }

    private static void registerLegacy(int id, BlockState state) {
        LEGACY_MAP.put(id, state);
    }

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".schematic");
    }

    @Override
    public Schematic read(File file) throws IOException {
        NbtCompound root = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());

        int width = root.getShort("Width", (short) 0) & 0xFFFF;
        int height = root.getShort("Height", (short) 0) & 0xFFFF;
        int length = root.getShort("Length", (short) 0) & 0xFFFF;

        String name = file.getName().replaceAll("(?i)\\.schematic$", "");
        Schematic schematic = new Schematic(name, width, height, length);

        byte[] blockIds = root.getByteArray("Blocks").orElse(new byte[0]);
        byte[] addBlocks = root.getByteArray("AddBlocks").orElse(null);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    if (index >= blockIds.length) continue;

                    int id = blockIds[index] & 0xFF;
                    if (addBlocks != null) {
                        int addIndex = index >> 1;
                        if (addIndex < addBlocks.length) {
                            if ((index & 1) == 0) {
                                id |= (addBlocks[addIndex] & 0x0F) << 8;
                            } else {
                                id |= ((addBlocks[addIndex] >> 4) & 0x0F) << 8;
                            }
                        }
                    }

                    BlockState state = LEGACY_MAP.getOrDefault(id, Blocks.AIR.getDefaultState());
                    schematic.setBlockState(x, y, z, state);
                }
            }
        }

        NbtList list = root.getListOrEmpty("TileEntities");
        for (int i = 0; i < list.size(); i++) {
            NbtCompound te = list.getCompoundOrEmpty(i);
            int x = te.getInt("x", 0);
            int y = te.getInt("y", 0);
            int z = te.getInt("z", 0);
            if (schematic.isInBounds(x, y, z)) {
                schematic.setBlockEntityNbt(x, y, z, te.copy());
            }
        }

        return schematic;
    }

    @Override
    public void write(Schematic schematic, File file) throws IOException {
        new SpongeSchematicHandler().write(schematic, file);
    }
}

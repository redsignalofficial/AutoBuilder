package dev.autobuilder.schematic;

import dev.autobuilder.AutoBuilder;
import dev.autobuilder.schematic.format.ISchematicHandler;
import dev.autobuilder.schematic.format.LegacySchematicHandler;
import dev.autobuilder.schematic.format.SpongeSchematicHandler;
import dev.autobuilder.schematic.format.VanillaStructureHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SchematicManager {
    private static final SchematicManager INSTANCE = new SchematicManager();

    private final List<ISchematicHandler> handlers = new ArrayList<>();
    private final File schematicsDirectory;
    private Schematic activeSchematic;

    private SchematicManager() {
        handlers.add(new SpongeSchematicHandler());
        handlers.add(new LegacySchematicHandler());
        handlers.add(new VanillaStructureHandler());

        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.schematicsDirectory = gameDir.resolve("schematics").resolve("autobuilder").toFile();
        if (!this.schematicsDirectory.exists()) {
            this.schematicsDirectory.mkdirs();
        }
    }

    public static SchematicManager getInstance() {
        return INSTANCE;
    }

    public File getSchematicsDirectory() {
        return schematicsDirectory;
    }

    public Schematic getActiveSchematic() {
        return activeSchematic;
    }

    public void setActiveSchematic(Schematic activeSchematic) {
        this.activeSchematic = activeSchematic;
    }

    public List<File> listFiles() {
        if (!schematicsDirectory.exists()) {
            return new ArrayList<>();
        }
        File[] files = schematicsDirectory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".schem") || lower.endsWith(".schematic") || lower.endsWith(".nbt");
        });
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }

    public CompletableFuture<Schematic> loadAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (ISchematicHandler handler : handlers) {
                    if (handler.supports(file)) {
                        Schematic loaded = handler.read(file);
                        this.activeSchematic = loaded;
                        return loaded;
                    }
                }
                throw new IllegalArgumentException("Unsupported schematic format: " + file.getName());
            } catch (Exception e) {
                AutoBuilder.LOGGER.error("Failed to load schematic file {}: ", file.getName(), e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<File> saveAsync(Schematic schematic, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!schematicsDirectory.exists()) {
                    schematicsDirectory.mkdirs();
                }
                String cleanName = name.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (!cleanName.endsWith(".schem")) {
                    cleanName += ".schem";
                }
                File target = new File(schematicsDirectory, cleanName);
                new SpongeSchematicHandler().write(schematic, target);
                return target;
            } catch (Exception e) {
                AutoBuilder.LOGGER.error("Failed to save schematic {}: ", name, e);
                throw new RuntimeException(e);
            }
        });
    }

    public boolean deleteFile(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }

    public boolean renameFile(File file, String newName) {
        if (file == null || !file.exists()) {
            return false;
        }
        String ext = "";
        int dot = file.getName().lastIndexOf('.');
        if (dot != -1) {
            ext = file.getName().substring(dot);
        }
        String clean = newName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!clean.endsWith(ext)) {
            clean += ext;
        }
        File dest = new File(file.getParentFile(), clean);
        return file.renameTo(dest);
    }
}

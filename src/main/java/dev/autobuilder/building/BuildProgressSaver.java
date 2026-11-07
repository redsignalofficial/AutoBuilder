package dev.autobuilder.building;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.autobuilder.AutoBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles persistent saving and restoration of active build progress and climb log
 * across relogs, disconnects, or crashes.
 */
public class BuildProgressSaver {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ProgressData {
        public String schematicName;
        public int originX, originY, originZ;
        public int taskIndex;
        public int totalTasks;
        public List<int[]> climbLog = new ArrayList<>();
        public long timestamp;
    }

    private static File getSaveDir() {
        File dir = new File(FabricLoader.getInstance().getGameDir().toFile(), "autobuilder/saves");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static File getSaveFile(String schematicName) {
        String safeName = schematicName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(getSaveDir(), safeName + ".progress.json");
    }

    public static void saveAsync(String schematicName, BlockPos origin, int taskIndex, int totalTasks, List<BlockPos> climbLog) {
        if (schematicName == null || origin == null) return;

        ProgressData data = new ProgressData();
        data.schematicName = schematicName;
        data.originX = origin.getX();
        data.originY = origin.getY();
        data.originZ = origin.getZ();
        data.taskIndex = taskIndex;
        data.totalTasks = totalTasks;
        data.timestamp = System.currentTimeMillis();

        if (climbLog != null) {
            for (BlockPos pos : climbLog) {
                data.climbLog.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
            }
        }

        CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(getSaveFile(schematicName))) {
                GSON.toJson(data, writer);
            } catch (Exception e) {
                AutoBuilder.LOGGER.warn("Failed to persist build progress: " + e.getMessage());
            }
        });
    }

    public static ProgressData load(String schematicName) {
        if (schematicName == null) return null;
        File file = getSaveFile(schematicName);
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, ProgressData.class);
        } catch (Exception e) {
            AutoBuilder.LOGGER.warn("Failed to load build progress: " + e.getMessage());
            return null;
        }
    }

    public static void clear(String schematicName) {
        if (schematicName == null) return;
        File file = getSaveFile(schematicName);
        if (file.exists()) {
            file.delete();
        }
    }
}

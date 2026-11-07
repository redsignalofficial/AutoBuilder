package dev.autobuilder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.autobuilder.AutoBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "autobuilder.json";
    private static ModConfig instance = new ModConfig();

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            File configFile = configDir.resolve(FILE_NAME).toFile();
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) {
                        instance = loaded;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            AutoBuilder.LOGGER.error("Failed to load AutoBuilder configuration: ", e);
        }
    }

    public static void save() {
        CompletableFuture.runAsync(() -> {
            try {
                Path configDir = FabricLoader.getInstance().getConfigDir();
                File configFile = configDir.resolve(FILE_NAME).toFile();
                if (!configFile.getParentFile().exists()) {
                    configFile.getParentFile().mkdirs();
                }
                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(instance, writer);
                }
            } catch (Exception e) {
                AutoBuilder.LOGGER.error("Failed to save AutoBuilder configuration: ", e);
            }
        });
    }
}

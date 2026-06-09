package org.dnplugins.dnbackup.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("DNBackUp");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("dnbackup.json");
    
    private static ModConfig config = new ModConfig();

    public static ModConfig getConfig() {
        return config;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            LOGGER.info("Config file not found, creating default config.");
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig loadedConfig = GSON.fromJson(reader, ModConfig.class);
            if (loadedConfig != null) {
                config = loadedConfig;
                LOGGER.info("Configuration loaded successfully.");
            } else {
                LOGGER.warn("Failed to parse config, using defaults.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: ", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
                LOGGER.info("Configuration saved successfully.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: ", e);
        }
    }

    public static void reload() {
        load();
    }
}

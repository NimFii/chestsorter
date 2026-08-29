package qnimfi.cs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChestSorterConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chestsorter.json");
    private static ChestSorterConfig INSTANCE;

    public int filterSlots = 9;

    public static ChestSorterConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    private static ChestSorterConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ChestSorterConfig loaded = GSON.fromJson(reader, ChestSorterConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException e) {
                ChestSorter.LOGGER.error("Failed to read chestsorter.json, using defaults", e);
            }
        }
        ChestSorterConfig defaults = new ChestSorterConfig();
        defaults.save();
        return defaults;
    }

    private void sanitize() {
        if (filterSlots < 1) filterSlots = 1;
        if (filterSlots > 54) filterSlots = 54;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ChestSorter.LOGGER.error("Failed to save chestsorter.json", e);
        }
    }
}
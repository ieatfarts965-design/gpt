package com.example.storageesp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StorageEspConfig {
    public enum RenderMode { OUTLINE, LINES, BOTH }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("storageesp.json");

    public boolean enabled = true;
    public RenderMode renderMode = RenderMode.OUTLINE;
    public int range = 48;
    public int containersPerChunk = 8;
    public boolean chunkAlerts = true;
    public String alertMessage = "Storage cluster at chunk {x}, {z}: {count} containers";
    public boolean relogReminder = true;
    public int relogMinutes = 30;

    public static StorageEspConfig load() {
        if (Files.exists(PATH)) {
            try {
                StorageEspConfig loaded = GSON.fromJson(Files.readString(PATH), StorageEspConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException ignored) {
                // Use defaults if the config is missing or malformed.
            }
        }
        return new StorageEspConfig();
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {
            // A failed config write should not interrupt gameplay.
        }
    }

    public void sanitize() {
        if (renderMode == null) renderMode = RenderMode.OUTLINE;
        range = Math.max(8, Math.min(128, range));
        containersPerChunk = Math.max(2, Math.min(64, containersPerChunk));
        relogMinutes = Math.max(5, Math.min(120, relogMinutes));
        if (alertMessage == null || alertMessage.isBlank()) {
            alertMessage = "Storage cluster at chunk {x}, {z}: {count} containers";
        }
    }
}


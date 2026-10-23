package com.vanta.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VantaConfig {
    public enum RenderMode { OUTLINE, LINES, BOTH }
    public enum EmptyChunkState { IDLE, WAITING_FOR_RELOG, REPORTED_NOTHING }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("vanta-client.json");

    public boolean enabled = true;
    public boolean suspiciousEsp = true;
    public RenderMode renderMode = RenderMode.OUTLINE;
    public int range = 48;
    public boolean chests = true;
    public boolean trappedChests = true;
    public boolean enderChests = true;
    public boolean barrels = true;
    public boolean shulkerBoxes = true;
    public int containersPerChunk = 8;
    public boolean susChunkFinder = true;
    public boolean chunkAlerts = true;
    public String alertMessage = "Storage cluster at chunk {x}, {z}: {count} containers";
    public boolean chunkFinder = false;
    public String chunkFinderMessage = "New loaded chunk at {x}, {z}";
    public boolean relogReminder = true;
    public EmptyChunkState emptyChunkState = EmptyChunkState.IDLE;
    public int emptyChunkX;
    public int emptyChunkZ;
    public String relogMessage = "Nothing detected at Y -20. Relog once to refresh this chunk, then check again.";
    public int autoEatThreshold = 14;
    public Map<String, Boolean> moduleToggles = new LinkedHashMap<>();

    public static VantaConfig load() {
        if (Files.exists(PATH)) {
            try {
                VantaConfig loaded = GSON.fromJson(Files.readString(PATH), VantaConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException ignored) {
                // Use defaults if the config is missing or malformed.
            }
        }
        return new VantaConfig();
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
        if (moduleToggles == null) moduleToggles = new LinkedHashMap<>();
        if (renderMode == null) renderMode = RenderMode.OUTLINE;
        if (emptyChunkState == null) emptyChunkState = EmptyChunkState.IDLE;
        range = Math.max(8, Math.min(128, range));
        containersPerChunk = Math.max(2, Math.min(64, containersPerChunk));
        autoEatThreshold = Math.max(1, Math.min(19, autoEatThreshold));
        if (alertMessage == null || alertMessage.isBlank()) {
            alertMessage = "Storage cluster at chunk {x}, {z}: {count} containers";
        }
        if (chunkFinderMessage == null || chunkFinderMessage.isBlank()) {
            chunkFinderMessage = "New loaded chunk at {x}, {z}";
        }
        if (relogMessage == null || relogMessage.isBlank()) {
            relogMessage = "Nothing detected at Y -20. Relog once to refresh this chunk, then check again.";
        }
    }
}


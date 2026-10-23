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
    public int autoClickCps = 8;
    public int lightThreshold = 7;
    public int autoLogHealth = 6;
    public int totemHealthThreshold = 12;
    public String chatMacroMessage = "gg";
    public String friendNames = "";
    public String autoSellCommand = "sell hand";
    public String autoSpawnerSellCommand = "sell spawners";
    public String rtpCommand = "rtp";
    public String seedFinderSeed = "";
    public String fakePayPreviewText = "Payment preview: $100";
    public String fakeStatsText = "Kills 0  ·  Wins 0  ·  Balance $0";
    public String radioUrl = "";
    public String discordApplicationId = "";
    public String discordStatus = "Vanta Client";
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
        autoClickCps = Math.max(1, Math.min(20, autoClickCps));
        lightThreshold = Math.max(0, Math.min(15, lightThreshold));
        autoLogHealth = Math.max(1, Math.min(20, autoLogHealth));
        totemHealthThreshold = Math.max(1, Math.min(20, totemHealthThreshold));
        if (alertMessage == null || alertMessage.isBlank()) {
            alertMessage = "Storage cluster at chunk {x}, {z}: {count} containers";
        }
        if (chunkFinderMessage == null || chunkFinderMessage.isBlank()) {
            chunkFinderMessage = "New loaded chunk at {x}, {z}";
        }
        if (relogMessage == null || relogMessage.isBlank()) {
            relogMessage = "Nothing detected at Y -20. Relog once to refresh this chunk, then check again.";
        }
        if (chatMacroMessage == null || chatMacroMessage.isBlank()) chatMacroMessage = "gg";
        if (chatMacroMessage.length() > 160) chatMacroMessage = chatMacroMessage.substring(0, 160);
        if (friendNames == null) friendNames = "";
        if (friendNames.length() > 160) friendNames = friendNames.substring(0, 160);
        if (autoSellCommand == null || autoSellCommand.isBlank()) autoSellCommand = "sell hand";
        if (autoSpawnerSellCommand == null || autoSpawnerSellCommand.isBlank()) autoSpawnerSellCommand = "sell spawners";
        if (rtpCommand == null || rtpCommand.isBlank()) rtpCommand = "rtp";
        autoSellCommand = limitCommand(autoSellCommand);
        autoSpawnerSellCommand = limitCommand(autoSpawnerSellCommand);
        rtpCommand = limitCommand(rtpCommand);
        if (seedFinderSeed == null) seedFinderSeed = "";
        if (seedFinderSeed.length() > 32) seedFinderSeed = seedFinderSeed.substring(0, 32);
        if (fakePayPreviewText == null || fakePayPreviewText.isBlank()) fakePayPreviewText = "Payment preview: $100";
        if (fakeStatsText == null || fakeStatsText.isBlank()) fakeStatsText = "Kills 0  ·  Wins 0  ·  Balance $0";
        if (fakePayPreviewText.length() > 120) fakePayPreviewText = fakePayPreviewText.substring(0, 120);
        if (fakeStatsText.length() > 120) fakeStatsText = fakeStatsText.substring(0, 120);
        if (radioUrl == null) radioUrl = "";
        if (radioUrl.length() > 300) radioUrl = radioUrl.substring(0, 300);
        if (discordApplicationId == null) discordApplicationId = "";
        if (discordApplicationId.length() > 25) discordApplicationId = discordApplicationId.substring(0, 25);
        if (discordStatus == null || discordStatus.isBlank()) discordStatus = "Vanta Client";
        if (discordStatus.length() > 100) discordStatus = discordStatus.substring(0, 100);
    }

    private static String limitCommand(String value) {
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
}


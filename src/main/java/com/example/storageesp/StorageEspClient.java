package com.example.storageesp;

import com.example.storageesp.render.StorageEspRenderLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StorageEspClient implements ClientModInitializer {
    private static final float[] STORAGE_COLOR = {0.22f, 0.82f, 0.96f};
    private static final float[] SUSPICIOUS_COLOR = {1.00f, 0.34f, 0.25f};
    private static final StorageEspConfig CONFIG = StorageEspConfig.load();
    private static final Set<Long> ALERTED_CHUNKS = new HashSet<>();
    private static final Set<Long> PREVIOUS_LOADED_CHUNKS = new HashSet<>();
    private static final Set<Long> SUSPECT_CHUNK_KEYS = new HashSet<>();
    private static List<SuspectChunk> suspectChunks = List.of();
    private static List<BlockPos> storagePositions = List.of();
    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;
    private static long lastRelogReminder = System.currentTimeMillis();
    private static int scanTicks;
    private static boolean chunkFinderInitialized;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.silo.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.silo"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.silo.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.silo"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                CONFIG.enabled = !CONFIG.enabled;
                CONFIG.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Silo Storage ESP " + (CONFIG.enabled ? "enabled" : "disabled")), true);
                }
            }
            while (menuKey.wasPressed()) {
                client.setScreen(new StorageEspScreen(client.currentScreen, CONFIG));
            }

            if (client.world == null || client.player == null) {
                suspectChunks = List.of();
                storagePositions = List.of();
                ALERTED_CHUNKS.clear();
                PREVIOUS_LOADED_CHUNKS.clear();
                SUSPECT_CHUNK_KEYS.clear();
                chunkFinderInitialized = false;
                lastRelogReminder = System.currentTimeMillis();
                return;
            }

            if (++scanTicks >= 20) {
                scanTicks = 0;
                scanLoadedChunks(client);
            }

            long reminderInterval = CONFIG.relogMinutes * 60_000L;
            long now = System.currentTimeMillis();
            if (CONFIG.relogReminder && now - lastRelogReminder >= reminderInterval) {
                Text reminder = Text.literal(CONFIG.relogMessage);
                if (CONFIG.relogNotifyMode == StorageEspConfig.RelogNotifyMode.CHAT
                        || CONFIG.relogNotifyMode == StorageEspConfig.RelogNotifyMode.BOTH) {
                    client.player.sendMessage(reminder, false);
                }
                if (CONFIG.relogNotifyMode == StorageEspConfig.RelogNotifyMode.ACTIONBAR
                        || CONFIG.relogNotifyMode == StorageEspConfig.RelogNotifyMode.BOTH) {
                    client.player.sendMessage(reminder, true);
                }
                lastRelogReminder = now;
            }
        });

        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!CONFIG.enabled || client.world == null || client.player == null
                    || context.matrixStack() == null || context.consumers() == null) {
                return;
            }

            Vec3d camera = context.camera().getPos();
            double rangeSquared = CONFIG.range * (double) CONFIG.range;
            var matrices = context.matrixStack();
            matrices.push();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            VertexConsumer lines = context.consumers().getBuffer(
                    StorageEspRenderLayer.LINES);

            for (BlockPos pos : storagePositions) {
                if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) {
                    continue;
                }

                float[] color = CONFIG.suspiciousEsp
                        && SUSPECT_CHUNK_KEYS.contains(chunkKey(pos.getX() >> 4, pos.getZ() >> 4))
                        ? SUSPICIOUS_COLOR : STORAGE_COLOR;

                if (CONFIG.renderMode != StorageEspConfig.RenderMode.LINES) {
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.003),
                            color[0], color[1], color[2], 1.0f);
                }
                if (CONFIG.renderMode != StorageEspConfig.RenderMode.OUTLINE) {
                    Vec3d target = Vec3d.ofCenter(pos);
                    var matrix = matrices.peek();
                    lines.vertex(matrix, (float) camera.x, (float) camera.y, (float) camera.z)
                            .color(color[0], color[1], color[2], 1.0f).normal(matrix, 0.0f, 1.0f, 0.0f);
                    lines.vertex(matrix, (float) target.x, (float) target.y, (float) target.z)
                            .color(color[0], color[1], color[2], 1.0f).normal(matrix, 0.0f, 1.0f, 0.0f);
                }
            }

            matrices.pop();
        });
    }

    private static void scanLoadedChunks(MinecraftClient client) {
        Map<Long, ChunkCount> counts = new HashMap<>();
        List<BlockPos> currentStoragePositions = new ArrayList<>();
        Set<Long> loadedNow = new HashSet<>();
        int centerChunkX = client.player.getBlockPos().getX() >> 4;
        int centerChunkZ = client.player.getBlockPos().getZ() >> 4;
        int chunkRadius = (CONFIG.range + 15) / 16 + 1;
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                WorldChunk chunk = client.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                if (chunk == null) continue;
                long key = chunkKey(chunkX, chunkZ);
                loadedNow.add(key);
                if (CONFIG.chunkFinder && chunkFinderInitialized && !PREVIOUS_LOADED_CHUNKS.contains(key)) {
                    String message = CONFIG.chunkFinderMessage
                            .replace("{x}", Integer.toString(chunkX))
                            .replace("{z}", Integer.toString(chunkZ));
                    client.player.sendMessage(Text.literal(message), false);
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isStorage(blockEntity.getCachedState().getBlock())) continue;
                    BlockPos pos = blockEntity.getPos();
                    currentStoragePositions.add(pos.toImmutable());
                    ChunkCount count = counts.get(key);
                    if (count == null) {
                        count = new ChunkCount(chunkX, chunkZ);
                        counts.put(key, count);
                    }
                    count.count++;
                }
            }
        }
        PREVIOUS_LOADED_CHUNKS.clear();
        PREVIOUS_LOADED_CHUNKS.addAll(loadedNow);
        chunkFinderInitialized = true;

        List<SuspectChunk> current = new ArrayList<>();
        Set<Long> active = new HashSet<>();
        SUSPECT_CHUNK_KEYS.clear();
        for (Map.Entry<Long, ChunkCount> entry : counts.entrySet()) {
            if (!CONFIG.susChunkFinder) break;
            ChunkCount chunk = entry.getValue();
            if (chunk.count < CONFIG.containersPerChunk) continue;
            current.add(new SuspectChunk(chunk.x, chunk.z, chunk.count));
            active.add(entry.getKey());
            SUSPECT_CHUNK_KEYS.add(entry.getKey());
            if (CONFIG.chunkAlerts && ALERTED_CHUNKS.add(entry.getKey())) {
                String message = CONFIG.alertMessage
                        .replace("{x}", Integer.toString(chunk.x))
                        .replace("{z}", Integer.toString(chunk.z))
                        .replace("{count}", Integer.toString(chunk.count));
                client.player.sendMessage(Text.literal(message), false);
            }
        }
        ALERTED_CHUNKS.retainAll(active);
        current.sort((left, right) -> Integer.compare(right.count(), left.count()));
        suspectChunks = List.copyOf(current);
        storagePositions = List.copyOf(currentStoragePositions);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static boolean isStorage(Block block) {
        if (block instanceof TrappedChestBlock) return CONFIG.trappedChests;
        if (block instanceof ChestBlock) return CONFIG.chests;
        if (block instanceof EnderChestBlock) return CONFIG.enderChests;
        if (block instanceof BarrelBlock) return CONFIG.barrels;
        if (block instanceof ShulkerBoxBlock) return CONFIG.shulkerBoxes;
        return false;
    }

    public static List<SuspectChunk> getSuspectChunks() {
        return suspectChunks;
    }

    private static final class ChunkCount {
        private final int x;
        private final int z;
        private int count;

        private ChunkCount(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    public record SuspectChunk(int x, int z, int count) {
    }
}


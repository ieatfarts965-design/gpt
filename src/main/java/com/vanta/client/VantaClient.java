package com.vanta.client;

import com.vanta.client.render.VantaRenderLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VantaClient implements ClientModInitializer {
    private static final float[] STORAGE_COLOR = {0.22f, 0.82f, 0.96f};
    private static final float[] SUSPICIOUS_COLOR = {1.00f, 0.34f, 0.25f};
    private static final VantaConfig CONFIG = VantaConfig.load();
    private static final Set<Long> ALERTED_CHUNKS = new HashSet<>();
    private static final Set<Long> PREVIOUS_LOADED_CHUNKS = new HashSet<>();
    private static final Set<Long> SUSPECT_CHUNK_KEYS = new HashSet<>();
    private static final Set<BlockPos> ALERTED_SPAWNERS = new HashSet<>();
    private static List<SuspectChunk> suspectChunks = List.of();
    private static List<BlockPos> storagePositions = List.of();
    private static List<BlockPos> spawnerPositions = List.of();
    private static List<BlockPos> debrisPositions = List.of();
    private static List<BlockPos> holePositions = List.of();
    private static List<BlockPos> dimPositions = List.of();
    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;
    private static int scanTicks;
    private static boolean chunkFinderInitialized;
    private static boolean disconnectedSinceEmptyScan;
    private static boolean recheckAfterRelog;
    private static int worldReadyTicks;
    private static boolean fullbrightApplied;
    private static double previousGamma = 1.0;
    private static int autoEatOriginalSlot = -1;
    private static int autoEatSelectedSlot = -1;
    private static boolean autoEatActive;
    private static int autoToolOriginalSlot = -1;
    private static int autoToolSelectedSlot = -1;
    private static boolean autoToolActive;
    private static boolean previousRaining;
    private static boolean previousThundering;
    private static boolean weatherInitialized;
    private static int autoClickTicks;
    private static boolean autoWalkApplied;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.vanta"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vanta"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                CONFIG.enabled = !CONFIG.enabled;
                CONFIG.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Vanta Storage ESP " + (CONFIG.enabled ? "enabled" : "disabled")), true);
                }
            }
            while (menuKey.wasPressed()) {
                client.setScreen(new VantaScreen(client.currentScreen, CONFIG));
            }

            applyClientModules(client);
            applyUtilityModules(client);

            if (client.world == null || client.player == null) {
                suspectChunks = List.of();
                storagePositions = List.of();
                spawnerPositions = List.of();
                debrisPositions = List.of();
                holePositions = List.of();
                dimPositions = List.of();
                ALERTED_CHUNKS.clear();
                ALERTED_SPAWNERS.clear();
                PREVIOUS_LOADED_CHUNKS.clear();
                SUSPECT_CHUNK_KEYS.clear();
                chunkFinderInitialized = false;
                if (CONFIG.emptyChunkState == VantaConfig.EmptyChunkState.WAITING_FOR_RELOG) {
                    disconnectedSinceEmptyScan = true;
                }
                worldReadyTicks = 0;
                return;
            }

            worldReadyTicks++;
            if (disconnectedSinceEmptyScan) {
                disconnectedSinceEmptyScan = false;
                recheckAfterRelog = true;
                worldReadyTicks = 0;
            }

            if (++scanTicks >= 20) {
                scanTicks = 0;
                scanLoadedChunks(client);
            }

            checkEmptyChunkRelogFlow(client);
        });

        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if ((!CONFIG.enabled && !moduleEnabled("PLAYER_ESP") && !moduleEnabled("MOB_ESP")
                    && !moduleEnabled("PLAYER_CHUNKS") && !moduleEnabled("SPAWNER_PROTECT"))
                    && !moduleEnabled("NETHERITE_FINDER") && !moduleEnabled("HOLE_ESP")
                    && !moduleEnabled("LIGHT_FINDER")
                    || client.world == null || client.player == null
                    || context.matrixStack() == null || context.consumers() == null) {
                return;
            }

            Vec3d camera = context.camera().getPos();
            double rangeSquared = CONFIG.range * (double) CONFIG.range;
            var matrices = context.matrixStack();
            matrices.push();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            VertexConsumer lines = context.consumers().getBuffer(
                    VantaRenderLayer.LINES);

            if (moduleEnabled("PLAYER_ESP")) {
                for (PlayerEntity target : client.world.getPlayers()) {
                    if (target == client.player || client.player.squaredDistanceTo(target) > rangeSquared) continue;
                    WorldRenderer.drawBox(matrices, lines, target.getBoundingBox().expand(0.04),
                            0.95f, 0.48f, 0.82f, 0.95f);
                }
            }
            if (moduleEnabled("MOB_ESP")) {
                for (MobEntity target : client.world.getEntitiesByClass(MobEntity.class,
                        client.player.getBoundingBox().expand(CONFIG.range), entity -> true)) {
                    WorldRenderer.drawBox(matrices, lines, target.getBoundingBox().expand(0.04),
                            1.0f, 0.68f, 0.24f, 0.9f);
                }
            }
            if (moduleEnabled("PLAYER_CHUNKS")) {
                for (PlayerEntity target : client.world.getPlayers()) {
                    if (target == client.player || client.player.squaredDistanceTo(target) > rangeSquared) continue;
                    int chunkX = target.getBlockPos().getX() >> 4;
                    int chunkZ = target.getBlockPos().getZ() >> 4;
                    double y = Math.floor(target.getY()) + 0.02;
                    Box footprint = new Box(chunkX * 16.0, y, chunkZ * 16.0,
                            (chunkX + 1) * 16.0, y + 0.04, (chunkZ + 1) * 16.0);
                    WorldRenderer.drawBox(matrices, lines, footprint, 0.55f, 0.38f, 1.0f, 0.75f);
                }
            }
            if (moduleEnabled("SPAWNER_PROTECT")) {
                for (BlockPos pos : spawnerPositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.04),
                            1.0f, 0.56f, 0.18f, 0.95f);
                }
            }
            if (moduleEnabled("NETHERITE_FINDER")) {
                for (BlockPos pos : debrisPositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.02),
                            0.35f, 0.95f, 0.68f, 1.0f);
                }
            }
            if (moduleEnabled("HOLE_ESP")) {
                for (BlockPos pos : holePositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    Box hole = new Box(pos.getX() + 0.08, pos.getY(), pos.getZ() + 0.08,
                            pos.getX() + 0.92, pos.getY() + 2.0, pos.getZ() + 0.92);
                    WorldRenderer.drawBox(matrices, lines, hole, 0.32f, 0.9f, 0.82f, 0.9f);
                }
            }
            if (moduleEnabled("LIGHT_FINDER")) {
                for (BlockPos pos : dimPositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.03),
                            1.0f, 0.28f, 0.25f, 0.8f);
                }
            }

            if (CONFIG.enabled) {
                for (BlockPos pos : storagePositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) {
                        continue;
                    }

                    float[] color = CONFIG.suspiciousEsp
                            && SUSPECT_CHUNK_KEYS.contains(chunkKey(pos.getX() >> 4, pos.getZ() >> 4))
                            ? SUSPICIOUS_COLOR : STORAGE_COLOR;

                    if (CONFIG.renderMode != VantaConfig.RenderMode.LINES) {
                        WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.003),
                                color[0], color[1], color[2], 1.0f);
                    }
                    if (CONFIG.renderMode != VantaConfig.RenderMode.OUTLINE) {
                        Vec3d target = Vec3d.ofCenter(pos);
                        var matrix = matrices.peek();
                        lines.vertex(matrix, (float) camera.x, (float) camera.y, (float) camera.z)
                                .color(color[0], color[1], color[2], 1.0f).normal(matrix, 0.0f, 1.0f, 0.0f);
                        lines.vertex(matrix, (float) target.x, (float) target.y, (float) target.z)
                                .color(color[0], color[1], color[2], 1.0f).normal(matrix, 0.0f, 1.0f, 0.0f);
                    }
                }
            }

            matrices.pop();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (moduleEnabled("HUD")) {
                var pos = client.player.getBlockPos();
                String info = String.format("VANTA  XYZ %d %d %d  FPS %d",
                        pos.getX(), pos.getY(), pos.getZ(), client.getCurrentFps());
                context.drawText(client.textRenderer, Text.literal(info), 8, 8, 0xFFE8E2FF, true);
            }
            if (moduleEnabled("BLOCK_ENTITY_DEBUG")) {
                String info = "VANTA DEBUG  loaded storage " + storagePositions.size()
                        + "  flagged chunks " + suspectChunks.size();
                context.drawText(client.textRenderer, Text.literal(info), 8, 22, 0xFFBBA8FF, true);
            }
            if (moduleEnabled("TARGET_HUD") && client.targetedEntity instanceof LivingEntity target
                    && target.isAlive() && target != client.player) {
                int left = 8;
                int top = moduleEnabled("HUD") ? 42 : 8;
                int right = 190;
                context.fill(left, top, right, top + 43, 0xD014171E);
                context.fill(left, top, right, top + 1, 0xFF9A7BFF);
                context.drawText(client.textRenderer, target.getDisplayName(), left + 8, top + 7,
                        0xFFE6E9EF, true);
                float health = Math.max(0.0f, Math.min(1.0f, target.getHealth() / target.getMaxHealth()));
                context.fill(left + 8, top + 28, right - 8, top + 34, 0xFF343A46);
                context.fill(left + 8, top + 28, left + 8 + (int) ((right - left - 16) * health),
                        top + 34, 0xFF9A7BFF);
                String healthText = String.format("%.1f / %.1f", target.getHealth(), target.getMaxHealth());
                context.drawText(client.textRenderer, Text.literal(healthText), left + 8, top + 36,
                        0xFFBFC5D0, true);
            }
        });
    }

    private static void applyClientModules(MinecraftClient client) {
        boolean fullbright = moduleEnabled("FULLBRIGHT");
        if (client.world != null && fullbright) {
            if (!fullbrightApplied) {
                previousGamma = client.options.getGamma().getValue();
                fullbrightApplied = true;
            }
            client.options.getGamma().setValue(16.0);
        } else if (fullbrightApplied) {
            client.options.getGamma().setValue(previousGamma);
            fullbrightApplied = false;
        }

        if (client.player != null && moduleEnabled("SPRINT")) {
            client.player.setSprinting(true);
        }
    }

    private static void applyUtilityModules(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            stopAutoEat(client);
            stopAutoTool(client);
            autoClickTicks = 0;
            tickAutoWalk(client);
            weatherInitialized = false;
            return;
        }
        tickAutoEat(client);
        tickAutoTool(client);
        tickCombatAutomation(client);
        tickAutoWalk(client);
        tickWeatherNotifier(client);
    }

    private static void tickAutoWalk(MinecraftClient client) {
        boolean shouldWalk = moduleEnabled("AUTO_WALK") && client.player != null && client.world != null
                && client.currentScreen == null;
        if (shouldWalk) {
            client.options.forwardKey.setPressed(true);
            autoWalkApplied = true;
        } else if (autoWalkApplied) {
            client.options.forwardKey.setPressed(false);
            autoWalkApplied = false;
        }
    }

    private static void tickCombatAutomation(MinecraftClient client) {
        boolean click = false;
        boolean attacking = moduleEnabled("AUTO_CLICKER") && client.currentScreen == null
                && client.options.attackKey.isPressed();
        if (attacking) {
            autoClickTicks++;
            int interval = Math.max(1, 20 / CONFIG.autoClickCps);
            if (autoClickTicks >= interval) {
                autoClickTicks = 0;
                click = client.player.getAttackCooldownProgress(0.5f) >= 0.99f;
            }
        } else {
            autoClickTicks = 0;
        }

        if (moduleEnabled("TRIGGER_BOT") && client.currentScreen == null
                && client.targetedEntity instanceof LivingEntity target && target.isAlive()
                && client.player.squaredDistanceTo(target) <= 9.0
                && client.player.getAttackCooldownProgress(0.5f) >= 0.99f) {
            click = true;
        }
        if (click) KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(
                client.options.attackKey.getBoundKeyTranslationKey()));
    }

    private static void tickAutoEat(MinecraftClient client) {
        if (!moduleEnabled("AUTO_EAT") || client.currentScreen != null) {
            stopAutoEat(client);
            return;
        }

        var player = client.player;
        if (player.isUsingItem() && !autoEatActive) return;
        if (player.getHungerManager().getFoodLevel() > CONFIG.autoEatThreshold) {
            stopAutoEat(client);
            return;
        }

        int bestSlot = -1;
        double bestScore = -1.0;
        for (int slot = 0; slot < 9; slot++) {
            FoodComponent food = player.getInventory().getStack(slot).get(DataComponentTypes.FOOD);
            if (food == null) continue;
            double score = food.nutrition() + food.saturation() * 2.0;
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        if (bestSlot < 0) {
            stopAutoEat(client);
            return;
        }

        if (!autoEatActive) {
            autoEatOriginalSlot = player.getInventory().selectedSlot;
            autoEatActive = true;
        }
        autoEatSelectedSlot = bestSlot;
        player.getInventory().selectedSlot = bestSlot;
        client.options.useKey.setPressed(true);
    }

    private static void stopAutoEat(MinecraftClient client) {
        if (!autoEatActive) return;
        client.options.useKey.setPressed(false);
        if (client.player != null && client.player.getInventory().selectedSlot == autoEatSelectedSlot) {
            client.player.getInventory().selectedSlot = autoEatOriginalSlot;
        }
        autoEatOriginalSlot = -1;
        autoEatSelectedSlot = -1;
        autoEatActive = false;
    }

    private static void tickAutoTool(MinecraftClient client) {
        if (!moduleEnabled("AUTO_TOOL") || client.currentScreen != null
                || !client.options.attackKey.isPressed()
                || !(client.crosshairTarget instanceof BlockHitResult hit)) {
            stopAutoTool(client);
            return;
        }

        BlockState state = client.world.getBlockState(hit.getBlockPos());
        if (state.isAir()) {
            stopAutoTool(client);
            return;
        }
        var inventory = client.player.getInventory();
        int bestSlot = inventory.selectedSlot;
        float bestSpeed = inventory.getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = inventory.getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        if (!autoToolActive && bestSlot != inventory.selectedSlot) {
            autoToolOriginalSlot = inventory.selectedSlot;
            autoToolActive = true;
        }
        if (autoToolActive) {
            autoToolSelectedSlot = bestSlot;
            inventory.selectedSlot = bestSlot;
        }
    }

    private static void stopAutoTool(MinecraftClient client) {
        if (!autoToolActive) return;
        if (client.player != null && client.player.getInventory().selectedSlot == autoToolSelectedSlot) {
            client.player.getInventory().selectedSlot = autoToolOriginalSlot;
        }
        autoToolOriginalSlot = -1;
        autoToolSelectedSlot = -1;
        autoToolActive = false;
    }

    private static void tickWeatherNotifier(MinecraftClient client) {
        if (!moduleEnabled("WEATHER_NOTIFIER")) {
            weatherInitialized = false;
            return;
        }
        boolean raining = client.world.isRaining();
        boolean thundering = client.world.isThundering();
        if (!weatherInitialized) {
            previousRaining = raining;
            previousThundering = thundering;
            weatherInitialized = true;
            return;
        }
        if (raining == previousRaining && thundering == previousThundering) return;
        String message = thundering ? "Weather: thunderstorm started."
                : raining ? "Weather: rain started." : "Weather cleared.";
        client.player.sendMessage(Text.literal(message), false);
        previousRaining = raining;
        previousThundering = thundering;
    }

    private static boolean moduleEnabled(String moduleName) {
        return CONFIG.moduleToggles.getOrDefault(moduleName, false);
    }

    private static void scanLoadedChunks(MinecraftClient client) {
        Map<Long, ChunkCount> counts = new HashMap<>();
        List<BlockPos> currentStoragePositions = new ArrayList<>();
        List<BlockPos> currentSpawnerPositions = new ArrayList<>();
        List<BlockPos> currentDebrisPositions = new ArrayList<>();
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
                if (moduleEnabled("NETHERITE_FINDER")) {
                    BlockPos.Mutable cursor = new BlockPos.Mutable();
                    for (int localX = 0; localX < 16; localX++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            for (int y = 8; y <= 23; y++) {
                                cursor.set(chunkX * 16 + localX, y, chunkZ * 16 + localZ);
                                if (chunk.getBlockState(cursor).isOf(Blocks.ANCIENT_DEBRIS)) {
                                    currentDebrisPositions.add(cursor.toImmutable());
                                }
                            }
                        }
                    }
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof MobSpawnerBlockEntity) {
                        BlockPos spawner = blockEntity.getPos().toImmutable();
                        currentSpawnerPositions.add(spawner);
                        if (moduleEnabled("SPAWNER_PROTECT") && ALERTED_SPAWNERS.add(spawner)) {
                            client.player.sendMessage(Text.literal("Spawner detected at "
                                    + spawner.getX() + ", " + spawner.getY() + ", " + spawner.getZ()), false);
                        }
                    }
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
        spawnerPositions = List.copyOf(currentSpawnerPositions);
        debrisPositions = List.copyOf(currentDebrisPositions);
        ALERTED_SPAWNERS.retainAll(currentSpawnerPositions);
        holePositions = moduleEnabled("HOLE_ESP") ? findNearbyHoles(client) : List.of();
        dimPositions = moduleEnabled("LIGHT_FINDER") ? findDimSpots(client) : List.of();
    }

    private static List<BlockPos> findNearbyHoles(MinecraftClient client) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = client.player.getBlockPos();
        int radius = Math.min(20, CONFIG.range);
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (client.world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                for (int dy = -4; dy <= 4; dy++) {
                    int y = origin.getY() + dy;
                    cursor.set(x, y, z);
                    BlockPos hole = cursor.toImmutable();
                    if (!client.world.getBlockState(hole).isAir()
                            || !client.world.getBlockState(hole.up()).isAir()
                            || !client.world.getBlockState(hole.down()).isSolidBlock(client.world, hole.down())) {
                        continue;
                    }

                    boolean enclosed = true;
                    for (Direction direction : Direction.Type.HORIZONTAL) {
                        BlockPos side = hole.offset(direction);
                        if (!client.world.getBlockState(side).isSolidBlock(client.world, side)
                                || !client.world.getBlockState(side.up()).isSolidBlock(client.world, side.up())) {
                            enclosed = false;
                            break;
                        }
                    }
                    if (enclosed) found.add(hole);
                }
            }
        }
        return List.copyOf(found);
    }

    private static List<BlockPos> findDimSpots(MinecraftClient client) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = client.player.getBlockPos();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        int radius = 12;
        for (int dx = -radius; dx <= radius && found.size() < 512; dx++) {
            for (int dz = -radius; dz <= radius && found.size() < 512; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (client.world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                for (int dy = -2; dy <= 2 && found.size() < 512; dy++) {
                    cursor.set(x, origin.getY() + dy, z);
                    BlockPos pos = cursor.toImmutable();
                    if (client.world.getBlockState(pos).isAir()
                            && client.world.getLightLevel(pos) <= CONFIG.lightThreshold) {
                        found.add(pos);
                    }
                }
            }
        }
        return List.copyOf(found);
    }

    private static void checkEmptyChunkRelogFlow(MinecraftClient client) {
        if (!CONFIG.relogReminder) return;
        if (CONFIG.emptyChunkState == VantaConfig.EmptyChunkState.REPORTED_NOTHING) return;
        boolean initialCheck = CONFIG.emptyChunkState == VantaConfig.EmptyChunkState.IDLE;
        if (initialCheck && client.player.getBlockPos().getY() > -20) return;
        if (!initialCheck && (!recheckAfterRelog || worldReadyTicks < 40)) return;

        int chunkX = initialCheck ? client.player.getBlockPos().getX() >> 4 : CONFIG.emptyChunkX;
        int chunkZ = initialCheck ? client.player.getBlockPos().getZ() >> 4 : CONFIG.emptyChunkZ;
        WorldChunk chunk = client.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
        if (chunk == null) return;
        boolean foundStorage = chunk.getBlockEntities().values().stream()
                .anyMatch(blockEntity -> isStorage(blockEntity.getCachedState().getBlock()));
        if (foundStorage) {
            CONFIG.emptyChunkState = VantaConfig.EmptyChunkState.IDLE;
            recheckAfterRelog = false;
            CONFIG.save();
            return;
        }

        if (!initialCheck) {
            client.player.sendMessage(Text.literal("Nothing found."), false);
            CONFIG.emptyChunkState = VantaConfig.EmptyChunkState.REPORTED_NOTHING;
            recheckAfterRelog = false;
        } else {
            CONFIG.emptyChunkX = chunkX;
            CONFIG.emptyChunkZ = chunkZ;
            CONFIG.emptyChunkState = VantaConfig.EmptyChunkState.WAITING_FOR_RELOG;
            recheckAfterRelog = false;
            client.player.sendMessage(Text.literal(CONFIG.relogMessage), false);
        }
        CONFIG.save();
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


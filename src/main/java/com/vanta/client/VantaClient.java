package com.vanta.client;

import com.vanta.client.render.VantaRenderLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DisconnectedScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;
import com.mojang.authlib.GameProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private static List<BlockPos> blockEspPositions = List.of();
    private static List<BlockPos> oreSimPositions = List.of();
    private static List<TunnelMarker> tunnelPositions = List.of();
    private static List<BlockPos> seedChunkPositions = List.of();
    private static List<BlockPos> crystalPositions = List.of();
    private static final List<JumpRing> jumpRings = new ArrayList<>();
    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;
    private static KeyBinding pearlKey;
    private static KeyBinding windChargeKey;
    private static KeyBinding cordSnapperKey;
    private static KeyBinding chatMacroKey;
    private static KeyBinding itemDropperKey;
    private static KeyBinding autoSellKey;
    private static KeyBinding autoSpawnerSellKey;
    private static KeyBinding rtpBaseFinderKey;
    private static KeyBinding elytraSwapKey;
    private static KeyBinding fakePlayerKey;
    private static KeyBinding anchorMacroKey;
    private static KeyBinding freecamKey;
    private static KeyBinding fakePayKey;
    private static KeyBinding radioKey;
    private static KeyBinding discordPresenceKey;
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
    private static boolean autoMineApplied;
    private static boolean autoJumpApplied;
    private static boolean previousOnGround;
    private static BlockPos lastWatchedBlock;
    private static BlockState lastWatchedState;
    private static boolean autoLogTriggered;
    private static int quickUseTicks;
    private static int quickUseOriginalSlot = -1;
    private static int quickUseSelectedSlot = -1;
    private static boolean quickUseRestorePressed;
    private static int fireworkCooldown;
    private static int elytraGlideCooldown;
    private static int totemSwapCooldown;
    private static int autoLootDelay;
    private static int autoLootSlot;
    private static int reconnectDelay;
    private static int reconnectAttempts;
    private static int tpaCooldown;
    private static boolean fastPlaceApplied;
    private static boolean fastPlaceRestorePressed;
    private static int antiTrapCooldown;
    private static int autoCrystalCooldown;
    private static int doubleHandCooldown;
    private static int anchorMacroCooldown;
    private static int swingSpeedCooldown;
    private static int combatSwapOriginalSlot = -1;
    private static int combatSwapSelectedSlot = -1;
    private static String combatSwapReason = "";
    private static boolean combatSwapActive;
    private static OtherClientPlayerEntity localFakePlayer;
    private static OtherClientPlayerEntity freecamCamera;
    private static boolean rtpScanActive;
    private static int rtpScanTicksRemaining;
    private static final Set<Long> RTP_ALERTED_CHUNKS = new HashSet<>();
    private static final Set<Long> PRIME_ALERTED_CHUNKS = new HashSet<>();

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.vanta"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vanta"));
        pearlKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.pearl", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.vanta"));
        windChargeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.wind_charge", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.vanta"));
        cordSnapperKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.cord_snapper", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.vanta"));
        chatMacroKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.chat_macro", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.vanta"));
        itemDropperKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.item_dropper", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.vanta"));
        autoSellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.auto_sell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.vanta"));
        autoSpawnerSellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.auto_spawner_sell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.vanta"));
        rtpBaseFinderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.rtp_base_finder", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.vanta"));
        elytraSwapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.elytra_swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.vanta"));
        fakePlayerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.fakeplayer", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.vanta"));
        anchorMacroKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.anchor_macro", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.vanta"));
        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.freecam", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.vanta"));
        fakePayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.fake_pay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, "category.vanta"));
        radioKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.radio", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.vanta"));
        discordPresenceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vanta.discord_presence", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, "category.vanta"));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!moduleEnabled("AUTO_TPA") || overlay || tpaCooldown > 0) return;
            String text = message.getString().toLowerCase(java.util.Locale.ROOT);
            if ((text.contains("requested to teleport") || text.contains("wants to teleport"))
                    && text.contains("you")) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("tpaccept");
                    tpaCooldown = 100;
                }
            }
        });

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

            tickQuickUse(client);
            while (pearlKey.wasPressed()) {
                if (moduleEnabled("KEY_PEARL")) startHotbarUse(client, Items.ENDER_PEARL);
            }
            while (windChargeKey.wasPressed()) {
                if (moduleEnabled("KEY_WIND_CHARGE")) startHotbarUse(client, Items.WIND_CHARGE);
            }
            while (cordSnapperKey.wasPressed()) {
                if (moduleEnabled("CORD_SNAPPER") && client.player != null) {
                    BlockPos pos = client.player.getBlockPos();
                    String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ()
                            + "  (chunk " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + ")";
                    GLFW.glfwSetClipboardString(client.getWindow().getHandle(), coordinates);
                    client.player.sendMessage(Text.literal("Coordinates copied: " + coordinates), true);
                }
            }
            while (chatMacroKey.wasPressed()) {
                if (moduleEnabled("CHAT_MACRO") && client.player != null && client.getNetworkHandler() != null
                        && client.currentScreen == null) {
                    String message = CONFIG.chatMacroMessage.trim();
                    if (message.startsWith("/")) client.getNetworkHandler().sendChatCommand(message.substring(1));
                    else client.getNetworkHandler().sendChatMessage(message);
                }
            }
            while (itemDropperKey.wasPressed()) {
                if (moduleEnabled("ITEM_DROPPER") && client.player != null && client.currentScreen == null) {
                    client.player.dropSelectedItem(true);
                }
            }
            while (autoSellKey.wasPressed()) {
                if (moduleEnabled("AUTO_SELL")) sendConfiguredCommand(client, CONFIG.autoSellCommand);
            }
            while (autoSpawnerSellKey.wasPressed()) {
                if (moduleEnabled("AUTO_SPAWNER_SELL")) sendConfiguredCommand(client, CONFIG.autoSpawnerSellCommand);
            }
            while (rtpBaseFinderKey.wasPressed()) {
                if (moduleEnabled("RTP_BASE_FINDER") && sendConfiguredCommand(client, CONFIG.rtpCommand)) {
                    rtpScanActive = true;
                    rtpScanTicksRemaining = 20 * 60;
                    RTP_ALERTED_CHUNKS.clear();
                    PREVIOUS_LOADED_CHUNKS.clear();
                    chunkFinderInitialized = false;
                }
            }
            while (elytraSwapKey.wasPressed()) {
                if (moduleEnabled("ELYTRA_SWAP")) swapChestWithHotbar(client);
            }
            while (anchorMacroKey.wasPressed()) {
                if (moduleEnabled("ANCHOR_MACRO")) useTargetAnchor(client);
            }
            while (fakePlayerKey.wasPressed()) {
                CONFIG.moduleToggles.put("FAKEPLAYER", !moduleEnabled("FAKEPLAYER"));
                CONFIG.save();
            }
            while (freecamKey.wasPressed()) {
                CONFIG.moduleToggles.put("FREECAM", !moduleEnabled("FREECAM"));
                CONFIG.save();
            }
            while (fakePayKey.wasPressed()) {
                if (moduleEnabled("FAKE_PAY") && client.player != null) {
                    client.player.sendMessage(Text.literal("[Vanta local preview] " + CONFIG.fakePayPreviewText), false);
                }
            }
            while (radioKey.wasPressed()) {
                if (!moduleEnabled("RADIO")) continue;
                if (VantaRadio.isRunning()) {
                    VantaRadio.stop();
                    if (client.player != null) client.player.sendMessage(Text.literal("Vanta Radio: stopped."), true);
                } else {
                    VantaRadio.start(CONFIG.radioUrl,
                            message -> client.execute(() -> {
                                if (client.player != null) client.player.sendMessage(Text.literal(message), true);
                            }));
                }
            }
            while (discordPresenceKey.wasPressed()) {
                if (!moduleEnabled("DISCORD_PRESENCE")) continue;
                if (VantaDiscordPresence.isRunning()) {
                    VantaDiscordPresence.stop();
                    if (client.player != null) client.player.sendMessage(Text.literal("Vanta Presence: stopped."), true);
                } else {
                    VantaDiscordPresence.start(CONFIG.discordApplicationId, CONFIG.discordStatus,
                            message -> client.execute(() -> {
                                if (client.player != null) client.player.sendMessage(Text.literal(message), true);
                            }));
                }
            }

            applyClientModules(client);
            applyUtilityModules(client);
            tickLocalEntityModules(client);
            if (!moduleEnabled("RADIO") && VantaRadio.isRunning()) VantaRadio.stop();
            if (!moduleEnabled("DISCORD_PRESENCE") && VantaDiscordPresence.isRunning()) VantaDiscordPresence.stop();
            if (rtpScanTicksRemaining > 0 && --rtpScanTicksRemaining == 0) rtpScanActive = false;

            if (client.world == null || client.player == null) {
                suspectChunks = List.of();
                storagePositions = List.of();
                spawnerPositions = List.of();
                debrisPositions = List.of();
                holePositions = List.of();
                dimPositions = List.of();
                blockEspPositions = List.of();
                oreSimPositions = List.of();
                tunnelPositions = List.of();
                seedChunkPositions = List.of();
                crystalPositions = List.of();
                rtpScanActive = false;
                rtpScanTicksRemaining = 0;
                jumpRings.clear();
                previousOnGround = false;
                lastWatchedBlock = null;
                lastWatchedState = null;
                autoLogTriggered = false;
                ALERTED_CHUNKS.clear();
                ALERTED_SPAWNERS.clear();
                PREVIOUS_LOADED_CHUNKS.clear();
                SUSPECT_CHUNK_KEYS.clear();
                PRIME_ALERTED_CHUNKS.clear();
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
                    && !moduleEnabled("LIGHT_FINDER") && !moduleEnabled("BLOCK_ESP")
                    && !moduleEnabled("STATIC_HITBOXES") && !moduleEnabled("REAL_HITBOX")
                    && !moduleEnabled("JUMP_CIRCLES") && !moduleEnabled("PEARL_TRAJECTORY")
                    && !moduleEnabled("ORE_SIM")
                    && !moduleEnabled("TUNNEL_BASE_FINDER") && !moduleEnabled("SEED_CHUNK_FINDER")
                    && !moduleEnabled("CRYSTAL_OPTIMIZER") && !moduleEnabled("HITBOX")
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
                    boolean friend = isFriend(target);
                    WorldRenderer.drawBox(matrices, lines, target.getBoundingBox().expand(0.04),
                            friend ? 0.30f : 0.95f, friend ? 0.95f : 0.48f,
                            friend ? 0.56f : 0.82f, 0.95f);
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
                    boolean friend = isFriend(target);
                    WorldRenderer.drawBox(matrices, lines, footprint, friend ? 0.30f : 0.55f,
                            friend ? 0.95f : 0.38f, friend ? 0.56f : 1.0f, 0.75f);
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
            if (moduleEnabled("BLOCK_ESP")) {
                for (BlockPos pos : blockEspPositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    BlockState state = client.world.getBlockState(pos);
                    float red = 0.28f, green = 0.78f, blue = 1.0f;
                    if (state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)) {
                        red = 0.30f; green = 1.0f; blue = 0.43f;
                    } else if (state.isOf(Blocks.ANCIENT_DEBRIS)) {
                        red = 0.42f; green = 0.95f; blue = 0.62f;
                    }
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.02), red, green, blue, 1.0f);
                }
            }
            if (moduleEnabled("ORE_SIM")) {
                for (BlockPos pos : oreSimPositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.015),
                            0.97f, 0.82f, 0.29f, 0.9f);
                }
            }
            if (moduleEnabled("TUNNEL_BASE_FINDER")) {
                for (TunnelMarker marker : tunnelPositions) {
                    BlockPos pos = marker.pos();
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSquared) continue;
                    Box tunnel = marker.direction() == Direction.EAST
                            ? new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 3, pos.getY() + 2, pos.getZ() + 1)
                            : new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 3);
                    WorldRenderer.drawBox(matrices, lines, tunnel, 0.34f, 0.91f, 0.85f, 0.75f);
                }
            }
            if (moduleEnabled("SEED_CHUNK_FINDER")) {
                for (BlockPos pos : seedChunkPositions) {
                    int chunkX = pos.getX();
                    int chunkZ = pos.getZ();
                    double y = client.player.getY() + 0.03;
                    Box footprint = new Box(chunkX * 16.0, y, chunkZ * 16.0,
                            (chunkX + 1) * 16.0, y + 0.05, (chunkZ + 1) * 16.0);
                    WorldRenderer.drawBox(matrices, lines, footprint, 0.72f, 0.55f, 1.0f, 0.8f);
                }
            }
            if (moduleEnabled("CRYSTAL_OPTIMIZER")) {
                for (BlockPos pos : crystalPositions) {
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64.0) continue;
                    WorldRenderer.drawBox(matrices, lines, new Box(pos).expand(0.03),
                            0.70f, 0.42f, 1.0f, 0.8f);
                }
            }
            if (moduleEnabled("HITBOX") && client.targetedEntity instanceof LivingEntity target
                    && target != client.player) {
                WorldRenderer.drawBox(matrices, lines, target.getBoundingBox().expand(0.08),
                        1.0f, 0.26f, 0.28f, 1.0f);
            }
            if (moduleEnabled("STATIC_HITBOXES")) {
                for (PlayerEntity target : client.world.getPlayers()) {
                    if (target == client.player || client.player.squaredDistanceTo(target) > rangeSquared) continue;
                    Box fixed = new Box(target.getX() - 0.3, target.getY(), target.getZ() - 0.3,
                            target.getX() + 0.3, target.getY() + 1.8, target.getZ() + 0.3);
                    WorldRenderer.drawBox(matrices, lines, fixed, 1.0f, 0.32f, 0.34f, 0.9f);
                }
            }
            if (moduleEnabled("REAL_HITBOX")) {
                for (LivingEntity target : client.world.getEntitiesByClass(LivingEntity.class,
                        client.player.getBoundingBox().expand(CONFIG.range), entity -> entity != client.player)) {
                    WorldRenderer.drawBox(matrices, lines, target.getBoundingBox().expand(0.015),
                            0.9f, 0.88f, 0.35f, 0.9f);
                }
            }
            if (moduleEnabled("JUMP_CIRCLES")) {
                var matrix = matrices.peek();
                for (JumpRing ring : jumpRings) {
                    float fade = Math.max(0.0f, 1.0f - ring.age() / 25.0f);
                    double radius = 0.35 + ring.age() * 0.04;
                    for (int segment = 0; segment < 32; segment++) {
                        double a1 = segment * Math.PI * 2.0 / 32.0;
                        double a2 = (segment + 1) * Math.PI * 2.0 / 32.0;
                        lines.vertex(matrix, (float) (ring.x() + Math.cos(a1) * radius), (float) ring.y(),
                                        (float) (ring.z() + Math.sin(a1) * radius))
                                .color(0.62f, 0.46f, 1.0f, fade).normal(matrix, 0.0f, 1.0f, 0.0f);
                        lines.vertex(matrix, (float) (ring.x() + Math.cos(a2) * radius), (float) ring.y(),
                                        (float) (ring.z() + Math.sin(a2) * radius))
                                .color(0.62f, 0.46f, 1.0f, fade).normal(matrix, 0.0f, 1.0f, 0.0f);
                    }
                }
            }
            if (moduleEnabled("PEARL_TRAJECTORY")) {
                drawPearlTrajectory(client, lines, matrices.peek());
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
            if (moduleEnabled("MUSIC_HUD")) {
                int row = 0;
                for (BlockEntity blockEntity : client.world.getBlockEntities().values()) {
                    if (blockEntity instanceof JukeboxBlockEntity jukebox && !jukebox.getStack().isEmpty()
                            && client.player.squaredDistanceTo(Vec3d.ofCenter(jukebox.getPos())) <= 4096.0) {
                        String playing = "Now playing: " + jukebox.getStack().getName().getString();
                        context.drawText(client.textRenderer, Text.literal(playing), 8, 36 + row * 12,
                                0xFFE6D5FF, true);
                        if (++row >= 3) break;
                    }
                }
            }
            if (moduleEnabled("FAKE_STATS")) {
                int top = moduleEnabled("HUD") ? 50 : 8;
                context.drawText(client.textRenderer, Text.literal("[LOCAL STATS PREVIEW] " + CONFIG.fakeStatsText),
                        8, top, 0xFFFFCF79, true);
            }
            if (moduleEnabled("TARGET_HUD") && client.targetedEntity instanceof LivingEntity target
                    && target.isAlive() && target != client.player) {
                int left = 8;
                int top = moduleEnabled("HUD") ? 42 : 8;
                int right = 190;
                context.fill(left, top, right, top + 43, 0xD014171E);
                context.fill(left, top, right, top + 1, 0xFF9A7BFF);
                String targetName = target.getDisplayName().getString();
                boolean friend = target instanceof PlayerEntity playerTarget && isFriend(playerTarget);
                if (friend) targetName = "Friend  " + targetName;
                context.drawText(client.textRenderer, Text.literal(targetName), left + 8, top + 7,
                        friend ? 0xFF74E49A : 0xFFE6E9EF, true);
                float health = Math.max(0.0f, Math.min(1.0f, target.getHealth() / target.getMaxHealth()));
                context.fill(left + 8, top + 28, right - 8, top + 34, 0xFF343A46);
                context.fill(left + 8, top + 28, left + 8 + (int) ((right - left - 16) * health),
                        top + 34, 0xFF9A7BFF);
                String healthText = String.format("%.1f / %.1f", target.getHealth(), target.getMaxHealth());
                context.drawText(client.textRenderer, Text.literal(healthText), left + 8, top + 36,
                        0xFFBFC5D0, true);
            }
            if (moduleEnabled("NAME_TAGS")) {
                int top = moduleEnabled("HUD") ? 42 : 8;
                for (PlayerEntity target : client.world.getPlayers()) {
                    if (target == client.player || client.player.squaredDistanceTo(target) > 4096.0) continue;
                    top += moduleEnabled("TARGET_HUD") ? 48 : 0;
                    boolean friend = isFriend(target);
                    String label = target.getName().getString() + "  "
                            + Math.round(client.player.distanceTo(target)) + "m";
                    context.drawText(client.textRenderer, Text.literal(label), 8, top,
                            friend ? 0xFF74E49A : 0xFFE6E9EF, true);
                    top += 12;
                }
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

    private static void tickLocalEntityModules(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            localFakePlayer = null;
            freecamCamera = null;
            return;
        }

        if (moduleEnabled("FAKEPLAYER")) {
            if (localFakePlayer == null || localFakePlayer.getWorld() != client.world) {
                localFakePlayer = createLocalPlayer(client, "VantaDummy", -730001);
            }
        } else if (localFakePlayer != null) {
            localFakePlayer.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            localFakePlayer = null;
        }

        if (moduleEnabled("FREECAM") || moduleEnabled("FREE_LOOK")) {
            if (freecamCamera == null || freecamCamera.getWorld() != client.world) {
                freecamCamera = createLocalPlayer(client, "VantaCamera", -730002);
                freecamCamera.setInvisible(true);
                freecamCamera.setNoGravity(true);
            }
            client.setCameraEntity(freecamCamera);
            if (moduleEnabled("FREECAM")) {
                float yaw = (float) Math.toRadians(freecamCamera.getYaw());
                double forward = (client.options.forwardKey.isPressed() ? 1.0 : 0.0)
                        - (client.options.backKey.isPressed() ? 1.0 : 0.0);
                double side = (client.options.leftKey.isPressed() ? 1.0 : 0.0)
                        - (client.options.rightKey.isPressed() ? 1.0 : 0.0);
                double speed = client.options.sprintKey.isPressed() ? 0.65 : 0.35;
                double vx = (-Math.sin(yaw) * forward + Math.cos(yaw) * side) * speed;
                double vz = (Math.cos(yaw) * forward + Math.sin(yaw) * side) * speed;
                double vy = (client.options.jumpKey.isPressed() ? speed : 0.0)
                        - (client.options.sneakKey.isPressed() ? speed : 0.0);
                freecamCamera.setPosition(freecamCamera.getX() + vx,
                        freecamCamera.getY() + vy, freecamCamera.getZ() + vz);
            } else {
                freecamCamera.setPosition(client.player.getX(), client.player.getY() + 0.1, client.player.getZ());
            }
        } else if (freecamCamera != null) {
            if (client.getCameraEntity() == freecamCamera) client.setCameraEntity(client.player);
            freecamCamera.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            freecamCamera = null;
        }
    }

    private static OtherClientPlayerEntity createLocalPlayer(MinecraftClient client, String name, int id) {
        UUID uuid = UUID.nameUUIDFromBytes((name + client.player.getUuid()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        OtherClientPlayerEntity entity = new OtherClientPlayerEntity(client.world, new GameProfile(uuid, name));
        entity.setId(id);
        entity.copyPositionAndRotation(client.player);
        client.world.addEntity(entity);
        return entity;
    }

    private static void applyUtilityModules(MinecraftClient client) {
        tickAutoReconnect(client);
        tickAutoLoot(client);
        tickFastPlace(client);
        tickAntiTrap(client);
        if (tpaCooldown > 0) tpaCooldown--;
        if (client.player == null || client.world == null) {
            stopAutoEat(client);
            stopAutoTool(client);
            autoClickTicks = 0;
            tickAutoWalk(client);
            tickAutoMine(client);
            tickAutoJumpReset(client);
            weatherInitialized = false;
            fireworkCooldown = 0;
            elytraGlideCooldown = 0;
            return;
        }
        tickAutoEat(client);
        tickAutoTool(client);
        tickTotemModules(client);
        tickAdvancedCombat(client);
        tickCombatAutomation(client);
        tickAutoWalk(client);
        tickAutoMine(client);
        tickAutoJumpReset(client);
        tickJumpCircles(client);
        tickBlockNotifier(client);
        tickAutoLog(client);
        tickWeatherNotifier(client);
        tickElytraGlide(client);
        tickAutoFirework(client);
    }

    private static boolean sendConfiguredCommand(MinecraftClient client, String command) {
        if (client.player == null || client.getNetworkHandler() == null || command == null || command.isBlank()) {
            return false;
        }
        String normalized = command.trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        client.getNetworkHandler().sendChatCommand(normalized);
        return true;
    }

    private static void tickAutoReconnect(MinecraftClient client) {
        if (client.player != null && client.world != null) {
            reconnectAttempts = 0;
            reconnectDelay = 0;
            return;
        }
        if (!moduleEnabled("AUTO_RECONNECT") || client.isInSingleplayer()
                || !(client.currentScreen instanceof DisconnectedScreen)
                || client.getCurrentServerEntry() == null || reconnectAttempts >= 3) return;
        if (reconnectDelay > 0) {
            reconnectDelay--;
            return;
        }
        ServerInfo server = client.getCurrentServerEntry();
        ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(server.address), server, false, null);
        reconnectAttempts++;
        reconnectDelay = 20 * 15;
    }

    private static void tickAutoLoot(MinecraftClient client) {
        if (!moduleEnabled("AUTO_LOOT") || client.player == null || client.interactionManager == null
                || !(client.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            autoLootDelay = 0;
            autoLootSlot = 0;
            return;
        }
        if (autoLootDelay > 0) {
            autoLootDelay--;
            return;
        }
        int containerSlots = handler.getRows() * 9;
        while (autoLootSlot < containerSlots) {
            int slot = autoLootSlot++;
            if (!handler.getSlot(slot).hasStack()) continue;
            client.interactionManager.clickSlot(handler.syncId, slot, 0,
                    SlotActionType.QUICK_MOVE, client.player);
            autoLootDelay = 2;
            return;
        }
        autoLootSlot = 0;
    }

    private static void tickFastPlace(MinecraftClient client) {
        boolean shouldPlace = moduleEnabled("FAST_PLACE") && quickUseTicks == 0
                && client.currentScreen == null && client.player != null && client.world != null
                && client.crosshairTarget instanceof BlockHitResult
                && client.player.getMainHandStack().getItem() instanceof BlockItem;
        if (shouldPlace && !fastPlaceApplied) {
            fastPlaceRestorePressed = client.options.useKey.isPressed();
            fastPlaceApplied = true;
        }
        if (shouldPlace) {
            client.options.useKey.setPressed(true);
        } else if (fastPlaceApplied) {
            client.options.useKey.setPressed(fastPlaceRestorePressed);
            fastPlaceApplied = false;
            fastPlaceRestorePressed = false;
        }
    }

    private static void tickAntiTrap(MinecraftClient client) {
        if (antiTrapCooldown > 0) antiTrapCooldown--;
        if (!moduleEnabled("ANTI_TRAP") || client.currentScreen != null || client.player == null
                || !client.player.isInsideWall() || antiTrapCooldown > 0) return;
        KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(
                client.options.jumpKey.getBoundKeyTranslationKey()));
        antiTrapCooldown = 8;
    }

    private static void tickJumpCircles(MinecraftClient client) {
        for (int i = jumpRings.size() - 1; i >= 0; i--) {
            JumpRing ring = jumpRings.get(i);
            if (ring.age() >= 25) jumpRings.remove(i);
            else jumpRings.set(i, new JumpRing(ring.x(), ring.y(), ring.z(), ring.age() + 1));
        }
        if (moduleEnabled("JUMP_CIRCLES") && client.player != null && client.player.isOnGround()
                && !previousOnGround) {
            jumpRings.add(new JumpRing(client.player.getX(), client.player.getY() + 0.03,
                    client.player.getZ(), 0));
        }
        previousOnGround = client.player != null && client.player.isOnGround();
    }

    private static void tickBlockNotifier(MinecraftClient client) {
        if (!moduleEnabled("BLOCK_NOTIFIER") || client.currentScreen != null
                || client.player == null || client.world == null) {
            lastWatchedBlock = null;
            lastWatchedState = null;
            return;
        }
        if (lastWatchedBlock != null) {
            BlockState current = client.world.getBlockState(lastWatchedBlock);
            if (!current.equals(lastWatchedState)) {
                client.player.sendMessage(Text.literal("Block changed at " + lastWatchedBlock.getX() + ", "
                        + lastWatchedBlock.getY() + ", " + lastWatchedBlock.getZ()), false);
                lastWatchedState = current;
            }
        }
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos target = hit.getBlockPos().toImmutable();
            if (!target.equals(lastWatchedBlock)) {
                lastWatchedBlock = target;
                lastWatchedState = client.world.getBlockState(target);
            }
        }
    }

    private static void tickAutoLog(MinecraftClient client) {
        if (!moduleEnabled("AUTO_LOG") || client.currentScreen != null || client.player == null
                || client.world == null || client.isInSingleplayer()
                || client.player.getHealth() > CONFIG.autoLogHealth) {
            autoLogTriggered = false;
            return;
        }
        if (autoLogTriggered || client.getNetworkHandler() == null) return;
        autoLogTriggered = true;
        client.getNetworkHandler().getConnection().disconnect(
                Text.literal("Vanta Auto Log: health threshold reached"));
    }

    private static boolean startHotbarUse(MinecraftClient client, Item item) {
        if (client.player == null || client.world == null || client.currentScreen != null || quickUseTicks > 0) {
            return false;
        }
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            client.player.sendMessage(Text.literal("Vanta: item not found in hotbar"), true);
            return false;
        }
        quickUseRestorePressed = client.options.useKey.isPressed();
        quickUseOriginalSlot = client.player.getInventory().selectedSlot;
        quickUseSelectedSlot = slot;
        client.player.getInventory().selectedSlot = slot;
        client.options.useKey.setPressed(true);
        quickUseTicks = 2;
        return true;
    }

    private static void tickQuickUse(MinecraftClient client) {
        if (quickUseTicks <= 0) return;
        if (quickUseTicks > 1) {
            quickUseTicks--;
            return;
        }
        client.options.useKey.setPressed(quickUseRestorePressed);
        if (client.player != null && client.player.getInventory().selectedSlot == quickUseSelectedSlot) {
            client.player.getInventory().selectedSlot = quickUseOriginalSlot;
        }
        quickUseTicks = 0;
        quickUseOriginalSlot = -1;
        quickUseSelectedSlot = -1;
        quickUseRestorePressed = false;
    }

    private static void tickElytraGlide(MinecraftClient client) {
        if (elytraGlideCooldown > 0) elytraGlideCooldown--;
        if (!moduleEnabled("ELYTRA_GLIDE") || client.currentScreen != null || client.player == null
                || client.player.isOnGround() || client.player.isFallFlying() || elytraGlideCooldown > 0
                || !client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
                || client.player.getVelocity().y >= -0.08) return;
        KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(
                client.options.jumpKey.getBoundKeyTranslationKey()));
        elytraGlideCooldown = 20;
    }

    private static void tickAutoFirework(MinecraftClient client) {
        if (fireworkCooldown > 0) fireworkCooldown--;
        if (!moduleEnabled("AUTO_FIREWORK") || client.currentScreen != null || client.player == null
                || !client.player.isFallFlying() || fireworkCooldown > 0 || quickUseTicks > 0) return;
        boolean hasRocket = false;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                hasRocket = true;
                break;
            }
        }
        if (hasRocket && startHotbarUse(client, Items.FIREWORK_ROCKET)) fireworkCooldown = 40;
    }

    private static void tickTotemModules(MinecraftClient client) {
        if (totemSwapCooldown > 0) totemSwapCooldown--;
        if (client.player == null || client.world == null || client.currentScreen != null
                || client.interactionManager == null || totemSwapCooldown > 0) return;
        boolean keepOffhandReady = moduleEnabled("TOTEM_OFFHAND");
        boolean lowHealth = client.player.getHealth() <= CONFIG.totemHealthThreshold;
        boolean needsTotem = keepOffhandReady
                || (moduleEnabled("AUTO_TOTEM") && lowHealth)
                || (moduleEnabled("AUTO_INV_TOTEM") && client.player.getHealth() <= 6.0f)
                || (moduleEnabled("HOVER_TOTEM") && !client.player.isOnGround()
                && client.player.getVelocity().y < -0.03 && lowHealth);
        if (!needsTotem || client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        var inventory = client.player.getInventory();
        for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
            if (!inventory.getStack(inventorySlot).isOf(Items.TOTEM_OF_UNDYING)) continue;
            int handlerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,
                    handlerSlot, 40, SlotActionType.SWAP, client.player);
            totemSwapCooldown = 20;
            return;
        }
    }

    private static void drawPearlTrajectory(MinecraftClient client, VertexConsumer lines,
                                             org.joml.Matrix4f matrix) {
        if (client.player == null || client.world == null
                || (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL)
                && !client.player.getOffHandStack().isOf(Items.ENDER_PEARL))) return;
        Vec3d velocity = client.player.getRotationVec(1.0f).multiply(1.5);
        Vec3d position = client.player.getEyePos().add(velocity.normalize().multiply(0.15));
        for (int step = 0; step < 80; step++) {
            Vec3d next = position.add(velocity);
            BlockHitResult hit = client.world.raycast(new RaycastContext(position, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            Vec3d end = hit.getType() == HitResult.Type.MISS ? next : hit.getPos();
            lines.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                    .color(0.75f, 0.53f, 1.0f, 0.95f).normal(matrix, 0.0f, 1.0f, 0.0f);
            lines.vertex(matrix, (float) end.x, (float) end.y, (float) end.z)
                    .color(0.75f, 0.53f, 1.0f, 0.95f).normal(matrix, 0.0f, 1.0f, 0.0f);
            if (hit.getType() != HitResult.Type.MISS) break;
            position = next;
            velocity = velocity.multiply(0.99).add(0.0, -0.03, 0.0);
        }
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

    private static void tickAutoMine(MinecraftClient client) {
        boolean shouldMine = moduleEnabled("AUTO_MINE") && client.player != null && client.world != null
                && client.currentScreen == null && client.crosshairTarget instanceof BlockHitResult;
        if (shouldMine) {
            client.options.attackKey.setPressed(true);
            autoMineApplied = true;
        } else if (autoMineApplied) {
            client.options.attackKey.setPressed(false);
            autoMineApplied = false;
        }
    }

    private static void tickAutoJumpReset(MinecraftClient client) {
        boolean shouldJump = moduleEnabled("AUTO_JUMP_RESET") && client.player != null && client.world != null
                && client.currentScreen == null && client.player.isOnGround() && client.player.hurtTime > 0;
        if (shouldJump) {
            client.options.jumpKey.setPressed(true);
            autoJumpApplied = true;
        } else if (autoJumpApplied) {
            client.options.jumpKey.setPressed(false);
            autoJumpApplied = false;
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
                click = moduleEnabled("NO_HIT_DELAY")
                        || client.player.getAttackCooldownProgress(0.5f) >= 0.99f;
            }
        } else {
            autoClickTicks = 0;
        }

        if (moduleEnabled("TRIGGER_BOT") && client.currentScreen == null
                && client.targetedEntity instanceof LivingEntity target && target.isAlive()
                && client.player.squaredDistanceTo(target) <= 9.0
                && (moduleEnabled("NO_HIT_DELAY")
                || client.player.getAttackCooldownProgress(0.5f) >= 0.99f)) {
            click = true;
        }
        if (click) KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(
                client.options.attackKey.getBoundKeyTranslationKey()));
    }

    private static void tickAdvancedCombat(MinecraftClient client) {
        if (autoCrystalCooldown > 0) autoCrystalCooldown--;
        if (doubleHandCooldown > 0) doubleHandCooldown--;
        if (anchorMacroCooldown > 0) anchorMacroCooldown--;
        if (client.player == null || client.world == null || client.currentScreen != null
                || client.interactionManager == null) {
            stopCombatSwap(client);
            return;
        }

        if (moduleEnabled("AIM_ASSIST") && client.options.attackKey.isPressed()) {
            PlayerEntity target = client.world.getPlayers().stream()
                    .filter(candidate -> candidate != client.player && candidate.isAlive()
                            && client.player.squaredDistanceTo(candidate) <= 20.25
                            && client.player.canSee(candidate))
                    .filter(candidate -> angleDifference(client.player.getYaw(), yawTo(client.player, candidate)) < 24.0f)
                    .min(java.util.Comparator.comparingDouble(client.player::squaredDistanceTo)).orElse(null);
            if (target != null) {
                float yaw = yawTo(client.player, target);
                float pitch = pitchTo(client.player, target);
                client.player.setYaw(approachAngle(client.player.getYaw(), yaw, 2.0f));
                client.player.setPitch(approachAngle(client.player.getPitch(), pitch, 1.6f));
            }
        }

        if (moduleEnabled("AUTO_CRYSTAL") && autoCrystalCooldown == 0
                && client.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                && client.crosshairTarget instanceof BlockHitResult hit) {
            BlockState base = client.world.getBlockState(hit.getBlockPos());
            if (base.isOf(Blocks.OBSIDIAN) || base.isOf(Blocks.BEDROCK)) {
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
                client.player.swingHand(Hand.MAIN_HAND);
                autoCrystalCooldown = 4;
            }
        }

        if (moduleEnabled("AUTO_HIT_CRYSTAL") && client.targetedEntity instanceof EndCrystalEntity crystal
                && client.player.getAttackCooldownProgress(0.5f) >= 0.99f) {
            client.interactionManager.attackEntity(client.player, crystal);
            client.player.swingHand(Hand.MAIN_HAND);
        }

        if (moduleEnabled("AUTO_DOUBLE_HAND") && client.options.useKey.isPressed()
                && !client.player.getOffHandStack().isEmpty() && doubleHandCooldown == 0) {
            client.interactionManager.interactItem(client.player, Hand.OFF_HAND);
            client.player.swingHand(Hand.OFF_HAND);
            doubleHandCooldown = 6;
        }

        boolean doubleAnchor = moduleEnabled("DOUBLE_ANCHOR") && client.options.useKey.isPressed()
                && client.crosshairTarget instanceof BlockHitResult anchorHit
                && client.world.getBlockState(anchorHit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
        if (doubleAnchor && anchorMacroCooldown == 0) {
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                    (BlockHitResult) client.crosshairTarget);
            anchorMacroCooldown = 8;
        }

        PlayerEntity shieldTarget = client.targetedEntity instanceof PlayerEntity playerTarget
                && playerTarget.isBlocking() && client.options.attackKey.isPressed() ? playerTarget : null;
        boolean fallingMace = client.player.getVelocity().y < -0.18
                && client.targetedEntity instanceof LivingEntity target && target.isAlive()
                && client.player.squaredDistanceTo(target) <= 25.0;
        boolean tridentSituation = client.player.isSubmergedInWater()
                && client.targetedEntity instanceof LivingEntity target && target.isAlive();
        if (moduleEnabled("SHIELD_BREAKER") && shieldTarget != null) {
            selectCombatItem(client, item -> item instanceof AxeItem, "shield");
        } else if (moduleEnabled("MACE_SWAP") && fallingMace) {
            selectCombatItem(client, item -> item == Items.MACE, "mace");
        } else if (moduleEnabled("SPEAR_SWAP") && tridentSituation) {
            selectCombatItem(client, item -> item == Items.TRIDENT, "trident");
        } else {
            stopCombatSwap(client);
        }

        if (moduleEnabled("MACE_BOMBER") && fallingMace
                && client.player.getMainHandStack().isOf(Items.MACE)
                && (moduleEnabled("NO_HIT_DELAY")
                || client.player.getAttackCooldownProgress(0.5f) >= 0.99f)) {
            client.interactionManager.attackEntity(client.player, client.targetedEntity);
            client.player.swingHand(Hand.MAIN_HAND);
        }
        if (swingSpeedCooldown > 0) swingSpeedCooldown--;
        if (moduleEnabled("SWING_SPEED") && client.options.attackKey.isPressed()
                && swingSpeedCooldown == 0) {
            client.player.swingHand(Hand.MAIN_HAND);
            swingSpeedCooldown = 3;
        }
    }

    private static void selectCombatItem(MinecraftClient client,
                                         java.util.function.Predicate<Item> matches, String reason) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (matches.test(client.player.getInventory().getStack(i).getItem())) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            stopCombatSwap(client);
            return;
        }
        if (!combatSwapActive) {
            combatSwapOriginalSlot = client.player.getInventory().selectedSlot;
            combatSwapActive = true;
        }
        combatSwapReason = reason;
        combatSwapSelectedSlot = slot;
        client.player.getInventory().selectedSlot = slot;
    }

    private static void stopCombatSwap(MinecraftClient client) {
        if (!combatSwapActive) return;
        if (client.player != null && client.player.getInventory().selectedSlot == combatSwapSelectedSlot) {
            client.player.getInventory().selectedSlot = combatSwapOriginalSlot;
        }
        combatSwapOriginalSlot = -1;
        combatSwapSelectedSlot = -1;
        combatSwapReason = "";
        combatSwapActive = false;
    }

    private static float yawTo(PlayerEntity player, PlayerEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    private static float pitchTo(PlayerEntity player, PlayerEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = target.getEyeY() - player.getEyeY();
        return (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float delta = ((target - current + 540.0f) % 360.0f) - 180.0f;
        return current + Math.max(-maxStep, Math.min(maxStep, delta));
    }

    private static float angleDifference(float current, float target) {
        return Math.abs(((target - current + 540.0f) % 360.0f) - 180.0f);
    }

    private static void swapChestWithHotbar(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.currentScreen != null) return;
        int selected = client.player.getInventory().selectedSlot;
        boolean hasElytra = client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        int candidate = -1;
        for (int slot = 0; slot < 9; slot++) {
            Item item = client.player.getInventory().getStack(slot).getItem();
            if ((hasElytra && slot != selected && !client.player.getInventory().getStack(slot).isEmpty())
                    || (!hasElytra && item == Items.ELYTRA)) {
                candidate = slot;
                break;
            }
        }
        if (candidate < 0) {
            client.player.sendMessage(Text.literal("Vanta: no chest item or elytra in hotbar"), true);
            return;
        }
        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 6, candidate,
                SlotActionType.SWAP, client.player);
    }

    private static void useTargetAnchor(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null
                || !(client.crosshairTarget instanceof BlockHitResult hit)
                || !client.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) return;
        Hand hand = client.player.getOffHandStack().isOf(Items.GLOWSTONE)
                ? Hand.OFF_HAND : Hand.MAIN_HAND;
        client.interactionManager.interactBlock(client.player, hand, hit);
        client.player.swingHand(hand);
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

    public static boolean isModuleEnabled(String moduleName) {
        return moduleEnabled(moduleName);
    }

    public static OtherClientPlayerEntity getFreecamCamera() {
        return freecamCamera;
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
                boolean hasStorage = false;
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
                    hasStorage = true;
                    BlockPos pos = blockEntity.getPos();
                    currentStoragePositions.add(pos.toImmutable());
                    ChunkCount count = counts.get(key);
                    if (count == null) {
                        count = new ChunkCount(chunkX, chunkZ);
                        counts.put(key, count);
                    }
                    count.count++;
                }
                if (rtpScanActive && hasStorage && RTP_ALERTED_CHUNKS.add(key)) {
                    client.player.sendMessage(Text.literal("Possible storage in RTP chunk "
                            + chunkX + ", " + chunkZ), false);
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
            ChunkCount chunk = entry.getValue();
            if (moduleEnabled("PRIME_CHUNK_FINDER")
                    && chunk.count >= Math.max(CONFIG.containersPerChunk * 2, 16)
                    && PRIME_ALERTED_CHUNKS.add(entry.getKey())) {
                client.player.sendMessage(Text.literal("Prime chunk indicator at " + chunk.x + ", " + chunk.z
                        + " (" + chunk.count + " loaded storage blocks)"), false);
            }
            if (!CONFIG.susChunkFinder) continue;
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
        blockEspPositions = moduleEnabled("BLOCK_ESP") ? findNearbyOreBlocks(client, false) : List.of();
        oreSimPositions = moduleEnabled("ORE_SIM") ? findNearbyOreBlocks(client, true) : List.of();
        tunnelPositions = moduleEnabled("TUNNEL_BASE_FINDER") ? findNearbyTunnels(client) : List.of();
        seedChunkPositions = moduleEnabled("SEED_CHUNK_FINDER") ? findSeedChunks(client) : List.of();
        crystalPositions = moduleEnabled("CRYSTAL_OPTIMIZER") ? findCrystalPositions(client) : List.of();
    }

    private static List<BlockPos> findCrystalPositions(MinecraftClient client) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = client.player.getBlockPos();
        int radius = 8;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (client.world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos base = new BlockPos(x, origin.getY() + dy, z);
                    if (!client.world.getBlockState(base).isOf(Blocks.OBSIDIAN)
                            && !client.world.getBlockState(base).isOf(Blocks.BEDROCK)) continue;
                    BlockPos spot = base.up();
                    if (client.world.getBlockState(spot).isAir()
                            && client.world.getBlockState(spot.up()).isAir()) found.add(spot);
                }
            }
        }
        return List.copyOf(found);
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

    private static List<BlockPos> findNearbyOreBlocks(MinecraftClient client, boolean includeCommonOres) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = client.player.getBlockPos();
        int radius = Math.min(20, CONFIG.range);
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (client.world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                for (int dy = -16; dy <= 16; dy++) {
                    cursor.set(x, origin.getY() + dy, z);
                    BlockPos pos = cursor.toImmutable();
                    BlockState state = client.world.getBlockState(pos);
                    if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
                            || state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)
                            || state.isOf(Blocks.ANCIENT_DEBRIS)
                            || (includeCommonOres && isCommonOre(state))) {
                        found.add(pos);
                    }
                }
            }
        }
        return List.copyOf(found);
    }

    private static List<TunnelMarker> findNearbyTunnels(MinecraftClient client) {
        List<TunnelMarker> found = new ArrayList<>();
        BlockPos origin = client.player.getBlockPos();
        int radius = Math.min(12, CONFIG.range);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (client.world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                for (int dy = -8; dy <= 8; dy++) {
                    BlockPos start = new BlockPos(x, origin.getY() + dy, z);
                    for (Direction direction : List.of(Direction.EAST, Direction.SOUTH)) {
                        if (isTunnelColumn(client, start.offset(direction.getOpposite()), direction)) continue;
                        if (!isTunnelColumn(client, start, direction)
                                || !isTunnelColumn(client, start.offset(direction), direction)
                                || !isTunnelColumn(client, start.offset(direction, 2), direction)) continue;
                        found.add(new TunnelMarker(start.toImmutable(), direction));
                    }
                }
            }
        }
        return List.copyOf(found);
    }

    private static boolean isTunnelColumn(MinecraftClient client, BlockPos pos, Direction direction) {
        int x = pos.getX();
        int z = pos.getZ();
        if (client.world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) return false;
        BlockPos above = pos.up();
        BlockPos floor = pos.down();
        BlockPos ceiling = pos.up(2);
        if (!client.world.getBlockState(pos).isAir() || !client.world.getBlockState(above).isAir()
                || !isSolidTerrain(client, floor) || !isSolidTerrain(client, ceiling)) return false;
        Direction sideA = direction == Direction.EAST ? Direction.NORTH : Direction.EAST;
        Direction sideB = sideA.getOpposite();
        return isSolidTerrain(client, pos.offset(sideA)) && isSolidTerrain(client, pos.offset(sideB))
                && isSolidTerrain(client, above.offset(sideA)) && isSolidTerrain(client, above.offset(sideB));
    }

    private static boolean isSolidTerrain(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static List<BlockPos> findSeedChunks(MinecraftClient client) {
        List<BlockPos> found = new ArrayList<>();
        if (CONFIG.seedFinderSeed.isBlank()) return List.of();
        long seed;
        try {
            seed = Long.parseLong(CONFIG.seedFinderSeed.trim());
        } catch (NumberFormatException ignored) {
            return List.of();
        }
        int centerX = client.player.getBlockPos().getX() >> 4;
        int centerZ = client.player.getBlockPos().getZ() >> 4;
        int radius = Math.min(12, (CONFIG.range + 15) / 16 + 1);
        for (int chunkX = centerX - radius; chunkX <= centerX + radius; chunkX++) {
            for (int chunkZ = centerZ - radius; chunkZ <= centerZ + radius; chunkZ++) {
                if (client.world.getChunkManager().getWorldChunk(chunkX, chunkZ) == null) continue;
                long mixedSeed = seed + (long) (chunkX * chunkX * 4987142)
                        + (long) (chunkX * 5947611) + (long) (chunkZ * chunkZ) * 4392871L
                        + (long) (chunkZ * 389711) ^ 987234911L;
                if (new java.util.Random(mixedSeed).nextInt(10) == 0) {
                    found.add(new BlockPos(chunkX, 0, chunkZ));
                }
            }
        }
        return List.copyOf(found);
    }

    private static boolean isCommonOre(BlockState state) {
        return state.isOf(Blocks.COAL_ORE) || state.isOf(Blocks.DEEPSLATE_COAL_ORE)
                || state.isOf(Blocks.IRON_ORE) || state.isOf(Blocks.DEEPSLATE_IRON_ORE)
                || state.isOf(Blocks.COPPER_ORE) || state.isOf(Blocks.DEEPSLATE_COPPER_ORE)
                || state.isOf(Blocks.GOLD_ORE) || state.isOf(Blocks.DEEPSLATE_GOLD_ORE)
                || state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.isOf(Blocks.LAPIS_ORE) || state.isOf(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.isOf(Blocks.NETHER_QUARTZ_ORE) || state.isOf(Blocks.NETHER_GOLD_ORE);
    }

    private static boolean isFriend(PlayerEntity player) {
        if (!moduleEnabled("FRIENDS") || CONFIG.friendNames.isBlank()) return false;
        String name = player.getName().getString();
        for (String entry : CONFIG.friendNames.split("[,\\s]+")) {
            if (!entry.isBlank() && entry.equalsIgnoreCase(name)) return true;
        }
        return false;
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

    private record TunnelMarker(BlockPos pos, Direction direction) {
    }

    private record JumpRing(double x, double y, double z, int age) {
    }
}


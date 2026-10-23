package com.vanta.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Vape-style category / module / settings layout for Vanta Client. */
public final class VantaScreen extends Screen {
    private static final int BG = 0xF014171E;
    private static final int RAIL = 0xF00F1117;
    private static final int PANEL = 0xFF1A1E27;
    private static final int PANEL_HOVER = 0xFF242A35;
    private static final int BORDER = 0xFF303642;
    private static final int TEXT = 0xFFE6E9EF;
    private static final int MUTED = 0xFF969EAD;
    private static final int ACCENT = 0xFF9A7BFF;
    private static final int ALERT = 0xFFFF6D62;

    private final Screen parent;
    private final VantaConfig config;
    private Category category = Category.RENDER;
    private Module selectedModule = Module.STORAGE_ESP;
    private String focusedField = "";
    private String activeSlider = "";
    private int moduleScroll = 6;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int railWidth;
    private int listX;
    private int listWidth;
    private int settingsX;
    private int settingsWidth;
    private int contentY;

    public VantaScreen(Screen parent, VantaConfig config) {
        super(Text.literal("Vanta Client"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        layout();

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 46, 0xFF10131A);
        context.fill(panelX, panelY + 46, panelX + railWidth, panelY + panelHeight, RAIL);
        context.fill(panelX + railWidth, panelY + 46, panelX + railWidth + listWidth, panelY + panelHeight, 0xFF151820);
        context.fill(panelX + railWidth + listWidth, panelY + 46, panelX + railWidth + listWidth + 1,
                panelY + panelHeight, BORDER);
        context.fill(panelX, panelY + 46, panelX + panelWidth, panelY + 47, BORDER);
        context.fill(panelX, panelY + panelHeight - 25, panelX + panelWidth, panelY + panelHeight - 24, BORDER);

        context.fill(panelX + 15, panelY + 11, panelX + 39, panelY + 35, ACCENT);
        centered(context, "V", panelX + 27, panelY + 16, 0xFF151020);
        draw(context, "VANTA CLIENT", panelX + 48, panelY + 11, TEXT);
        draw(context, "FABRIC  /  1.21.1", panelX + 48, panelY + 26, MUTED);
        draw(context, "RSHIFT", panelX + panelWidth - 86, panelY + 17, MUTED);
        draw(context, "×", panelX + panelWidth - 27, panelY + 12, TEXT);

        drawCategoryRail(context, mouseX, mouseY);
        drawModuleList(context, mouseX, mouseY);
        drawSettings(context, mouseX, mouseY);

        draw(context, "VANTA  •  CLIENT SETTINGS", panelX + 14, panelY + panelHeight - 17, MUTED);
        draw(context, "Only chunks sent to your client can be scanned", panelX + panelWidth - 270,
                panelY + panelHeight - 17, MUTED);
    }

    private void layout() {
        panelWidth = Math.min(900, Math.max(620, width - 24));
        panelHeight = Math.min(540, Math.max(390, height - 24));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        railWidth = 158;
        listWidth = 218;
        listX = panelX + railWidth;
        settingsX = listX + listWidth + 1;
        settingsWidth = panelWidth - railWidth - listWidth - 1;
        contentY = panelY + 58;
    }

    private void drawCategoryRail(DrawContext context, int mouseX, int mouseY) {
        draw(context, "MODULES", panelX + 16, panelY + 61, MUTED);
        int y = panelY + 83;
        for (Category entry : Category.values()) {
            int rowY = y + entry.ordinal() * 39;
            boolean selected = category == entry;
            boolean hover = inside(mouseX, mouseY, panelX + 10, rowY, railWidth - 20, 32);
            if (selected || hover) {
                context.fill(panelX + 10, rowY, panelX + railWidth - 10, rowY + 32,
                        selected ? 0xFF202832 : PANEL_HOVER);
            }
            if (selected) context.fill(panelX + 10, rowY + 4, panelX + 12, rowY + 28, ACCENT);
            draw(context, entry.label, panelX + 22, rowY + 11, selected ? TEXT : MUTED);
            draw(context, Integer.toString(entry.modules.size()), panelX + railWidth - 29, rowY + 11, MUTED);
        }

        int brandY = panelY + panelHeight - 77;
        context.fill(panelX + 13, brandY, panelX + railWidth - 13, brandY + 1, BORDER);
        draw(context, "PROFILE", panelX + 16, brandY + 12, MUTED);
        draw(context, "Default", panelX + 16, brandY + 28, TEXT);
        draw(context, "Right Shift to reopen", panelX + 16, brandY + 48, MUTED);
    }

    private void drawModuleList(DrawContext context, int mouseX, int mouseY) {
        context.fill(listX, panelY + 47, listX + listWidth, panelY + 48, BORDER);
        draw(context, category.label.toUpperCase(), listX + 14, panelY + 61, TEXT);
        draw(context, "Click name to toggle  ·  ⋮ settings", listX + 14, panelY + 77, MUTED);

        int y = panelY + 103;
        int rows = Math.max(1, (panelHeight - 152) / 38);
        for (int i = moduleScroll; i < Math.min(category.modules.size(), moduleScroll + rows); i++) {
            Module module = category.modules.get(i);
            int rowY = y + (i - moduleScroll) * 38;
            boolean enabled = moduleEnabled(module);
            boolean selected = selectedModule == module;
            boolean hover = inside(mouseX, mouseY, listX + 8, rowY, listWidth - 16, 32);
            if (selected || hover) context.fill(listX + 8, rowY, listX + listWidth - 8, rowY + 32,
                    selected ? 0xFF202832 : PANEL_HOVER);
            if (enabled) context.fill(listX + 8, rowY + 4, listX + 10, rowY + 28, ACCENT);
            draw(context, module.label, listX + 18, rowY + 11, enabled ? ACCENT : TEXT);
            draw(context, "⋮", listX + listWidth - 31, rowY + 8, selected ? ACCENT : MUTED);
            if (module == selectedModule) {
                draw(context, moduleSummary(module), settingsX + 17, panelY + 58, MUTED);
            }
        }
    }

    private void drawSettings(DrawContext context, int mouseX, int mouseY) {
        int x = settingsX + 16;
        int w = settingsWidth - 32;
        draw(context, selectedModule.label, x, contentY, TEXT);
        draw(context, moduleDescription(selectedModule), x, contentY + 17, MUTED);
        switch (selectedModule) {
            case STORAGE_ESP -> drawStorageSettings(context, mouseX, mouseY, x, w);
            case SUSPICIOUS_ESP -> drawSuspiciousSettings(context, x, w);
            case SUS_CHUNK_FINDER -> drawSusChunkSettings(context, mouseX, mouseY, x, w);
            case DONUT_CHUNK_FINDER -> drawChunkFinderSettings(context, x, w);
            case RELOG_REMINDER -> drawRelogSettings(context, mouseX, mouseY, x, w);
            default -> drawCatalogSettings(context, x, w);
        }
    }

    private void drawStorageSettings(DrawContext c, int mx, int my, int x, int w) {
        drawToggle(c, x, contentY + 40, w, "Storage ESP", "Show selected storage blocks", config.enabled, mx, my);
        draw(c, "DISPLAY STYLE", x, contentY + 101, MUTED);
        drawSegments(c, x, contentY + 120, w, new String[]{"Outline", "Lines", "Both"},
                config.renderMode.ordinal(), "render-mode", mx, my);
        drawSlider(c, x, contentY + 164, w, "Range", config.range, "" + config.range + " blocks",
                (config.range - 8) / 120.0, "range", mx, my);
        draw(c, "STORAGE TYPES", x, contentY + 226, MUTED);
        drawStorageChips(c, x, contentY + 247, w, mx, my);
        draw(c, "Range is measured from your player. Storage ESP reads loaded block entities at all Y levels.",
                x, contentY + 293, MUTED);
    }

    private void drawSuspiciousSettings(DrawContext c, int x, int w) {
        drawToggle(c, x, contentY + 40, w, "Suspicious ESP", "Use a warning color in suspect storage chunks",
                config.suspiciousEsp, -1, -1);
        c.fill(x, contentY + 105, x + w, contentY + 171, PANEL);
        draw(c, "WARNING COLOR", x + 14, contentY + 118, MUTED);
        c.fill(x + 14, contentY + 143, x + 31, contentY + 160, ALERT);
        draw(c, "Warm red marks chunks that meet the storage threshold.", x + 41, contentY + 147, TEXT);
        draw(c, "This is a visual overlay; it does not inspect container contents.", x, contentY + 194, MUTED);
    }

    private void drawSusChunkSettings(DrawContext c, int mx, int my, int x, int w) {
        drawToggle(c, x, contentY + 40, w, "Sus Chunk Finder", "Flag loaded chunks by storage count",
                config.susChunkFinder, mx, my);
        drawSlider(c, x, contentY + 100, w, "Storage threshold", config.containersPerChunk,
                config.containersPerChunk + " containers / chunk", (config.containersPerChunk - 2) / 62.0,
                "threshold", mx, my);
        drawToggle(c, x, contentY + 158, w, "Notifications", "Alert when a chunk first meets the threshold",
                config.chunkAlerts, mx, my);
        drawInput(c, x, contentY + 202, w, "ALERT MESSAGE", config.alertMessage, "alert", mx, my);
        draw(c, "Placeholders: {x} chunk X, {z} chunk Z, {count} storage blocks", x, contentY + 270, MUTED);
        draw(c, "Scans loaded chunks through the full world height, including bedrock depth.", x, contentY + 289, MUTED);
    }

    private void drawChunkFinderSettings(DrawContext c, int x, int w) {
        drawToggle(c, x, contentY + 40, w, "Chunk Finder", "Notify when a new nearby chunk loads",
                config.chunkFinder, -1, -1);
        drawInput(c, x, contentY + 100, w, "CHUNK MESSAGE", config.chunkFinderMessage, "chunk", -1, -1);
        draw(c, "Tracks loaded chunk boundaries as you move. First scan after joining is the baseline.",
                x, contentY + 169, MUTED);
        draw(c, "Only detects chunks the server has sent to your client.", x, contentY + 189, MUTED);
    }

    private void drawRelogSettings(DrawContext c, int mx, int my, int x, int w) {
        drawToggle(c, x, contentY + 40, w, "Empty Chunk Relog Prompt", "Check the current chunk after you reach Y -20",
                config.relogReminder, mx, my);
        drawInput(c, x, contentY + 108, w, "FIRST PROMPT", config.relogMessage, "relog", mx, my);
        draw(c, "If nothing is detected, Vanta asks you to relog once.", x, contentY + 177, MUTED);
        draw(c, "After reconnect, the same empty chunk reports: Nothing found.", x, contentY + 197, MUTED);
        draw(c, "The empty result is reported once; this search will not prompt again.", x, contentY + 217, MUTED);
        draw(c, "Only loaded storage block entities are counted; container contents are not read.",
                x, contentY + 248, MUTED);
    }

    private void drawCatalogSettings(DrawContext c, int x, int w) {
        String runtimeDescription = runtimeModuleDescription(selectedModule);
        drawToggle(c, x, contentY + 40, w, selectedModule.label,
                runtimeDescription == null ? "Catalogue switch saved in your local Vanta profile" : runtimeDescription,
                moduleEnabled(selectedModule), -1, -1);
        c.fill(x, contentY + 105, x + w, contentY + 180, PANEL);
        if (selectedModule == Module.PROXY) {
            draw(c, "UNAVAILABLE IN THIS BUILD", x + 14, contentY + 119, ALERT);
            draw(c, "Proxy routing is excluded because ban bypass was requested for this client.",
                    x + 14, contentY + 141, TEXT);
            draw(c, "No proxy settings are saved or applied.", x + 14, contentY + 158, MUTED);
        } else if (runtimeDescription != null) {
            draw(c, "VANTA MODULE", x + 14, contentY + 119, MUTED);
            draw(c, "Runs in your local client while enabled.", x + 14, contentY + 141, TEXT);
            draw(c, "Uses normal client controls and loaded game state.", x + 14, contentY + 158, MUTED);
        } else {
            draw(c, "MODULE PROFILE", x + 14, contentY + 119, MUTED);
            draw(c, "Catalogue entry only", x + 14, contentY + 141, TEXT);
            draw(c, "This switch saves a profile value; it does not run game behavior.", x + 14, contentY + 158, MUTED);
        }
        if (selectedModule == Module.AUTO_EAT) {
            drawSlider(c, x, contentY + 205, w, "Start eating at", config.autoEatThreshold,
                    config.autoEatThreshold + " hunger", (config.autoEatThreshold - 1) / 18.0,
                    "autoEat", -1, -1);
            draw(c, "Uses food from your hotbar and restores your selected slot afterward.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.AUTO_CLICKER) {
            drawSlider(c, x, contentY + 205, w, "Click rate", config.autoClickCps,
                    config.autoClickCps + " CPS", (config.autoClickCps - 1) / 19.0,
                    "autoClick", -1, -1);
            draw(c, "Only runs while you hold attack and respects the vanilla attack cooldown.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.LIGHT_FINDER) {
            drawSlider(c, x, contentY + 205, w, "Maximum light level", config.lightThreshold,
                    Integer.toString(config.lightThreshold), config.lightThreshold / 15.0,
                    "light", -1, -1);
            draw(c, "Marks dark air blocks in the nearby area, up to 512 markers.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.AUTO_LOG) {
            drawSlider(c, x, contentY + 205, w, "Disconnect at health", config.autoLogHealth,
                    config.autoLogHealth + " HP", (config.autoLogHealth - 1) / 19.0,
                    "autoLog", -1, -1);
            draw(c, "Disconnects once when your health reaches the threshold.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.AUTO_TOTEM || selectedModule == Module.HOVER_TOTEM) {
            drawSlider(c, x, contentY + 205, w, "Equip at health", config.totemHealthThreshold,
                    config.totemHealthThreshold + " HP", (config.totemHealthThreshold - 1) / 19.0,
                    "totem", -1, -1);
            draw(c, "Uses the vanilla inventory swap action to equip an available totem.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.CHAT_MACRO) {
            drawInput(c, x, contentY + 205, w, "MESSAGE  ·  PREFIX / FOR A COMMAND",
                    config.chatMacroMessage, "macro", -1, -1);
            draw(c, "Press J by default. Change the key in Minecraft Controls.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.FRIENDS) {
            drawInput(c, x, contentY + 205, w, "FRIEND NAMES  ·  COMMA OR SPACE SEPARATED",
                    config.friendNames, "friends", -1, -1);
            draw(c, "Friend colors only appear in Vanta overlays while enabled.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.AUTO_SELL) {
            drawInput(c, x, contentY + 205, w, "SELL COMMAND", config.autoSellCommand, "sell", -1, -1);
            draw(c, "Press K to send this command to the current server.", x, contentY + 270, MUTED);
        } else if (selectedModule == Module.AUTO_SPAWNER_SELL) {
            drawInput(c, x, contentY + 205, w, "SPAWNER SELL COMMAND", config.autoSpawnerSellCommand,
                    "spawnerSell", -1, -1);
            draw(c, "Press B to send this command to the current server.", x, contentY + 270, MUTED);
        } else if (selectedModule == Module.RTP_BASE_FINDER) {
            drawInput(c, x, contentY + 205, w, "RTP COMMAND", config.rtpCommand, "rtp", -1, -1);
            draw(c, "Press R to run this command and scan new loaded chunks for storage.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.SEED_CHUNK_FINDER) {
            drawInput(c, x, contentY + 205, w, "WORLD SEED  ·  INTEGER", config.seedFinderSeed, "seed", -1, -1);
            draw(c, "Marks slime chunks only among terrain already loaded by your client.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.FAKE_PAY) {
            drawInput(c, x, contentY + 205, w, "LOCAL PREVIEW TEXT", config.fakePayPreviewText, "fakePay", -1, -1);
            draw(c, "Press F7 to show this text to yourself. Nothing is sent to the server.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.FAKE_STATS) {
            drawInput(c, x, contentY + 205, w, "HUD PREVIEW TEXT", config.fakeStatsText, "fakeStats", -1, -1);
            draw(c, "This is a local HUD preview and does not change server statistics.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.RADIO) {
            drawInput(c, x, contentY + 205, w, "DIRECT PCM WAV / AIFF STREAM URL", config.radioUrl, "radio", -1, -1);
            draw(c, "Press F8 to start or stop. Audio streams only; it is not sent to game chat.",
                    x, contentY + 270, MUTED);
        } else if (selectedModule == Module.DISCORD_PRESENCE) {
            drawInput(c, x, contentY + 190, w, "DISCORD APPLICATION ID", config.discordApplicationId,
                    "discordId", -1, -1);
            drawInput(c, x, contentY + 248, w, "PRESENCE STATUS", config.discordStatus,
                    "discordStatus", -1, -1);
            draw(c, "Press F6. Discord desktop must be running on Windows.", x, contentY + 298, MUTED);
        }
    }

    private String runtimeModuleDescription(Module module) {
        return switch (module) {
            case FULLBRIGHT -> "Raise client brightness while enabled; restore your prior setting when off.";
            case SPRINT -> "Request sprint while enabled; normal Minecraft movement limits still apply.";
            case HUD -> "Show Vanta, coordinates, and FPS in the corner of the screen.";
            case PLAYER_ESP -> "Draw visible client-side boxes around other loaded players.";
            case MOB_ESP -> "Draw visible client-side boxes around nearby loaded mobs.";
            case AUTO_EAT -> "Use the best food in your hotbar when hunger reaches your threshold.";
            case AUTO_TOOL -> "Choose the fastest hotbar tool while mining, then restore your prior slot.";
            case WEATHER_NOTIFIER -> "Notify when local weather starts, changes, or clears.";
            case BLOCK_ENTITY_DEBUG -> "Show loaded storage and flagged-chunk counts on the HUD.";
            case AUTO_CLICKER -> "Repeat your held attack input at a capped rate using vanilla cooldown.";
            case TRIGGER_BOT -> "Attack a living entity under your crosshair at vanilla cooldown speed.";
            case AUTO_WALK -> "Hold forward at normal movement speed while enabled.";
            case PLAYER_CHUNKS -> "Outline the current chunk footprint of nearby loaded players.";
            case SPAWNER_PROTECT -> "Outline loaded spawners and alert once when each is first found.";
            case TARGET_HUD -> "Show the crosshair target's name and health bar.";
            case NETHERITE_FINDER -> "Outline ancient debris in loaded chunks between Y 8 and 23.";
            case HOLE_ESP -> "Outline nearby two-block-high cavities with solid floors and walls.";
            case LIGHT_FINDER -> "Mark nearby air blocks at or below a configurable light level.";
            case BLOCK_ESP -> "Outline nearby loaded diamond ore, emerald ore, and ancient debris.";
            case AUTO_MINE -> "Hold normal attack while your crosshair points at a block.";
            case AUTO_JUMP_RESET -> "Jump when hurt while grounded, using normal Minecraft movement.";
            case BLOCK_NOTIFIER -> "Notify when the block you are watching changes state.";
            case JUMP_CIRCLES -> "Draw a fading ring when you land after a jump or fall.";
            case STATIC_HITBOXES -> "Draw fixed-size outlines around nearby loaded players.";
            case REAL_HITBOX -> "Draw current collision boxes around nearby loaded living entities.";
            case AUTO_LOG -> "Disconnect from the current server at a configurable health threshold.";
            case AUTO_FIREWORK -> "Use a hotbar firework rocket every two seconds while elytra-flying.";
            case ELYTRA_GLIDE -> "Request a normal jump input when falling with an equipped elytra.";
            case CORD_SNAPPER -> "Copy your block coordinates and chunk coordinates with C (default).";
            case KEY_PEARL -> "Use a hotbar Ender Pearl with P (default); binds are configurable in Controls.";
            case KEY_WIND_CHARGE -> "Use a hotbar Wind Charge with H (default); binds are configurable in Controls.";
            case PEARL_TRAJECTORY -> "Draw a client-side estimate of the held Ender Pearl's path.";
            case AUTO_TOTEM -> "Move an available totem to offhand at the configured health threshold.";
            case AUTO_INV_TOTEM -> "Search inventory for a totem when health is critical.";
            case HOVER_TOTEM -> "Equip an offhand totem when falling at low health.";
            case TOTEM_OFFHAND -> "Keep a totem in offhand whenever one is available.";
            case AUTO_LOOT -> "Move items out of open vanilla container screens into your inventory.";
            case AUTO_RECONNECT -> "Retry the selected multiplayer server up to three times after a disconnect.";
            case AUTO_TPA -> "Accept matching incoming teleport requests with the server's /tpaccept command.";
            case FAST_PLACE -> "Hold normal use while targeting a block with a placeable block in hand.";
            case CHAT_MACRO -> "Send your configured message with J (default); binds are configurable in Controls.";
            case FRIENDS -> "Mark names from your list green in Player ESP, Player Chunks, and Target HUD.";
            case AUTO_SELL -> "Run your configured sell command with K (default).";
            case AUTO_SPAWNER_SELL -> "Run your configured spawner-sell command with B (default).";
            case RTP_BASE_FINDER -> "Run the configured RTP command with R, then scan arriving chunks for storage.";
            case ORE_SIM -> "Outline common ores in blocks already loaded by your client.";
            case ANTI_TRAP -> "Request a normal jump when the player is inside a solid block.";
            case ITEM_DROPPER -> "Drop the selected stack when you press N (default).";
            case AIM_ASSIST -> "Gently guide your view toward a nearby visible player while you hold attack.";
            case ANCHOR_MACRO -> "Press G to use the respawn anchor under your crosshair.";
            case AUTO_CRYSTAL -> "Use a held End Crystal on a targeted obsidian or bedrock base.";
            case AUTO_DOUBLE_HAND -> "Repeat offhand use while the normal use key is held.";
            case AUTO_HIT_CRYSTAL -> "Attack an End Crystal under your crosshair at vanilla cooldown speed.";
            case CRYSTAL_OPTIMIZER -> "Outline valid nearby End Crystal placement spots on loaded terrain.";
            case DOUBLE_ANCHOR -> "Repeat the normal interact action while holding use on a respawn anchor.";
            case ELYTRA_SWAP -> "Press X to swap a hotbar item with your chest slot.";
            case HITBOX -> "Highlight the living entity currently under your crosshair.";
            case MACE_BOMBER -> "Attack a living target while falling with a mace equipped.";
            case MACE_SWAP -> "Select a hotbar mace while falling toward a living target.";
            case SHIELD_BREAKER -> "Select a hotbar axe while attacking a player who is blocking.";
            case SPEAR_SWAP -> "Select a hotbar trident while submerged and targeting a living entity.";
            case FAKEPLAYER -> "Create a client-only player stand-in at your current position.";
            case FREECAM -> "Move a client-only camera with movement keys; your player stays in the world.";
            case FREE_LOOK -> "Use a local camera view tethered to your player position.";
            case NAME_TAGS -> "List nearby loaded players and distances in the HUD.";
            case MUSIC_HUD -> "Show the names of records in nearby loaded jukeboxes.";
            case SEED_CHUNK_FINDER -> "Use a configured world seed to outline loaded slime chunks.";
            case NO_HIT_DELAY -> "Remove the local attack cooldown check; the server still decides whether attacks deal damage.";
            case SWING_SPEED -> "Repeat the swing animation while attack is held; server damage timing is unchanged.";
            case NAME_PROTECT -> "Replace your account name in local chat and the tab list with Vanta.";
            case SKIN_PROTECT -> "Show the default Steve texture for your own player model on this client.";
            case FAKE_PAY -> "Press F7 to print your configured payment preview in local chat only.";
            case FAKE_STATS -> "Show a configurable statistics preview in your HUD; values are local text.";
            case PRIME_CHUNK_FINDER -> "Alert when a loaded chunk contains at least twice the storage threshold.";
            case TUNNEL_BASE_FINDER -> "Mark straight three-block tunnels in already loaded terrain.";
            case RADIO -> "Play a configured direct PCM WAV or AIFF stream locally with F8.";
            case DISCORD_PRESENCE -> "Send a local Rich Presence status to the running Discord desktop app with F6.";
            case PROXY -> "Unavailable in this build because the requested proxy module would enable ban bypass.";
            default -> null;
        };
    }

    private void drawToggle(DrawContext c, int x, int y, int w, String title, String subtitle, boolean enabled,
                            int mx, int my) {
        c.fill(x, y, x + w, y + 48, PANEL);
        draw(c, title, x + 12, y + 9, TEXT);
        draw(c, subtitle, x + 12, y + 26, MUTED);
        int sx = x + w - 53;
        int sy = y + 13;
        c.fill(sx, sy, sx + 39, sy + 22, enabled ? 0xFF226D67 : 0xFF353B47);
        c.fill(enabled ? sx + 21 : sx + 3, sy + 3, enabled ? sx + 36 : sx + 18, sy + 19,
                enabled ? ACCENT : 0xFFB7BEC9);
    }

    private void drawSegments(DrawContext c, int x, int y, int w, String[] labels, int active,
                              String id, int mx, int my) {
        int gap = 5;
        int buttonW = (w - gap * (labels.length - 1)) / labels.length;
        for (int i = 0; i < labels.length; i++) {
            int bx = x + i * (buttonW + gap);
            boolean selected = active == i;
            boolean hover = inside(mx, my, bx, y, buttonW, 26);
            c.fill(bx, y, bx + buttonW, y + 26, selected ? 0xFF23514E : hover ? PANEL_HOVER : PANEL);
            if (selected) c.fill(bx, y + 24, bx + buttonW, y + 26, ACCENT);
            centered(c, labels[i], bx + buttonW / 2, y + 8, selected ? ACCENT : TEXT);
        }
    }

    private void drawSlider(DrawContext c, int x, int y, int w, String title, int value, String valueText,
                            double progress, String id, int mx, int my) {
        c.fill(x, y, x + w, y + 54, PANEL);
        draw(c, title, x + 12, y + 8, TEXT);
        draw(c, valueText, x + w - textRenderer.getWidth(valueText) - 12, y + 8, ACCENT);
        int left = x + 14;
        int right = x + w - 14;
        int trackY = y + 38;
        c.fill(left, trackY, right, trackY + 4, 0xFF3A404B);
        int handleX = left + (int) Math.round((right - left) * Math.max(0, Math.min(1, progress)));
        c.fill(left, trackY, handleX, trackY + 4, ACCENT);
        c.fill(handleX - 4, trackY - 4, handleX + 5, trackY + 9, 0xFFFFFFFF);
    }

    private void drawStorageChips(DrawContext c, int x, int y, int w, int mx, int my) {
        String[] names = {"Chest", "Trapped", "Ender", "Barrel", "Shulker"};
        boolean[] values = {config.chests, config.trappedChests, config.enderChests, config.barrels, config.shulkerBoxes};
        int gap = 5;
        int chipW = (w - gap * 4) / 5;
        for (int i = 0; i < names.length; i++) {
            int bx = x + i * (chipW + gap);
            boolean hover = inside(mx, my, bx, y, chipW, 28);
            c.fill(bx, y, bx + chipW, y + 28, values[i] ? 0xFF23514E : hover ? PANEL_HOVER : PANEL);
            centered(c, names[i], bx + chipW / 2, y + 9, values[i] ? ACCENT : MUTED);
        }
    }

    private void drawInput(DrawContext c, int x, int y, int w, String label, String value, String id,
                           int mx, int my) {
        draw(c, label, x, y, MUTED);
        int fy = y + 17;
        boolean focused = focusedField.equals(id);
        boolean hover = inside(mx, my, x, fy, w, 29);
        c.fill(x, fy, x + w, fy + 29, focused ? 0xFF202A34 : hover ? PANEL_HOVER : PANEL);
        c.fill(x, fy, x + w, fy + 1, focused ? ACCENT : BORDER);
        String display = value == null ? "" : value;
        int maxWidth = w - 22;
        while (!display.isEmpty() && textRenderer.getWidth(display) > maxWidth) {
            display = display.substring(1);
        }
        draw(c, display, x + 9, fy + 10, TEXT);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursor = x + 9 + textRenderer.getWidth(display);
            c.fill(cursor, fy + 8, cursor + 1, fy + 21, ACCENT);
        }
    }

    private void drawText(DrawContext c, String value, int x, int y, int color) {
        draw(c, value, x, y, color);
    }

    private void draw(DrawContext c, String value, int x, int y, int color) {
        c.drawText(textRenderer, Text.literal(value), x, y, color, false);
    }

    private void centered(DrawContext c, String value, int centerX, int y, int color) {
        c.drawText(textRenderer, Text.literal(value), centerX - textRenderer.getWidth(value) / 2, y, color, false);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String moduleSummary(Module module) {
        return switch (module) {
            case STORAGE_ESP -> "Block highlights and range controls";
            case SUSPICIOUS_ESP -> "Highlights storage in flagged chunks";
            case SUS_CHUNK_FINDER -> "Storage-count threshold scanner";
            case DONUT_CHUNK_FINDER -> "Alerts on newly loaded chunks";
            case RELOG_REMINDER -> "One relog prompt, followed by a single empty result";
            case AUTO_EAT -> "Hotbar food at a configurable hunger level";
            case AUTO_TOOL -> "Select a faster mining tool from the hotbar";
            case WEATHER_NOTIFIER -> "Weather transition notifications";
            case BLOCK_ENTITY_DEBUG -> "Loaded block-entity scan counters";
            case AUTO_CLICKER -> "Repeat held attack input with configurable CPS";
            case TRIGGER_BOT -> "Attack a living crosshair target";
            case AUTO_WALK -> "Walk forward using the normal movement key";
            case PLAYER_CHUNKS -> "Outline chunks containing loaded players";
            case SPAWNER_PROTECT -> "Highlight and report loaded spawners";
            case TARGET_HUD -> "Target name and health display";
            case NETHERITE_FINDER -> "Loaded ancient-debris scan";
            case HOLE_ESP -> "Nearby enclosed two-block cavities";
            case LIGHT_FINDER -> "Nearby dark air blocks";
            case BLOCK_ESP -> "Loaded rare-ore outlines";
            case AUTO_MINE -> "Hold attack on the targeted block";
            case AUTO_JUMP_RESET -> "Jump on grounded hurt frames";
            case BLOCK_NOTIFIER -> "Watch the targeted block for state changes";
            case JUMP_CIRCLES -> "Landing ring visual";
            case STATIC_HITBOXES -> "Fixed-size player outlines";
            case REAL_HITBOX -> "Living entity collision outlines";
            case AUTO_LOG -> "Disconnect at a health threshold";
            case AUTO_FIREWORK -> "Firework boost while elytra-flying";
            case ELYTRA_GLIDE -> "Start gliding with normal jump input";
            case CORD_SNAPPER -> "Copy position and chunk coordinates";
            case KEY_PEARL -> "Use a hotbar pearl on a key press";
            case KEY_WIND_CHARGE -> "Use a hotbar wind charge on a key press";
            case PEARL_TRAJECTORY -> "Estimate the held pearl flight path";
            case AUTO_TOTEM -> "Equip totem below health threshold";
            case AUTO_INV_TOTEM -> "Search inventory for a totem";
            case HOVER_TOTEM -> "Equip totem during a dangerous fall";
            case TOTEM_OFFHAND -> "Keep a totem in offhand";
            case AUTO_LOOT -> "Quick-move items from an open container";
            case AUTO_RECONNECT -> "Retry a disconnected multiplayer server";
            case AUTO_TPA -> "Respond to recognized teleport requests";
            case FAST_PLACE -> "Hold use on a placeable block target";
            case CHAT_MACRO -> "Send a saved chat message on a key";
            case FRIENDS -> "Color saved friend names green";
            case AUTO_SELL -> "Run a server sell command on key press";
            case AUTO_SPAWNER_SELL -> "Run a server spawner-sell command on key press";
            case RTP_BASE_FINDER -> "Run RTP and scan new chunks for storage";
            case ORE_SIM -> "Mark ores in nearby loaded terrain";
            case ANTI_TRAP -> "Jump when inside a solid block";
            case ITEM_DROPPER -> "Drop selected stack with a key";
            case AIM_ASSIST -> "View guidance toward a nearby target";
            case ANCHOR_MACRO -> "Interact with a targeted respawn anchor";
            case AUTO_CRYSTAL -> "Place a held crystal on a valid target";
            case AUTO_DOUBLE_HAND -> "Repeat offhand use input";
            case AUTO_HIT_CRYSTAL -> "Attack a targeted crystal";
            case CRYSTAL_OPTIMIZER -> "Valid loaded placement spots";
            case DOUBLE_ANCHOR -> "Repeat a held anchor interaction";
            case ELYTRA_SWAP -> "Chest equipment swap key";
            case HITBOX -> "Crosshair target collision outline";
            case MACE_BOMBER -> "Fall-window mace attack";
            case MACE_SWAP -> "Select mace during a fall";
            case SHIELD_BREAKER -> "Select axe against a blocking player";
            case SPEAR_SWAP -> "Select trident underwater";
            case FAKEPLAYER -> "Client-side player stand-in";
            case FREECAM -> "Client-only movable camera";
            case FREE_LOOK -> "Local camera tethered to player";
            case NAME_TAGS -> "Nearby player names and distances";
            case MUSIC_HUD -> "Nearby jukebox record labels";
            case SEED_CHUNK_FINDER -> "Loaded slime-chunk overlay from entered seed";
            case NO_HIT_DELAY -> "Local attack timer option";
            case SWING_SPEED -> "Repeat held swing animation";
            case NAME_PROTECT -> "Mask your name in chat and tab list";
            case SKIN_PROTECT -> "Replace your own skin locally";
            case FAKE_PAY -> "Local payment text preview";
            case FAKE_STATS -> "Local stats HUD preview";
            case PRIME_CHUNK_FINDER -> "High storage count alert";
            case TUNNEL_BASE_FINDER -> "Loaded terrain tunnel markers";
            case RADIO -> "Local audio stream control";
            case DISCORD_PRESENCE -> "Discord desktop Rich Presence";
            case PROXY -> "Unavailable for this project scope";
            default -> "Module configuration";
        };
    }

    private String moduleDescription(Module module) {
        return switch (module) {
            case STORAGE_ESP -> "Choose targets, style, and render range.";
            case SUSPICIOUS_ESP -> "Mark storage blocks in chunks above your threshold.";
            case SUS_CHUNK_FINDER -> "Count loaded storage block entities across full world height.";
            case DONUT_CHUNK_FINDER -> "Track when new nearby chunks arrive from the server.";
            case RELOG_REMINDER -> "At Y -20, check the current loaded chunk and guide a single relog retry.";
            case AUTO_EAT -> "Eat hotbar food at the configured hunger threshold.";
            case AUTO_TOOL -> "Select the quickest hotbar tool while the attack key is held on a block.";
            case WEATHER_NOTIFIER -> "Report weather transitions in the current world.";
            case BLOCK_ENTITY_DEBUG -> "Display the latest loaded-storage scan counts.";
            case AUTO_CLICKER -> "Repeat your held attack input without changing Minecraft's cooldown.";
            case TRIGGER_BOT -> "Attack the living entity under your crosshair at vanilla cooldown speed.";
            case AUTO_WALK -> "Hold forward while enabled; movement uses the normal client input.";
            case PLAYER_CHUNKS -> "Outline the chunk footprints of nearby players already loaded by the client.";
            case SPAWNER_PROTECT -> "Highlight nearby loaded mob spawners and report newly found positions once.";
            case TARGET_HUD -> "Show the name and health of the living entity under your crosshair.";
            case NETHERITE_FINDER -> "Scan loaded chunk blocks from Y 8 through 23 for ancient debris.";
            case HOLE_ESP -> "Find enclosed, two-block-high holes within 20 blocks of your position.";
            case LIGHT_FINDER -> "Mark low-light air blocks around your current height.";
            case BLOCK_ESP -> "Scan loaded blocks within 20 horizontal blocks and 16 vertical blocks.";
            case AUTO_MINE -> "Mine the block under your crosshair at normal client mining speed.";
            case AUTO_JUMP_RESET -> "Request a normal jump after taking damage while grounded.";
            case BLOCK_NOTIFIER -> "Track the block under your crosshair and alert if its state changes.";
            case JUMP_CIRCLES -> "Show a short fading ring after you land.";
            case STATIC_HITBOXES -> "Show a 0.6 by 1.8 block outline around nearby loaded players.";
            case REAL_HITBOX -> "Show the current collision bounds of nearby loaded living entities.";
            case AUTO_LOG -> "Leave the current server once when your health reaches the threshold.";
            case AUTO_FIREWORK -> "Use a firework from the hotbar at most once every 40 ticks while flying.";
            case ELYTRA_GLIDE -> "Request the normal jump action while falling with an elytra equipped.";
            case CORD_SNAPPER -> "Press C to copy your block position and chunk coordinates to the clipboard.";
            case KEY_PEARL -> "Press P to select and use the first hotbar Ender Pearl, then restore your slot.";
            case KEY_WIND_CHARGE -> "Press H to select and use the first hotbar Wind Charge, then restore your slot.";
            case PEARL_TRAJECTORY -> "Render an estimated projectile arc when a pearl is held; actual server physics may vary.";
            case AUTO_TOTEM -> "Swap an available totem into offhand when health reaches the chosen threshold.";
            case AUTO_INV_TOTEM -> "Search the full player inventory and equip a totem below 6 health.";
            case HOVER_TOTEM -> "Equip a totem while falling below the chosen health threshold.";
            case TOTEM_OFFHAND -> "Keep an available totem in offhand while enabled.";
            case AUTO_LOOT -> "Quick-move each item from a vanilla container screen into your inventory.";
            case AUTO_RECONNECT -> "Retry a multiplayer disconnect after a delay, up to three attempts; disabled in singleplayer.";
            case AUTO_TPA -> "Send /tpaccept when a chat message matches common English teleport-request wording.";
            case FAST_PLACE -> "Hold the normal use input while a block is targeted and a placeable block is selected.";
            case CHAT_MACRO -> "Press J to send the configured text; a leading slash sends a command.";
            case FRIENDS -> "Enter comma-separated names. Friend colors appear in Vanta player overlays.";
            case AUTO_SELL -> "Press K to send the configured command. Syntax varies by server.";
            case AUTO_SPAWNER_SELL -> "Press B to send the configured command. Syntax varies by server.";
            case RTP_BASE_FINDER -> "Press R to send the configured RTP command, then scan arriving chunks for storage.";
            case ORE_SIM -> "Highlight common and rare ores in nearby chunks already loaded by the client.";
            case ANTI_TRAP -> "Request a jump input while Minecraft reports the player inside a wall.";
            case ITEM_DROPPER -> "Press N to drop the entire selected stack with the normal game action.";
            case AIM_ASSIST -> "While attack is held, gradually adjusts your local view toward visible players within 4.5 blocks and 24 degrees.";
            case ANCHOR_MACRO -> "Press G while looking at a respawn anchor to interact with it using glowstone in offhand when available.";
            case AUTO_CRYSTAL -> "With an End Crystal selected, automatically uses it when your crosshair targets obsidian or bedrock.";
            case AUTO_DOUBLE_HAND -> "While use is held and offhand is non-empty, invokes offhand interaction every six ticks.";
            case AUTO_HIT_CRYSTAL -> "Attacks only an End Crystal under your crosshair, using Minecraft's attack cooldown.";
            case CRYSTAL_OPTIMIZER -> "Outlines air spaces above loaded obsidian or bedrock with two air blocks; it does not place crystals.";
            case DOUBLE_ANCHOR -> "Repeats the normal main-hand interaction every eight ticks while use is held on a respawn anchor.";
            case ELYTRA_SWAP -> "Press X to swap the chest slot with an elytra or another non-empty hotbar item.";
            case HITBOX -> "Draws an outline around the living entity under your crosshair; it does not change reach or collision.";
            case MACE_BOMBER -> "When falling toward a living target, attacks with a mace at vanilla cooldown speed.";
            case MACE_SWAP -> "Selects a hotbar mace when you are falling toward a living target and restores your slot afterward.";
            case SHIELD_BREAKER -> "Selects a hotbar axe while you hold attack on a blocking player, then restores your slot.";
            case SPEAR_SWAP -> "Selects a hotbar trident while submerged and aiming at a living target, then restores your slot.";
            case FAKEPLAYER -> "Adds a local-only stand-in named VantaDummy at the position where the toggle was enabled.";
            case FREECAM -> "Moves a local-only camera entity with movement keys. It does not move your server-side player.";
            case FREE_LOOK -> "Uses a local camera entity at your position and routes mouse-look to that camera.";
            case NAME_TAGS -> "Lists nearby loaded player names and distances in the HUD; this does not change world nameplates.";
            case MUSIC_HUD -> "Lists up to three records found in nearby loaded jukebox block entities.";
            case SEED_CHUNK_FINDER -> "Enter the numeric world seed to outline slime chunks among terrain already loaded by your client.";
            case NO_HIT_DELAY -> "Removes the local cooldown gate for attack input. A multiplayer server still validates attack timing and damage.";
            case SWING_SPEED -> "Plays a local swing animation every three ticks while attack is held. It does not alter server-side damage timing.";
            case NAME_PROTECT -> "Masks your account name as Vanta in client chat and the tab list; it does not change the name sent to a server.";
            case SKIN_PROTECT -> "Replaces your own skin texture with the default Steve texture in this client only.";
            case FAKE_PAY -> "Press F7 to display the configured preview message in your own chat. No command or payment packet is sent.";
            case FAKE_STATS -> "Displays configured text prefixed as a local stats preview. It does not change server data.";
            case PRIME_CHUNK_FINDER -> "Alerts once when an already loaded chunk reaches at least the larger of 16 storage blocks or twice your storage threshold.";
            case TUNNEL_BASE_FINDER -> "Marks three-wide, two-high solid-walled corridors in loaded chunks near your position.";
            case RADIO -> "Enter a direct PCM WAV or AIFF stream URL and press F8. The audio plays locally and never becomes a game message.";
            case DISCORD_PRESENCE -> "Enter a numeric app ID from Discord's developer portal and press F6. This build uses the local Windows Discord IPC pipe; Discord must be running.";
            case PROXY -> "This module stays unavailable because proxy configuration was requested alongside ban bypass. No proxy or ban-bypass behavior is included.";
            default -> "Configure this module.";
        };
    }

    private boolean moduleEnabled(Module module) {
        return switch (module) {
            case STORAGE_ESP -> config.enabled;
            case SUSPICIOUS_ESP -> config.suspiciousEsp;
            case SUS_CHUNK_FINDER -> config.susChunkFinder;
            case DONUT_CHUNK_FINDER -> config.chunkFinder;
            case RELOG_REMINDER -> config.relogReminder;
            case PROXY -> false;
            default -> catalogEnabled(module);
        };
    }

    private boolean catalogEnabled(Module module) {
        return config.moduleToggles.getOrDefault(module.name(), false);
    }

    private void toggleModule(Module module) {
        if (module == Module.PROXY) return;
        switch (module) {
            case STORAGE_ESP -> config.enabled = !config.enabled;
            case SUSPICIOUS_ESP -> config.suspiciousEsp = !config.suspiciousEsp;
            case SUS_CHUNK_FINDER -> config.susChunkFinder = !config.susChunkFinder;
            case DONUT_CHUNK_FINDER -> config.chunkFinder = !config.chunkFinder;
            case RELOG_REMINDER -> config.relogReminder = !config.relogReminder;
            default -> config.moduleToggles.put(module.name(), !catalogEnabled(module));
        }
        config.save();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        layout();
        if (!inside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            close();
            return true;
        }
        if (inside(mouseX, mouseY, panelX + panelWidth - 37, panelY + 3, 37, 40)) {
            close();
            return true;
        }

        int catY = panelY + 83;
        for (Category entry : Category.values()) {
            int rowY = catY + entry.ordinal() * 39;
            if (inside(mouseX, mouseY, panelX + 10, rowY, railWidth - 20, 32)) {
                category = entry;
                selectedModule = entry.modules.get(0);
                moduleScroll = 0;
                focusedField = "";
                return true;
            }
        }

        int listY = panelY + 103;
        int rows = Math.max(1, (panelHeight - 152) / 38);
        for (int i = moduleScroll; i < Math.min(category.modules.size(), moduleScroll + rows); i++) {
            Module module = category.modules.get(i);
            int rowY = listY + (i - moduleScroll) * 38;
            if (inside(mouseX, mouseY, listX + 8, rowY, listWidth - 16, 32)) {
                selectedModule = module;
                if (mouseX >= listX + listWidth - 43) {
                    focusedField = "";
                } else {
                    toggleModule(module);
                }
                return true;
            }
        }

        if (clickSettings(mouseX, mouseY)) return true;
        focusedField = "";
        return true;
    }

    private boolean clickSettings(double mx, double my) {
        int x = settingsX + 16;
        int w = settingsWidth - 32;
        int y = contentY;
        switch (selectedModule) {
            case STORAGE_ESP -> {
                if (inside(mx, my, x, y + 40, w, 48)) config.enabled = !config.enabled;
                else if (inside(mx, my, x, y + 120, w, 26)) {
                    int gap = 5, bw = (w - gap * 2) / 3;
                    int choice = Math.max(0, Math.min(2, (int) ((mx - x) / (bw + gap))));
                    config.renderMode = VantaConfig.RenderMode.values()[choice];
                } else if (inside(mx, my, x, y + 198, w, 17)) {
                    activeSlider = "range";
                    updateSlider(mx);
                } else if (inside(mx, my, x, y + 247, w, 28)) {
                    int gap = 5, cw = (w - gap * 4) / 5;
                    int choice = Math.max(0, Math.min(4, (int) ((mx - x) / (cw + gap))));
                    switch (choice) {
                        case 0 -> config.chests = !config.chests;
                        case 1 -> config.trappedChests = !config.trappedChests;
                        case 2 -> config.enderChests = !config.enderChests;
                        case 3 -> config.barrels = !config.barrels;
                        case 4 -> config.shulkerBoxes = !config.shulkerBoxes;
                    }
                }
            }
            case SUSPICIOUS_ESP -> {
                if (inside(mx, my, x, y + 40, w, 48)) config.suspiciousEsp = !config.suspiciousEsp;
            }
            case SUS_CHUNK_FINDER -> {
                if (inside(mx, my, x, y + 40, w, 48)) config.susChunkFinder = !config.susChunkFinder;
                else if (inside(mx, my, x, y + 133, w, 17)) {
                    activeSlider = "threshold";
                    updateSlider(mx);
                } else if (inside(mx, my, x, y + 158, w, 48)) config.chunkAlerts = !config.chunkAlerts;
                else if (inside(mx, my, x, y + 219, w, 29)) focusedField = "alert";
            }
            case DONUT_CHUNK_FINDER -> {
                if (inside(mx, my, x, y + 40, w, 48)) config.chunkFinder = !config.chunkFinder;
                else if (inside(mx, my, x, y + 117, w, 29)) focusedField = "chunk";
            }
            case RELOG_REMINDER -> {
                if (inside(mx, my, x, y + 40, w, 48)) config.relogReminder = !config.relogReminder;
                else if (inside(mx, my, x, y + 125, w, 29)) focusedField = "relog";
            }
            case AUTO_EAT -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 205, w, 54)) {
                    activeSlider = "autoEat";
                    updateSlider(mx);
                }
            }
            case AUTO_CLICKER -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 205, w, 54)) {
                    activeSlider = "autoClick";
                    updateSlider(mx);
                }
            }
            case LIGHT_FINDER -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 205, w, 54)) {
                    activeSlider = "light";
                    updateSlider(mx);
                }
            }
            case AUTO_LOG -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 205, w, 54)) {
                    activeSlider = "autoLog";
                    updateSlider(mx);
                }
            }
            case AUTO_TOTEM, HOVER_TOTEM -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 205, w, 54)) {
                    activeSlider = "totem";
                    updateSlider(mx);
                }
            }
            case CHAT_MACRO -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) {
                    focusedField = "macro";
                }
            }
            case FRIENDS -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) {
                    focusedField = "friends";
                }
            }
            case AUTO_SELL -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "sell";
            }
            case AUTO_SPAWNER_SELL -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "spawnerSell";
            }
            case RTP_BASE_FINDER -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "rtp";
            }
            case SEED_CHUNK_FINDER -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "seed";
            }
            case FAKE_PAY -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "fakePay";
            }
            case FAKE_STATS -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "fakeStats";
            }
            case RADIO -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 222, w, 29)) focusedField = "radio";
            }
            case DISCORD_PRESENCE -> {
                if (inside(mx, my, x, y + 40, w, 48)) {
                    config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
                } else if (inside(mx, my, x, y + 207, w, 29)) focusedField = "discordId";
                else if (inside(mx, my, x, y + 265, w, 29)) focusedField = "discordStatus";
            }
            case PROXY -> {
                // Intentionally unavailable in this project scope.
            }
            default -> config.moduleToggles.put(selectedModule.name(), !catalogEnabled(selectedModule));
        }
        config.sanitize();
        config.save();
        return true;
    }

    private void updateSlider(double mouseX) {
        int x = settingsX + 16;
        int w = settingsWidth - 32;
        double progress = Math.max(0, Math.min(1, (mouseX - (x + 14.0)) / (w - 28.0)));
        switch (activeSlider) {
            case "range" -> config.range = 8 + (int) Math.round(progress * 120);
            case "threshold" -> config.containersPerChunk = 2 + (int) Math.round(progress * 62);
            case "autoEat" -> config.autoEatThreshold = 1 + (int) Math.round(progress * 18);
            case "autoClick" -> config.autoClickCps = 1 + (int) Math.round(progress * 19);
            case "light" -> config.lightThreshold = (int) Math.round(progress * 15);
            case "autoLog" -> config.autoLogHealth = 1 + (int) Math.round(progress * 19);
            case "totem" -> config.totemHealthThreshold = 1 + (int) Math.round(progress * 19);
        }
        config.sanitize();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inside(mouseX, mouseY, listX, panelY + 90, listWidth, panelHeight - 120)) {
            int rows = Math.max(1, (panelHeight - 152) / 38);
            moduleScroll = Math.max(0, Math.min(Math.max(0, category.modules.size() - rows),
                    moduleScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && !activeSlider.isEmpty()) {
            updateSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!activeSlider.isEmpty()) {
            activeSlider = "";
            config.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (!focusedField.isEmpty()) {
            String value = fieldValue();
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                focusedField = "";
                config.save();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
                setFieldValue(value.substring(0, value.length() - 1));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                setFieldValue("");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focusedField.isEmpty() && !Character.isISOControl(chr)) {
            String value = fieldValue();
            if (value.length() < 160) setFieldValue(value + chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private String fieldValue() {
        return switch (focusedField) {
            case "alert" -> config.alertMessage;
            case "chunk" -> config.chunkFinderMessage;
            case "relog" -> config.relogMessage;
            case "macro" -> config.chatMacroMessage;
            case "friends" -> config.friendNames;
            case "sell" -> config.autoSellCommand;
            case "spawnerSell" -> config.autoSpawnerSellCommand;
            case "rtp" -> config.rtpCommand;
            case "seed" -> config.seedFinderSeed;
            case "fakePay" -> config.fakePayPreviewText;
            case "fakeStats" -> config.fakeStatsText;
            case "radio" -> config.radioUrl;
            case "discordId" -> config.discordApplicationId;
            case "discordStatus" -> config.discordStatus;
            default -> "";
        };
    }

    private void setFieldValue(String value) {
        switch (focusedField) {
            case "alert" -> config.alertMessage = value;
            case "chunk" -> config.chunkFinderMessage = value;
            case "relog" -> config.relogMessage = value;
            case "macro" -> config.chatMacroMessage = value;
            case "friends" -> config.friendNames = value;
            case "sell" -> config.autoSellCommand = value;
            case "spawnerSell" -> config.autoSpawnerSellCommand = value;
            case "rtp" -> config.rtpCommand = value;
            case "seed" -> config.seedFinderSeed = value;
            case "fakePay" -> config.fakePayPreviewText = value;
            case "fakeStats" -> config.fakeStatsText = value;
            case "radio" -> config.radioUrl = value;
            case "discordId" -> config.discordApplicationId = value;
            case "discordStatus" -> config.discordStatus = value;
        }
        config.sanitize();
        config.save();
    }

    @Override
    public void close() {
        focusedField = "";
        activeSlider = "";
        config.sanitize();
        config.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Category {
        COMBAT("Combat", List.of(Module.AIM_ASSIST, Module.ANCHOR_MACRO, Module.AUTO_CRYSTAL,
                Module.AUTO_DOUBLE_HAND, Module.AUTO_HIT_CRYSTAL, Module.AUTO_INV_TOTEM, Module.AUTO_JUMP_RESET,
                Module.AUTO_TOTEM, Module.CRYSTAL_OPTIMIZER, Module.DOUBLE_ANCHOR, Module.ELYTRA_SWAP,
                Module.HITBOX, Module.HOVER_TOTEM, Module.MACE_BOMBER, Module.MACE_SWAP, Module.NO_HIT_DELAY,
                Module.SHIELD_BREAKER, Module.SPEAR_SWAP, Module.STATIC_HITBOXES, Module.TOTEM_OFFHAND,
                Module.TRIGGER_BOT)),
        MISC("Misc", List.of(Module.AUTO_CLICKER, Module.AUTO_EAT, Module.AUTO_FIREWORK, Module.AUTO_LOG,
                Module.AUTO_LOOT, Module.AUTO_MINE, Module.AUTO_RECONNECT, Module.AUTO_TOOL, Module.AUTO_TPA,
                Module.AUTO_WALK, Module.CORD_SNAPPER, Module.ELYTRA_GLIDE, Module.FAKEPLAYER, Module.FAST_PLACE,
                Module.FREECAM, Module.KEY_PEARL, Module.KEY_WIND_CHARGE, Module.NAME_PROTECT,
                Module.SKIN_PROTECT, Module.SPRINT, Module.WEATHER_NOTIFIER)),
        DONUT("Donut", List.of(Module.ANTI_TRAP, Module.AUTO_SELL, Module.AUTO_SPAWNER_SELL,
                Module.DONUT_CHUNK_FINDER, Module.FAKE_PAY, Module.FAKE_STATS, Module.ITEM_DROPPER,
                Module.NETHERITE_FINDER, Module.PLAYER_CHUNKS, Module.SPAWNER_PROTECT)),
        BASE_FINDING("Base Finding", List.of(Module.BLOCK_ENTITY_DEBUG, Module.HOLE_ESP, Module.LIGHT_FINDER,
                Module.PRIME_CHUNK_FINDER, Module.RTP_BASE_FINDER, Module.SUS_CHUNK_FINDER,
                Module.SUSPICIOUS_ESP, Module.TUNNEL_BASE_FINDER, Module.SEED_CHUNK_FINDER)),
        RENDER("Render", List.of(Module.BLOCK_ESP, Module.BLOCK_NOTIFIER, Module.FREE_LOOK, Module.FULLBRIGHT,
                Module.HUD, Module.JUMP_CIRCLES, Module.MOB_ESP, Module.NAME_TAGS, Module.ORE_SIM,
                Module.PEARL_TRAJECTORY, Module.PLAYER_ESP, Module.REAL_HITBOX, Module.MUSIC_HUD,
                Module.STORAGE_ESP, Module.SWING_SPEED, Module.TARGET_HUD)),
        CLIENT("Client", List.of(Module.CHAT_MACRO, Module.DISCORD_PRESENCE, Module.FRIENDS, Module.PROXY,
                Module.RADIO, Module.RELOG_REMINDER));

        private final String label;
        private final List<Module> modules;

        Category(String label, List<Module> modules) {
            this.label = label;
            this.modules = modules;
        }
    }

    private enum Module {
        AIM_ASSIST("Aim Assist"),
        ANCHOR_MACRO("Anchor Macro"),
        AUTO_CRYSTAL("Auto Crystal"),
        AUTO_DOUBLE_HAND("Auto Double Hand"),
        AUTO_HIT_CRYSTAL("Auto Hit Crystal"),
        AUTO_INV_TOTEM("Auto Inv Totem"),
        AUTO_JUMP_RESET("Auto Jump Reset"),
        AUTO_TOTEM("Auto Totem"),
        CRYSTAL_OPTIMIZER("Crystal Optimizer"),
        DOUBLE_ANCHOR("Double Anchor"),
        ELYTRA_SWAP("Elytra Swap"),
        HITBOX("HitBox"),
        HOVER_TOTEM("Hover Totem"),
        MACE_BOMBER("Mace Bomber"),
        MACE_SWAP("Mace Swap"),
        NO_HIT_DELAY("No Hit Delay"),
        SHIELD_BREAKER("Shield Breaker"),
        SPEAR_SWAP("Spear Swap"),
        STATIC_HITBOXES("Static HitBoxes"),
        TOTEM_OFFHAND("Totem Offhand"),
        TRIGGER_BOT("Trigger Bot"),
        AUTO_CLICKER("Auto Clicker"),
        AUTO_EAT("Auto Eat"),
        AUTO_FIREWORK("Auto Firework"),
        AUTO_LOG("Auto Log"),
        AUTO_LOOT("Auto Loot"),
        AUTO_MINE("Auto Mine"),
        AUTO_RECONNECT("Auto Reconnect"),
        AUTO_TOOL("Auto Tool"),
        AUTO_TPA("Auto TPA"),
        AUTO_WALK("Auto Walk"),
        CORD_SNAPPER("Cord Snapper"),
        ELYTRA_GLIDE("Elytra Glide"),
        FAKEPLAYER("Fakeplayer"),
        FAST_PLACE("Fast Place"),
        FREECAM("Freecam"),
        KEY_PEARL("Key Pearl"),
        KEY_WIND_CHARGE("Key Wind Charge"),
        NAME_PROTECT("Name Protect"),
        SKIN_PROTECT("Skin Protect"),
        SPRINT("Sprint"),
        WEATHER_NOTIFIER("Weather Notifier"),
        ANTI_TRAP("Anti Trap"),
        AUTO_SELL("Auto Sell"),
        AUTO_SPAWNER_SELL("Auto Spawner Sell"),
        DONUT_CHUNK_FINDER("Chunk Finder"),
        FAKE_PAY("Fake Pay"),
        FAKE_STATS("Fake Stats"),
        ITEM_DROPPER("Item Dropper"),
        NETHERITE_FINDER("Netherite Finder"),
        PLAYER_CHUNKS("Player Chunks"),
        SPAWNER_PROTECT("Spawner Protect"),
        BLOCK_ENTITY_DEBUG("Block Entity Debug"),
        HOLE_ESP("Hole ESP"),
        LIGHT_FINDER("Light Finder"),
        PRIME_CHUNK_FINDER("Prime Chunk Finder"),
        RTP_BASE_FINDER("RTP Base Finder"),
        SEED_CHUNK_FINDER("Seed Chunk Finder"),
        TUNNEL_BASE_FINDER("Tunnel Base Finder"),
        BLOCK_ESP("Block ESP"),
        BLOCK_NOTIFIER("Block Notifier"),
        FREE_LOOK("Free Look"),
        FULLBRIGHT("Fullbright"),
        HUD("HUD"),
        JUMP_CIRCLES("Jump Circles"),
        MOB_ESP("Mob ESP"),
        NAME_TAGS("Name Tags"),
        ORE_SIM("Ore Sim"),
        PEARL_TRAJECTORY("Pearl Trajectory"),
        PLAYER_ESP("Player ESP"),
        REAL_HITBOX("RealHitBox"),
        MUSIC_HUD("Music HUD"),
        SWING_SPEED("SwingSpeed"),
        TARGET_HUD("Target HUD"),
        CHAT_MACRO("Chat Macro"),
        DISCORD_PRESENCE("Discord Presence"),
        FRIENDS("Friends"),
        PROXY("Proxy"),
        RADIO("Radio"),
        STORAGE_ESP("Storage ESP"),
        SUSPICIOUS_ESP("Suspicious ESP"),
        SUS_CHUNK_FINDER("Sus Chunk Finder"),
        RELOG_REMINDER("Empty Chunk Relog Prompt");

        private final String label;

        Module(String label) {
            this.label = label;
        }
    }
}


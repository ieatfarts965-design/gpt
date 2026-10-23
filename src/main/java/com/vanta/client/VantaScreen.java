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
    private int moduleScroll;
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
                catalogEnabled(selectedModule), -1, -1);
        c.fill(x, contentY + 105, x + w, contentY + 180, PANEL);
        if (runtimeDescription != null) {
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
            default -> catalogEnabled(module);
        };
    }

    private boolean catalogEnabled(Module module) {
        return config.moduleToggles.getOrDefault(module.name(), false);
    }

    private void toggleModule(Module module) {
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
            default -> "";
        };
    }

    private void setFieldValue(String value) {
        switch (focusedField) {
            case "alert" -> config.alertMessage = value;
            case "chunk" -> config.chunkFinderMessage = value;
            case "relog" -> config.relogMessage = value;
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


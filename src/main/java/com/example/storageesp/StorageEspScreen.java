package com.example.storageesp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class StorageEspScreen extends Screen {
    private final Screen parent;
    private final StorageEspConfig config;
    private TextFieldWidget alertField;
    private int panelX;
    private int panelY;

    public StorageEspScreen(Screen parent, StorageEspConfig config) {
        super(Text.literal("Storage ESP"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        clearChildren();
        panelX = (width - 560) / 2;
        panelY = (height - 360) / 2;
        int controlX = panelX + 220;
        int controlW = 300;
        int y = panelY + 48;

        addDrawableChild(ButtonWidget.builder(enabledLabel(), button -> {
            config.enabled = !config.enabled;
            button.setMessage(enabledLabel());
            config.save();
        }).dimensions(controlX, y, controlW, 20).build());
        y += 31;

        addDrawableChild(ButtonWidget.builder(modeLabel(), button -> {
            config.renderMode = switch (config.renderMode) {
                case OUTLINE -> StorageEspConfig.RenderMode.LINES;
                case LINES -> StorageEspConfig.RenderMode.BOTH;
                case BOTH -> StorageEspConfig.RenderMode.OUTLINE;
            };
            button.setMessage(modeLabel());
            config.save();
        }).dimensions(controlX, y, controlW, 20).build());
        y += 31;

        addDrawableChild(new SliderWidget(controlX, y, controlW, 20,
                Text.literal("Range: " + config.range + " blocks"), (config.range - 8) / 120.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Range: " + config.range + " blocks"));
            }

            @Override
            protected void applyValue() {
                config.range = 8 + (int) Math.round(value * 120.0);
                config.save();
            }
        });
        y += 31;

        addDrawableChild(new SliderWidget(controlX, y, controlW, 20,
                Text.literal("Sus chunk threshold: " + config.containersPerChunk),
                (config.containersPerChunk - 2) / 62.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Sus chunk threshold: " + config.containersPerChunk));
            }

            @Override
            protected void applyValue() {
                config.containersPerChunk = 2 + (int) Math.round(value * 62.0);
                config.save();
            }
        });
        y += 31;

        addDrawableChild(ButtonWidget.builder(alertsLabel(), button -> {
            config.chunkAlerts = !config.chunkAlerts;
            button.setMessage(alertsLabel());
            config.save();
        }).dimensions(controlX, y, controlW, 20).build());
        y += 31;

        alertField = new TextFieldWidget(textRenderer, controlX, y, controlW, 20,
                Text.literal("Alert message"));
        alertField.setMaxLength(160);
        alertField.setText(config.alertMessage);
        alertField.setChangedListener(value -> config.alertMessage = value);
        addDrawableChild(alertField);
        y += 31;

        addDrawableChild(ButtonWidget.builder(relogLabel(), button -> {
            config.relogReminder = !config.relogReminder;
            button.setMessage(relogLabel());
            config.save();
        }).dimensions(controlX, y, 146, 20).build());
        addDrawableChild(ButtonWidget.builder(intervalLabel(), button -> {
            config.relogMinutes = switch (config.relogMinutes) {
                case 15 -> 30;
                case 30 -> 60;
                case 60 -> 10;
                default -> 15;
            };
            button.setMessage(intervalLabel());
            config.save();
        }).dimensions(controlX + 154, y, 146, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(panelX + 452, panelY + 326, 80, 20).build());
    }

    private Text enabledLabel() {
        return Text.literal("Storage ESP: " + (config.enabled ? "ON" : "OFF"));
    }

    private Text modeLabel() {
        return Text.literal("Render style: " + config.renderMode.name());
    }

    private Text alertsLabel() {
        return Text.literal("Sus chunk alerts: " + (config.chunkAlerts ? "ON" : "OFF"));
    }

    private Text relogLabel() {
        return Text.literal("Relog reminder: " + (config.relogReminder ? "ON" : "OFF"));
    }

    private Text intervalLabel() {
        return Text.literal("Every " + config.relogMinutes + " min");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(panelX, panelY, panelX + 560, panelY + 360, 0xF0181B22);
        context.fill(panelX, panelY, panelX + 4, panelY + 360, 0xFF37C9E8);
        context.fill(panelX + 190, panelY + 1, panelX + 191, panelY + 359, 0xFF343943);
        context.drawText(textRenderer, Text.literal("STORAGE ESP"), panelX + 20, panelY + 18, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("VISUAL"), panelX + 20, panelY + 58, 0xFF37C9E8, false);
        context.drawText(textRenderer, Text.literal("Storage ESP"), panelX + 32, panelY + 78, 0xFFE0E3E9, false);
        context.drawText(textRenderer, Text.literal("WORLD"), panelX + 20, panelY + 116, 0xFF37C9E8, false);
        context.drawText(textRenderer, Text.literal("Sus chunk finder"), panelX + 32, panelY + 136, 0xFFE0E3E9, false);
        var suspects = StorageEspClient.getSuspectChunks();
        context.drawText(textRenderer, Text.literal("Sus chunks: " + suspects.size()), panelX + 20,
                panelY + 174, 0xFFE0E3E9, false);
        if (!suspects.isEmpty()) {
            var top = suspects.get(0);
            context.drawText(textRenderer, Text.literal("Top: " + top.x() + ", " + top.z() + " (" + top.count() + ")"),
                    panelX + 20, panelY + 192, 0xFFB1B6C0, false);
        }
        context.drawText(textRenderer, Text.literal("Loaded chunks only"), panelX + 20, panelY + 310, 0xFF969CA8, false);
        context.drawText(textRenderer, Text.literal("Counts containers, not hidden contents."), panelX + 205,
                panelY + 288, 0xFFB1B6C0, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        config.alertMessage = alertField == null ? config.alertMessage : alertField.getText();
        config.sanitize();
        config.save();
        if (client != null) client.setScreen(parent);
    }
}


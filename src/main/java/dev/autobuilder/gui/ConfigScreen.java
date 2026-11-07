package dev.autobuilder.gui;

import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.literal("AutoBuilder Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig cfg = ConfigManager.get();
        int centerX = this.width / 2;
        int startY = 40;
        int btnW = 150;
        int btnH = 20;
        int spacing = 24;

        int col1 = centerX - 160;
        int col2 = centerX + 10;

        // Column 1: Core Mechanics
        // Camera Speed
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cam Speed: §b" + (int) cfg.cameraRotationSpeed + "°/tick"),
                btn -> {
                    cfg.cameraRotationSpeed = (cfg.cameraRotationSpeed >= 40.0f) ? 5.0f : cfg.cameraRotationSpeed + 5.0f;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY, btnW, btnH).build());

        // Placement Delay
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Place Delay: §b" + cfg.placementDelayTicks + " ticks"),
                btn -> {
                    cfg.placementDelayTicks = (cfg.placementDelayTicks >= 10) ? 1 : cfg.placementDelayTicks + 1;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY + spacing, btnW, btnH).build());

        // Mining Delay
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mine Delay: §b" + cfg.miningDelayTicks + " ticks"),
                btn -> {
                    cfg.miningDelayTicks = (cfg.miningDelayTicks >= 10) ? 1 : cfg.miningDelayTicks + 1;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY + spacing * 2, btnW, btnH).build());

        // Max Reach
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(String.format("Reach: §b%.1f blocks", cfg.maxBuildReach)),
                btn -> {
                    cfg.maxBuildReach = (cfg.maxBuildReach >= 4.8) ? 3.0 : cfg.maxBuildReach + 0.3;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY + spacing * 3, btnW, btnH).build());

        // Clear Obstacles
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Obstacles: " + (cfg.breakObstacles ? "§aMine" : "§cIgnore")),
                btn -> {
                    cfg.breakObstacles = !cfg.breakObstacles;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY + spacing * 4, btnW, btnH).build());

        // Break Containers
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Containers: " + (cfg.breakContainers ? "§cBreak" : "§aProtect")),
                btn -> {
                    cfg.breakContainers = !cfg.breakContainers;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY + spacing * 5, btnW, btnH).build());

        // Smooth Camera
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Smooth Cam: " + (cfg.smoothCamera ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.smoothCamera = !cfg.smoothCamera;
                    this.clearAndInit();
                }
        ).dimensions(col1, startY + spacing * 6, btnW, btnH).build());

        // Column 2: Climbing & Tall Structures
        // Climb Mode
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Climb Mode: " + (cfg.climbMode ? "§aEnabled" : "§cDisabled")),
                btn -> {
                    cfg.climbMode = !cfg.climbMode;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY, btnW, btnH).build());

        // Auto Jump
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Auto Jump: " + (cfg.autoJump ? "§aAuto" : "§eManual")),
                btn -> {
                    cfg.autoJump = !cfg.autoJump;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY + spacing, btnW, btnH).build());

        // Remove Climb Blocks
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cleanup Climb: " + (cfg.removeClimbBlocks ? "§aAuto" : "§cKeep")),
                btn -> {
                    cfg.removeClimbBlocks = !cfg.removeClimbBlocks;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY + spacing * 2, btnW, btnH).build());

        // Pause on Unloaded Chunk
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Pause Unloaded: " + (cfg.pauseOnUnloadedChunk ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.pauseOnUnloadedChunk = !cfg.pauseOnUnloadedChunk;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY + spacing * 3, btnW, btnH).build());

        // Save Progress
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save Progress: " + (cfg.saveProgress ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.saveProgress = !cfg.saveProgress;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY + spacing * 4, btnW, btnH).build());

        // Pause on Missing
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Pause Missing: " + (cfg.pauseOnMissing ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.pauseOnMissing = !cfg.pauseOnMissing;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY + spacing * 5, btnW, btnH).build());

        // Layer-by-layer
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Layer by Layer: " + (cfg.layerByLayer ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.layerByLayer = !cfg.layerByLayer;
                    this.clearAndInit();
                }
        ).dimensions(col2, startY + spacing * 6, btnW, btnH).build());

        // Bottom: Save & Return
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§a✔ Save & Return"), btn -> {
            ConfigManager.save();
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lAutoBuilder Settings"), centerX, 15, 0xFFFFFFFF);
    }
}

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
        int startY = 45;
        int btnW = 260;
        int btnH = 20;
        int spacing = 22;

        // Camera Speed
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Camera Speed: §b" + (int) cfg.cameraRotationSpeed + "°/tick"),
                btn -> {
                    cfg.cameraRotationSpeed = (cfg.cameraRotationSpeed >= 40.0f) ? 5.0f : cfg.cameraRotationSpeed + 5.0f;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY, btnW, btnH).build());

        // Placement Delay
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Placement Delay: §b" + cfg.placementDelayTicks + " ticks"),
                btn -> {
                    cfg.placementDelayTicks = (cfg.placementDelayTicks >= 10) ? 1 : cfg.placementDelayTicks + 1;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing, btnW, btnH).build());

        // Mining Delay
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mining Delay: §b" + cfg.miningDelayTicks + " ticks"),
                btn -> {
                    cfg.miningDelayTicks = (cfg.miningDelayTicks >= 10) ? 1 : cfg.miningDelayTicks + 1;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing * 2, btnW, btnH).build());

        // Max Reach
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(String.format("Max Reach: §b%.1f blocks", cfg.maxBuildReach)),
                btn -> {
                    cfg.maxBuildReach = (cfg.maxBuildReach >= 4.8) ? 3.0 : cfg.maxBuildReach + 0.3;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing * 3, btnW, btnH).build());

        // Break Obstacles
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Clear Obstacles: " + (cfg.breakObstacles ? "§aEnabled" : "§cDisabled")),
                btn -> {
                    cfg.breakObstacles = !cfg.breakObstacles;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing * 4, btnW, btnH).build());

        // Break Containers
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Break Containers (Chests/Entities): " + (cfg.breakContainers ? "§cAllowed" : "§aProtected")),
                btn -> {
                    cfg.breakContainers = !cfg.breakContainers;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing * 5, btnW, btnH).build());

        // Pause on Missing
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Pause on Missing Blocks: " + (cfg.pauseOnMissing ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.pauseOnMissing = !cfg.pauseOnMissing;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing * 6, btnW, btnH).build());

        // Smooth Camera
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Smooth Camera Movement: " + (cfg.smoothCamera ? "§aON" : "§cOFF")),
                btn -> {
                    cfg.smoothCamera = !cfg.smoothCamera;
                    this.clearAndInit();
                }
        ).dimensions(centerX - 130, startY + spacing * 7, btnW, btnH).build());

        // Save & Back
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aSave & Return"), btn -> {
            ConfigManager.save();
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX - 75, this.height - 28, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lAutoBuilder Settings"), centerX, 15, 0xFFFFFFFF);
    }
}

package dev.autobuilder.gui;

import dev.autobuilder.building.BuildManager;
import dev.autobuilder.building.BuildState;
import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.schematic.SchematicManager;
import dev.autobuilder.selection.CuboidRegion;
import dev.autobuilder.selection.SelectionManager;
import dev.autobuilder.util.TextConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AutoBuilderScreen extends Screen {

    public AutoBuilderScreen() {
        super(Text.literal("AutoBuilder Control Panel"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 80;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int spacing = 24;

        // Column 1 - Construction Controls
        BuildManager bm = BuildManager.getInstance();
        Schematic activeSchem = SchematicManager.getInstance().getActiveSchematic();

        ButtonWidget startBtn = ButtonWidget.builder(Text.literal("§a▶ Start Building"), btn -> {
            if (activeSchem != null && client != null && client.player != null) {
                bm.startBuild(activeSchem, activeSchem.getOrigin());
                this.close();
            }
        }).dimensions(centerX - 160, startY, buttonWidth, buttonHeight).build();
        startBtn.active = activeSchem != null && !bm.isBuilding();
        this.addDrawableChild(startBtn);

        ButtonWidget pauseBtn = ButtonWidget.builder(
                Text.literal(bm.getState() == BuildState.PAUSED_MANUAL ? "§e▶ Resume" : "§6⏸ Pause"),
                btn -> {
                    if (bm.getState() == BuildState.PAUSED_MANUAL || bm.getState() == BuildState.PAUSED_MISSING_MATERIALS) {
                        bm.resumeBuild();
                    } else {
                        bm.pauseBuild();
                    }
                    this.init();
                }
        ).dimensions(centerX - 160, startY + spacing, buttonWidth, buttonHeight).build();
        pauseBtn.active = bm.isBuilding();
        this.addDrawableChild(pauseBtn);

        ButtonWidget stopBtn = ButtonWidget.builder(Text.literal("§c⏹ Cancel Build"), btn -> {
            bm.stopBuild();
            this.init();
        }).dimensions(centerX - 160, startY + spacing * 2, buttonWidth, buttonHeight).build();
        stopBtn.active = bm.isBuilding();
        this.addDrawableChild(stopBtn);

        ButtonWidget previewBtn = ButtonWidget.builder(Text.literal("Preview & Nudge"), btn -> {
            if (client != null) client.setScreen(new PreviewControlScreen(this));
        }).dimensions(centerX - 160, startY + spacing * 3, buttonWidth, buttonHeight).build();
        previewBtn.active = activeSchem != null;
        this.addDrawableChild(previewBtn);

        // Column 2 - Management & Settings
        ButtonWidget schemBtn = ButtonWidget.builder(Text.literal("📁 Schematic Manager"), btn -> {
            if (client != null) client.setScreen(new SchematicManagerScreen(this));
        }).dimensions(centerX + 10, startY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(schemBtn);

        ButtonWidget copyBtn = ButtonWidget.builder(Text.literal("📋 Copy Selection"), btn -> {
            Schematic copied = SelectionManager.getInstance().copySelection();
            if (copied != null) {
                if (client != null && client.player != null) {
                    copied.setOrigin(client.player.getBlockPos());
                }
                SchematicManager.getInstance().setActiveSchematic(copied);
                if (client != null && client.player != null) {
                    client.player.sendMessage(TextConstants.success("Selection copied to active clipboard!"), false);
                }
                this.init();
            }
        }).dimensions(centerX + 10, startY + spacing, buttonWidth, buttonHeight).build();
        copyBtn.active = SelectionManager.getInstance().hasCompleteSelection();
        this.addDrawableChild(copyBtn);

        ButtonWidget clearSelBtn = ButtonWidget.builder(Text.literal("❌ Clear Selection"), btn -> {
            SelectionManager.getInstance().clear();
            this.init();
        }).dimensions(centerX + 10, startY + spacing * 2, buttonWidth, buttonHeight).build();
        clearSelBtn.active = SelectionManager.getInstance().getPos1() != null || SelectionManager.getInstance().getPos2() != null;
        this.addDrawableChild(clearSelBtn);

        ButtonWidget configBtn = ButtonWidget.builder(Text.literal("⚙ Settings"), btn -> {
            if (client != null) client.setScreen(new ConfigScreen(this));
        }).dimensions(centerX + 10, startY + spacing * 3, buttonWidth, buttonHeight).build();
        this.addDrawableChild(configBtn);

        // Close button at bottom
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> this.close())
                .dimensions(centerX - 60, this.height - 35, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lAutoBuilder Control Panel"), centerX, 20, 0xFFFFFFFF);

        // Active Schematic Info
        Schematic schem = SchematicManager.getInstance().getActiveSchematic();
        String schemInfo = schem != null
                ? "§7Active: §b" + schem.getName() + " §7(" + schem.getWidth() + "x" + schem.getHeight() + "x" + schem.getLength() + ", " + schem.getNonAirBlockCount() + " blocks)"
                : "§7Active Schematic: §cNone (Load or Copy a structure)";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(schemInfo), centerX, 42, 0xFFE0E0E0);

        // Selection info
        SelectionManager sm = SelectionManager.getInstance();
        CuboidRegion reg = sm.getRegion();
        String selInfo = reg != null
                ? "§7Selection: §a" + reg.getWidth() + "x" + reg.getHeight() + "x" + reg.getLength() + " §7(" + reg.getVolume() + " volume)"
                : "§7Selection: §8No selection active";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(selInfo), centerX, 56, 0xFFCCCCCC);
    }
}

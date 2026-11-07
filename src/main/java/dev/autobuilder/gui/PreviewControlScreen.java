package dev.autobuilder.gui;

import dev.autobuilder.building.BuildManager;
import dev.autobuilder.client.render.HologramRenderer;
import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.schematic.SchematicManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

public class PreviewControlScreen extends Screen {
    private final Screen parent;

    public PreviewControlScreen(Screen parent) {
        super(Text.literal("Hologram Preview & Controls"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 60;
        int btnW = 45;
        int btnH = 20;

        Schematic schem = SchematicManager.getInstance().getActiveSchematic();

        // X Nudge
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-X"), btn -> nudge(-1, 0, 0))
                .dimensions(centerX - 80, startY, btnW, btnH).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+X"), btn -> nudge(1, 0, 0))
                .dimensions(centerX + 35, startY, btnW, btnH).build());

        // Y Nudge
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-Y"), btn -> nudge(0, -1, 0))
                .dimensions(centerX - 80, startY + 26, btnW, btnH).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+Y"), btn -> nudge(0, 1, 0))
                .dimensions(centerX + 35, startY + 26, btnW, btnH).build());

        // Z Nudge
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-Z"), btn -> nudge(0, 0, -1))
                .dimensions(centerX - 80, startY + 52, btnW, btnH).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+Z"), btn -> nudge(0, 0, 1))
                .dimensions(centerX + 35, startY + 52, btnW, btnH).build());

        // Align to player feet
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Align to Player"), btn -> {
            if (schem != null && client != null && client.player != null) {
                schem.setOrigin(client.player.getBlockPos());
            }
        }).dimensions(centerX - 80, startY + 80, 160, 20).build());

        // Rotations
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Rotate 90° CW"), btn -> {
            if (schem != null) schem.rotate(BlockRotation.CLOCKWISE_90);
        }).dimensions(centerX - 130, startY + 106, 125, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Rotate 90° CCW"), btn -> {
            if (schem != null) schem.rotate(BlockRotation.COUNTERCLOCKWISE_90);
        }).dimensions(centerX + 5, startY + 106, 125, 20).build());

        // Mirrors
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mirror X"), btn -> {
            if (schem != null) schem.mirror(BlockMirror.FRONT_BACK);
        }).dimensions(centerX - 130, startY + 132, 125, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mirror Z"), btn -> {
            if (schem != null) schem.mirror(BlockMirror.LEFT_RIGHT);
        }).dimensions(centerX + 5, startY + 132, 125, 20).build());

        // Toggle hologram
        boolean holoOn = HologramRenderer.getInstance().isEnabled();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Hologram: " + (holoOn ? "§aON" : "§cOFF")), btn -> {
            HologramRenderer.getInstance().toggle();
            this.clearAndInit();
        }).dimensions(centerX - 130, startY + 158, 260, 20).build());

        // Confirm & Start
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§a✔ Confirm & Start Building"), btn -> {
            if (schem != null) {
                BuildManager.getInstance().startBuild(schem, schem.getOrigin());
                this.close();
            }
        }).dimensions(centerX - 130, startY + 184, 125, 20).build());

        // Back
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX + 5, startY + 184, 125, 20).build());
    }

    private void nudge(int dx, int dy, int dz) {
        Schematic schem = SchematicManager.getInstance().getActiveSchematic();
        if (schem != null) {
            BlockPos current = schem.getOrigin();
            schem.setOrigin(current.add(dx, dy, dz));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lPreview & Structure Placement"), centerX, 15, 0xFFFFFFFF);

        Schematic schem = SchematicManager.getInstance().getActiveSchematic();
        if (schem != null) {
            BlockPos org = schem.getOrigin();
            String posStr = "§7Origin: §eX: " + org.getX() + "  Y: " + org.getY() + "  Z: " + org.getZ();
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(posStr), centerX, 36, 0xFFFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§cNo active schematic loaded"), centerX, 36, 0xFFFF5555);
        }
    }
}

package dev.autobuilder.client.render;

import dev.autobuilder.building.BuildManager;
import dev.autobuilder.building.BuildState;
import dev.autobuilder.building.BuildTask;
import dev.autobuilder.util.BlockUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class BuildProgressHud implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        BuildManager bm = BuildManager.getInstance();
        if (!bm.isBuilding() && bm.getState() != BuildState.COMPLETED) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        int x = 10;
        int y = 10;
        int width = 160;
        int height = 55;

        // Dark background with cyan border
        drawContext.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
        drawContext.fill(x - 2, y - 2, x + width + 2, y, 0xFF00CED1);

        // Status Header
        String stateStr = formatState(bm.getState());
        drawContext.drawTextWithShadow(tr, Text.literal("§b§lAutoBuilder §7| " + stateStr), x + 4, y + 4, 0xFFFFFFFF);

        // Progress metrics
        int total = bm.getTotalTasks();
        int current = bm.getTaskIndex();
        float percent = total > 0 ? (float) current / total : 0.0f;
        int percentInt = (int) (percent * 100);

        String progressText = "Blocks: " + current + " / " + total + " (" + percentInt + "%)";
        drawContext.drawTextWithShadow(tr, Text.literal("§f" + progressText), x + 4, y + 16, 0xFFE0E0E0);

        // Progress Bar
        int barX = x + 4;
        int barY = y + 27;
        int barWidth = width - 8;
        int barHeight = 6;
        int progressWidth = (int) (barWidth * percent);

        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        drawContext.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00E5FF);

        // Current block and layer info
        BuildTask task = bm.getCurrentTask();
        if (task != null) {
            String blockName = BlockUtils.getBlockName(task.getDesiredState());
            drawContext.drawTextWithShadow(tr, Text.literal("§7Next: §e" + blockName), x + 4, y + 36, 0xFFCCCCCC);
            drawContext.drawTextWithShadow(tr, Text.literal("§7Y: §b" + task.getWorldPos().getY()), x + width - 35, y + 36, 0xFFCCCCCC);
        } else if (bm.getState() == BuildState.COMPLETED) {
            drawContext.drawTextWithShadow(tr, Text.literal("§a✔ Construction Completed!"), x + 4, y + 36, 0xFF55FF55);
        }
    }

    private String formatState(BuildState state) {
        return switch (state) {
            case PLACING -> "§aPlacing";
            case MINING_OBSTACLE -> "§cClearing Obstacle";
            case ROTATING -> "§eAligning View";
            case WAITING_SERVER_ACK -> "§6Verifying";
            case PAUSED_MISSING_MATERIALS -> "§cMissing Materials";
            case PAUSED_MANUAL -> "§6Paused";
            case COMPLETED -> "§aDone";
            case CALCULATING -> "§7Calculating";
            default -> "§7Idle";
        };
    }
}

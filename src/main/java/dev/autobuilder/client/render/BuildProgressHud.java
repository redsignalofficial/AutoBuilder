package dev.autobuilder.client.render;

import dev.autobuilder.building.BuildManager;
import dev.autobuilder.building.BuildState;
import dev.autobuilder.building.BuildTask;
import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.util.BlockUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class BuildProgressHud implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        BuildManager bm = BuildManager.getInstance();
        if (!bm.isBuilding() && bm.getState() != BuildState.COMPLETED) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        int x = 10;
        int y = 10;
        int width = 185;
        int height = 75;

        // Dark background with cyan border
        drawContext.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
        drawContext.fill(x - 2, y - 2, x + width + 2, y, 0xFF00CED1);

        // 1. Status Header
        String stateStr = formatState(bm.getState());
        drawContext.drawTextWithShadow(tr, Text.literal("§b§lAutoBuilder §7| " + stateStr), x + 4, y + 4, 0xFFFFFFFF);

        // 2. Progress metrics
        int total = bm.getTotalTasks();
        int current = bm.getTaskIndex();
        float percent = total > 0 ? (float) current / total : 0.0f;
        int percentInt = (int) (percent * 100);

        String progressText = "Blocks: " + current + " / " + total + " (" + percentInt + "%)";
        drawContext.drawTextWithShadow(tr, Text.literal("§f" + progressText), x + 4, y + 16, 0xFFE0E0E0);

        // 3. Progress Bar
        int barX = x + 4;
        int barY = y + 27;
        int barWidth = width - 8;
        int barHeight = 5;
        int progressWidth = (int) (barWidth * percent);

        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        drawContext.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00E5FF);

        // 4. Rate & ETA (U7)
        float bps = bm.getBlocksPerSecond();
        int etaSec = bm.getEstimatedSecondsRemaining();
        String etaStr = (etaSec >= 0) ? formatEta(etaSec) : "--";
        String speedStr = String.format("§7Speed: §e%.1f b/s §7| ETA: §a%s", bps, etaStr);
        drawContext.drawTextWithShadow(tr, Text.literal(speedStr), x + 4, y + 36, 0xFFDDDDDD);

        // 5. Next block & Standing Height Advisory (U1)
        BuildTask task = bm.getCurrentTask();
        if (task != null) {
            String blockName = BlockUtils.getBlockName(task.getDesiredState());
            drawContext.drawTextWithShadow(tr, Text.literal("§7Next: §e" + blockName + " §7(Y:" + task.getWorldPos().getY() + ")"), x + 4, y + 48, 0xFFCCCCCC);

            BlockPos standPos = bm.getRecommendedStandPos();
            if (standPos != null && ConfigManager.get().showStandPoints) {
                int playerY = client.player.getBlockY();
                int targetY = standPos.getY();
                if (playerY < targetY) {
                    drawContext.drawTextWithShadow(tr, Text.literal("§c▲ Climb needed (Target Y: " + targetY + ")"), x + 4, y + 60, 0xFFFF7777);
                } else {
                    drawContext.drawTextWithShadow(tr, Text.literal("§aStand: Y=" + targetY + " (In reach)"), x + 4, y + 60, 0xFF77FF77);
                }
            }
        } else if (bm.getState() == BuildState.COMPLETED) {
            drawContext.drawTextWithShadow(tr, Text.literal("§a✔ Construction Completed!"), x + 4, y + 50, 0xFF55FF55);
        }
    }

    private String formatEta(int seconds) {
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;
        int sec = seconds % 60;
        if (minutes < 60) return minutes + "m " + sec + "s";
        int hours = minutes / 60;
        int min = minutes % 60;
        return hours + "h " + min + "m";
    }

    private String formatState(BuildState state) {
        return switch (state) {
            case PLACING -> "§aPlacing";
            case CLIMBING -> "§eClimbing";
            case CLEANING_CLIMB -> "§6Cleaning Climb";
            case MINING_OBSTACLE -> "§cClearing Obstacle";
            case ROTATING -> "§eAligning View";
            case WAITING_SERVER_ACK -> "§6Verifying";
            case PAUSED_MISSING_MATERIALS -> "§cMissing Materials";
            case PAUSED_CHUNK_UNLOADED -> "§cChunk Unloaded";
            case PAUSED_OUT_OF_REACH -> "§eOut of Reach";
            case PAUSED_MANUAL -> "§6Paused";
            case COMPLETED -> "§aDone";
            case CALCULATING -> "§7Calculating";
            default -> "§7Idle";
        };
    }
}

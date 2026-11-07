package dev.autobuilder.client.render;

import dev.autobuilder.selection.CuboidRegion;
import dev.autobuilder.selection.SelectionManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class SelectionRenderer {

    public static void render(WorldRenderContext context) {
        SelectionManager sm = SelectionManager.getInstance();
        BlockPos p1 = sm.getPos1();
        BlockPos p2 = sm.getPos2();
        CuboidRegion region = sm.getRegion();

        if (p1 == null && p2 == null && region == null) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        VertexConsumer linesConsumer = consumers.getBuffer(RenderLayer.LINES);
        MatrixStack matrices = context.matrices();
        Vec3d camPos = mc.player.getEyePos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        // Render full selection box if both are selected
        if (region != null) {
            Box box = region.toBoundingBox();
            drawBoxLines(matrices, linesConsumer, box, 0.2f, 0.8f, 1.0f, 1.0f);
        }

        // Render Pos1 marker (Blue)
        if (p1 != null) {
            Box box1 = new Box(p1);
            drawBoxLines(matrices, linesConsumer, box1, 0.0f, 0.5f, 1.0f, 0.9f);
        }

        // Render Pos2 marker (Green)
        if (p2 != null) {
            Box box2 = new Box(p2);
            drawBoxLines(matrices, linesConsumer, box2, 0.0f, 1.0f, 0.5f, 0.9f);
        }

        matrices.pop();
    }

    public static void drawBoxLines(MatrixStack matrices, VertexConsumer consumer, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        drawLine(matrix, consumer, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLine(matrix, consumer, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLine(matrix, consumer, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLine(matrix, consumer, x1, y1, z2, x1, y1, z1, r, g, b, a);

        drawLine(matrix, consumer, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLine(matrix, consumer, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLine(matrix, consumer, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLine(matrix, consumer, x1, y2, z2, x1, y2, z1, r, g, b, a);

        drawLine(matrix, consumer, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLine(matrix, consumer, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLine(matrix, consumer, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLine(matrix, consumer, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void drawLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 0 ? dx / len : 0;
        float ny = len > 0 ? dy / len : 1;
        float nz = len > 0 ? dz / len : 0;

        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(nx, ny, nz);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(nx, ny, nz);
    }
}

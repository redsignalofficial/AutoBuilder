package dev.autobuilder.client.render;

import dev.autobuilder.selection.CuboidRegion;
import dev.autobuilder.selection.SelectionManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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

        VertexConsumer linesConsumer = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        // Render full selection box if both are selected
        if (region != null) {
            Box box = region.toBoundingBox();
            WorldRenderer.drawBox(matrices, linesConsumer, box, 0.2f, 0.8f, 1.0f, 1.0f);
        }

        // Render Pos1 marker (Blue)
        if (p1 != null) {
            Box box1 = new Box(p1);
            WorldRenderer.drawBox(matrices, linesConsumer, box1, 0.0f, 0.5f, 1.0f, 0.9f);
        }

        // Render Pos2 marker (Green)
        if (p2 != null) {
            Box box2 = new Box(p2);
            WorldRenderer.drawBox(matrices, linesConsumer, box2, 0.0f, 1.0f, 0.5f, 0.9f);
        }

        matrices.pop();
    }
}

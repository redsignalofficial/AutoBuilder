package dev.autobuilder.client.render;

import dev.autobuilder.interaction.InventoryHelper;
import dev.autobuilder.obstacle.ObstacleDetector;
import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.schematic.SchematicManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class HologramRenderer {
    private static final HologramRenderer INSTANCE = new HologramRenderer();

    private boolean enabled = true;
    private static final int MAX_RENDER_RADIUS = 24;

    private HologramRenderer() {}

    public static HologramRenderer getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }

    public static void render(WorldRenderContext context) {
        if (!INSTANCE.enabled) return;

        Schematic schematic = SchematicManager.getInstance().getActiveSchematic();
        if (schematic == null) return;

        BlockPos origin = schematic.getOrigin();
        if (origin == null) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        VertexConsumer linesConsumer = consumers.getBuffer(RenderLayer.lines());
        MatrixStack matrices = context.matrices();
        Vec3d camPos = mc.player.getEyePos();
        ClientWorld world = mc.world;
        if (world == null) return;

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        // 1. Draw outer boundary box of entire schematic
        Box bounds = new Box(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + schematic.getWidth(),
                origin.getY() + schematic.getHeight(),
                origin.getZ() + schematic.getLength()
        );
        SelectionRenderer.drawBoxLines(matrices, linesConsumer, bounds, 0.9f, 0.3f, 0.9f, 0.8f);

        // 2. Render blocks within nearby radius of camera
        int minX = Math.max(0, (int) (camPos.x - origin.getX() - MAX_RENDER_RADIUS));
        int maxX = Math.min(schematic.getWidth() - 1, (int) (camPos.x - origin.getX() + MAX_RENDER_RADIUS));
        int minY = Math.max(0, (int) (camPos.y - origin.getY() - MAX_RENDER_RADIUS));
        int maxY = Math.min(schematic.getHeight() - 1, (int) (camPos.y - origin.getY() + MAX_RENDER_RADIUS));
        int minZ = Math.max(0, (int) (camPos.z - origin.getZ() - MAX_RENDER_RADIUS));
        int maxZ = Math.min(schematic.getLength() - 1, (int) (camPos.z - origin.getZ() + MAX_RENDER_RADIUS));

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockState desired = schematic.getBlockState(x, y, z);
                    if (desired.isAir()) continue;

                    BlockPos worldPos = origin.add(x, y, z);
                    BlockState currentState = world.getBlockState(worldPos);

                    float r, g, b, a;
                    if (currentState.getBlock() == desired.getBlock()) {
                        // Already placed (green)
                        r = 0.0f; g = 1.0f; b = 0.3f; a = 0.25f;
                    } else if (ObstacleDetector.isObstacle(world, worldPos, desired)) {
                        // Obstacle needing removal (red)
                        r = 1.0f; g = 0.1f; b = 0.1f; a = 0.7f;
                    } else if (!InventoryHelper.hasItemForBlock(desired.getBlock())) {
                        // Missing item (orange/yellow)
                        r = 1.0f; g = 0.7f; b = 0.0f; a = 0.5f;
                    } else {
                        // Normal to-be-built block (cyan)
                        r = 0.0f; g = 0.8f; b = 1.0f; a = 0.4f;
                    }

                    Box blockBox = new Box(worldPos);
                    SelectionRenderer.drawBoxLines(matrices, linesConsumer, blockBox, r, g, b, a);
                }
            }
        }

        matrices.pop();
    }
}

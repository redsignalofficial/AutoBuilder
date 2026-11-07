package dev.autobuilder.interaction;

import dev.autobuilder.camera.CameraController;
import dev.autobuilder.network.PlacementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class InteractionController {
    private static final InteractionController INSTANCE = new InteractionController();

    private InteractionController() {}

    public static InteractionController getInstance() {
        return INSTANCE;
    }

    public boolean placeBlock(BlockPos targetPos, BlockState desiredState) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (player == null || world == null || interactionManager == null) {
            return false;
        }

        if (!InventoryHelper.prepareItemInHand(desiredState.getBlock())) {
            return false;
        }

        BlockHitResult hitResult = RaycastHelper.findPlacementHitResult(targetPos);
        if (hitResult == null) {
            return false;
        }

        CameraController.getInstance().setTarget(hitResult.getPos());

        if (!CameraController.getInstance().isAligned()) {
            return false;
        }

        ActionResult result = interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        player.swingHand(Hand.MAIN_HAND);

        if (result.isAccepted()) {
            PlacementTracker.getInstance().recordPlacement(targetPos, desiredState);
            return true;
        }

        return false;
    }
}

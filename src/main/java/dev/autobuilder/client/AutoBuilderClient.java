package dev.autobuilder.client;

import dev.autobuilder.AutoBuilder;
import dev.autobuilder.building.BuildManager;
import dev.autobuilder.camera.CameraController;
import dev.autobuilder.client.render.BuildProgressHud;
import dev.autobuilder.client.render.HologramRenderer;
import dev.autobuilder.client.render.SelectionRenderer;
import dev.autobuilder.command.AutoBuilderCommands;
import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.gui.AutoBuilderScreen;
import dev.autobuilder.selection.SelectionManager;
import dev.autobuilder.util.TextConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class AutoBuilderClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;
    private static KeyBinding toggleBuildKey;
    private static KeyBinding setPos1Key;
    private static KeyBinding setPos2Key;

    @Override
    public void onInitializeClient() {
        AutoBuilder.LOGGER.info("[{}] Initializing AutoBuilder client...", AutoBuilder.MOD_NAME);
        ConfigManager.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("autobuilder", "main"));

        // Register keybindings
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobuilder.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));

        toggleBuildKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobuilder.toggle_build",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                category
        ));

        setPos1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobuilder.set_pos1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                category
        ));

        setPos2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobuilder.set_pos2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                category
        ));

        // Client Tick Event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (openGuiKey.wasPressed()) {
                client.setScreen(new AutoBuilderScreen());
            }

            while (toggleBuildKey.wasPressed()) {
                BuildManager bm = BuildManager.getInstance();
                if (bm.isBuilding()) {
                    bm.pauseBuild();
                } else {
                    bm.resumeBuild();
                }
            }

            while (setPos1Key.wasPressed()) {
                BlockPos pos = getTargetOrPlayerPos(client);
                SelectionManager.getInstance().setPos1(pos);
                client.player.sendMessage(TextConstants.success("Pos 1 set: " + pos.toShortString()), false);
            }

            while (setPos2Key.wasPressed()) {
                BlockPos pos = getTargetOrPlayerPos(client);
                SelectionManager.getInstance().setPos2(pos);
                client.player.sendMessage(TextConstants.success("Pos 2 set: " + pos.toShortString()), false);
            }

            // Subsystem ticks
            CameraController.getInstance().tick();
            BuildManager.getInstance().tick();
        });

        // World Rendering (selection box + hologram ghost preview)
        WorldRenderEvents.END_MAIN.register(context -> {
            SelectionRenderer.render(context);
            HologramRenderer.render(context);
        });

        // HUD Overlay
        HudRenderCallback.EVENT.register(new BuildProgressHud());

        // Client Commands
        ClientCommandRegistrationCallback.EVENT.register(AutoBuilderCommands::register);

        AutoBuilder.LOGGER.info("[{}] AutoBuilder client initialized successfully.", AutoBuilder.MOD_NAME);
    }

    private static BlockPos getTargetOrPlayerPos(MinecraftClient client) {
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) client.crosshairTarget).getBlockPos();
        }
        if (client.player != null) {
            return client.player.getBlockPos();
        }
        return BlockPos.ORIGIN;
    }
}

package dev.autobuilder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.autobuilder.building.BuildManager;
import dev.autobuilder.building.BuildState;
import dev.autobuilder.client.render.HologramRenderer;
import dev.autobuilder.gui.AutoBuilderScreen;
import dev.autobuilder.gui.ConfigScreen;
import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.schematic.SchematicManager;
import dev.autobuilder.selection.CuboidRegion;
import dev.autobuilder.selection.SelectionManager;
import dev.autobuilder.util.TextConstants;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.io.File;

public class AutoBuilderCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("autobuilder")
                .executes(ctx -> openGui())
                .then(ClientCommandManager.literal("gui").executes(ctx -> openGui()))
                .then(ClientCommandManager.literal("pos1").executes(ctx -> setPos1(ctx.getSource())))
                .then(ClientCommandManager.literal("pos2").executes(ctx -> setPos2(ctx.getSource())))
                .then(ClientCommandManager.literal("clear").executes(ctx -> clearSelection(ctx.getSource())))
                .then(ClientCommandManager.literal("copy").executes(ctx -> copySelection(ctx.getSource())))
                .then(ClientCommandManager.literal("preview").executes(ctx -> togglePreview(ctx.getSource())))
                .then(ClientCommandManager.literal("build").executes(ctx -> startBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("pause").executes(ctx -> pauseBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("resume").executes(ctx -> resumeBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("stop").executes(ctx -> stopBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("config").executes(ctx -> openConfig()))
                .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> saveSchematic(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(ClientCommandManager.literal("load")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> loadSchematic(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
        );

        // Alias /ab
        dispatcher.register(ClientCommandManager.literal("ab")
                .executes(ctx -> openGui())
                .then(ClientCommandManager.literal("gui").executes(ctx -> openGui()))
                .then(ClientCommandManager.literal("pos1").executes(ctx -> setPos1(ctx.getSource())))
                .then(ClientCommandManager.literal("pos2").executes(ctx -> setPos2(ctx.getSource())))
                .then(ClientCommandManager.literal("clear").executes(ctx -> clearSelection(ctx.getSource())))
                .then(ClientCommandManager.literal("copy").executes(ctx -> copySelection(ctx.getSource())))
                .then(ClientCommandManager.literal("preview").executes(ctx -> togglePreview(ctx.getSource())))
                .then(ClientCommandManager.literal("build").executes(ctx -> startBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("pause").executes(ctx -> pauseBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("resume").executes(ctx -> resumeBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("stop").executes(ctx -> stopBuild(ctx.getSource())))
                .then(ClientCommandManager.literal("config").executes(ctx -> openConfig()))
                .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> saveSchematic(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(ClientCommandManager.literal("load")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(ctx -> loadSchematic(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
        );
    }

    private static int openGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(new AutoBuilderScreen()));
        return 1;
    }

    private static int openConfig() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(new ConfigScreen(null)));
        return 1;
    }

    private static int setPos1(FabricClientCommandSource source) {
        BlockPos pos = getTargetOrPlayerPos();
        SelectionManager.getInstance().setPos1(pos);
        source.sendFeedback(TextConstants.success("Position 1 set to: " + pos.toShortString()));
        return 1;
    }

    private static int setPos2(FabricClientCommandSource source) {
        BlockPos pos = getTargetOrPlayerPos();
        SelectionManager.getInstance().setPos2(pos);
        source.sendFeedback(TextConstants.success("Position 2 set to: " + pos.toShortString()));
        return 1;
    }

    private static int clearSelection(FabricClientCommandSource source) {
        SelectionManager.getInstance().clear();
        source.sendFeedback(TextConstants.info("Selection cleared."));
        return 1;
    }

    private static int copySelection(FabricClientCommandSource source) {
        Schematic copied = SelectionManager.getInstance().copySelection();
        if (copied != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                copied.setOrigin(client.player.getBlockPos());
            }
            SchematicManager.getInstance().setActiveSchematic(copied);
            CuboidRegion reg = SelectionManager.getInstance().getRegion();
            source.sendFeedback(TextConstants.success("Copied " + reg.getVolume() + " blocks to active clipboard!"));
            return 1;
        } else {
            source.sendError(TextConstants.error("Select both Pos 1 and Pos 2 first!"));
            return 0;
        }
    }

    private static int togglePreview(FabricClientCommandSource source) {
        HologramRenderer.getInstance().toggle();
        boolean on = HologramRenderer.getInstance().isEnabled();
        source.sendFeedback(TextConstants.info("Hologram preview: " + (on ? "§aEnabled" : "§cDisabled")));
        return 1;
    }

    private static int startBuild(FabricClientCommandSource source) {
        Schematic schem = SchematicManager.getInstance().getActiveSchematic();
        if (schem == null) {
            source.sendError(TextConstants.error("No active schematic! Copy or load one first."));
            return 0;
        }
        BuildManager.getInstance().startBuild(schem, schem.getOrigin());
        return 1;
    }

    private static int pauseBuild(FabricClientCommandSource source) {
        BuildManager.getInstance().pauseBuild();
        return 1;
    }

    private static int resumeBuild(FabricClientCommandSource source) {
        BuildManager.getInstance().resumeBuild();
        return 1;
    }

    private static int stopBuild(FabricClientCommandSource source) {
        BuildManager.getInstance().stopBuild();
        return 1;
    }

    private static int saveSchematic(FabricClientCommandSource source, String name) {
        Schematic active = SchematicManager.getInstance().getActiveSchematic();
        if (active == null) {
            source.sendError(TextConstants.error("No active schematic to save!"));
            return 0;
        }
        SchematicManager.getInstance().saveAsync(active, name).thenAccept(file -> {
            source.sendFeedback(TextConstants.success("Saved schematic to: " + file.getName()));
        });
        return 1;
    }

    private static int loadSchematic(FabricClientCommandSource source, String name) {
        File dir = SchematicManager.getInstance().getSchematicsDirectory();
        File target = new File(dir, name.endsWith(".schem") ? name : name + ".schem");
        if (!target.exists()) {
            target = new File(dir, name + ".schematic");
            if (!target.exists()) {
                target = new File(dir, name + ".nbt");
            }
        }

        if (!target.exists()) {
            source.sendError(TextConstants.error("Schematic file not found: " + name));
            return 0;
        }

        SchematicManager.getInstance().loadAsync(target).thenAccept(schem -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                schem.setOrigin(client.player.getBlockPos());
            }
            source.sendFeedback(TextConstants.success("Loaded schematic: " + schem.getName() + " (" + schem.getWidth() + "x" + schem.getHeight() + "x" + schem.getLength() + ")"));
        });
        return 1;
    }

    private static BlockPos getTargetOrPlayerPos() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) client.crosshairTarget).getBlockPos();
        }
        if (client.player != null) {
            return client.player.getBlockPos();
        }
        return BlockPos.ORIGIN;
    }
}

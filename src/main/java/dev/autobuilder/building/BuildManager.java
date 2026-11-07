package dev.autobuilder.building;

import dev.autobuilder.camera.CameraController;
import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.config.ModConfig;
import dev.autobuilder.interaction.InteractionController;
import dev.autobuilder.network.PlacementTracker;
import dev.autobuilder.obstacle.ObstacleRemovalManager;
import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.util.BlockUtils;
import dev.autobuilder.util.TextConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BuildManager {
    private static final BuildManager INSTANCE = new BuildManager();

    private BuildState state = BuildState.IDLE;
    private Schematic activeSchematic;
    private BlockPos buildOrigin;
    private List<BuildTask> taskList = new ArrayList<>();
    private int taskIndex = 0;
    private int placementCooldown = 0;
    private long startTime = 0;

    private BuildManager() {}

    public static BuildManager getInstance() {
        return INSTANCE;
    }

    public BuildState getState() {
        return state;
    }

    public boolean isBuilding() {
        return state != BuildState.IDLE && state != BuildState.COMPLETED && state != BuildState.CANCELLED;
    }

    public int getTaskIndex() {
        return taskIndex;
    }

    public int getTotalTasks() {
        return taskList.size();
    }

    public BuildTask getCurrentTask() {
        if (taskIndex >= 0 && taskIndex < taskList.size()) {
            return taskList.get(taskIndex);
        }
        return null;
    }

    public void startBuild(Schematic schematic, BlockPos origin) {
        if (schematic == null || origin == null) return;

        this.activeSchematic = schematic;
        this.buildOrigin = origin;
        this.state = BuildState.CALCULATING;

        this.taskList = BuildOrderCalculator.calculateBuildOrder(schematic, origin);
        this.taskIndex = 0;
        this.placementCooldown = 0;
        this.startTime = System.currentTimeMillis();

        PlacementTracker.getInstance().reset();
        ObstacleRemovalManager.getInstance().reset();

        if (taskList.isEmpty()) {
            this.state = BuildState.COMPLETED;
            notifyPlayer(TextConstants.info("No blocks to construct."));
            return;
        }

        this.state = BuildState.PLACING;
        notifyPlayer(TextConstants.success("Construction started for " + taskList.size() + " blocks."));
    }

    public void pauseBuild() {
        if (isBuilding()) {
            this.state = BuildState.PAUSED_MANUAL;
            CameraController.getInstance().reset();
            ObstacleRemovalManager.getInstance().reset();
            notifyPlayer(TextConstants.warning("Construction paused."));
        }
    }

    public void resumeBuild() {
        if (state == BuildState.PAUSED_MANUAL || state == BuildState.PAUSED_MISSING_MATERIALS) {
            this.state = BuildState.PLACING;
            notifyPlayer(TextConstants.success("Construction resumed."));
        }
    }

    public void stopBuild() {
        this.state = BuildState.CANCELLED;
        CameraController.getInstance().reset();
        ObstacleRemovalManager.getInstance().reset();
        PlacementTracker.getInstance().reset();
        notifyPlayer(TextConstants.error("Construction stopped."));
    }

    public void tick() {
        if (!isBuilding()) {
            return;
        }

        PlacementTracker.getInstance().tick();

        if (state == BuildState.PAUSED_MANUAL || state == BuildState.PAUSED_MISSING_MATERIALS) {
            ModConfig config = ConfigManager.get();
            if (state == BuildState.PAUSED_MISSING_MATERIALS && config.autoResume) {
                BuildTask current = getCurrentTask();
                if (current != null && dev.autobuilder.interaction.InventoryHelper.hasItemForBlock(current.getDesiredState().getBlock())) {
                    resumeBuild();
                }
            }
            return;
        }

        if (taskIndex >= taskList.size()) {
            this.state = BuildState.COMPLETED;
            CameraController.getInstance().reset();
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            notifyPlayer(TextConstants.success("Construction completed in " + duration + "s!"));
            return;
        }

        BuildTask task = taskList.get(taskIndex);
        PlacementValidator.ValidationResult validation = PlacementValidator.validate(task);

        switch (validation) {
            case ALREADY_PLACED -> {
                taskIndex++;
                return;
            }
            case NEEDS_OBSTACLE_REMOVAL -> {
                this.state = BuildState.MINING_OBSTACLE;
                boolean cleared = ObstacleRemovalManager.getInstance().tickMining(task.getWorldPos());
                if (cleared) {
                    this.state = BuildState.PLACING;
                }
                return;
            }
            case OBSTACLE_FORBIDDEN -> {
                notifyPlayer(TextConstants.error("Unbreakable or protected obstacle at " + task.getWorldPos().toShortString() + "! Pausing."));
                pauseBuild();
                return;
            }
            case MISSING_MATERIAL -> {
                ModConfig config = ConfigManager.get();
                if (config.skipUnavailable) {
                    taskIndex++;
                } else if (config.pauseOnMissing) {
                    this.state = BuildState.PAUSED_MISSING_MATERIALS;
                    notifyPlayer(TextConstants.warning("Missing material: " + BlockUtils.getBlockName(task.getDesiredState()) + ". Pausing."));
                }
                return;
            }
            case MISSING_SUPPORT, OUT_OF_REACH -> {
                // Try next task in current layer if reachable, or wait for player to move closer
                return;
            }
            case VALID -> {
                // Valid for placement
            }
        }

        if (placementCooldown > 0) {
            placementCooldown--;
            return;
        }

        this.state = BuildState.PLACING;
        boolean placed = InteractionController.getInstance().placeBlock(task.getWorldPos(), task.getDesiredState());
        if (placed) {
            placementCooldown = ConfigManager.get().placementDelayTicks;
            taskIndex++;
        }
    }

    private void notifyPlayer(net.minecraft.text.Text message) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(message, false);
        }
    }
}

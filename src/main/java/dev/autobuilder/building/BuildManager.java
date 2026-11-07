package dev.autobuilder.building;

import dev.autobuilder.camera.CameraController;
import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.config.ModConfig;
import dev.autobuilder.interaction.ClimbController;
import dev.autobuilder.interaction.InteractionController;
import dev.autobuilder.interaction.InventoryHelper;
import dev.autobuilder.network.PlacementTracker;
import dev.autobuilder.obstacle.ObstacleRemovalManager;
import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.util.BlockUtils;
import dev.autobuilder.util.TextConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
    private int saveCooldown = 0;

    // Rate calculation for ETA and BPS
    private final Deque<Long> placementHistory = new ArrayDeque<>();

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

    public BlockPos getRecommendedStandPos() {
        BuildTask current = getCurrentTask();
        return current != null ? current.getRecommendedStandPos() : null;
    }

    public float getBlocksPerSecond() {
        long now = System.currentTimeMillis();
        while (!placementHistory.isEmpty() && now - placementHistory.peekFirst() > 5000) {
            placementHistory.pollFirst();
        }
        if (placementHistory.isEmpty()) return 0.0f;
        long duration = Math.max(1000, now - placementHistory.peekFirst());
        return (float) placementHistory.size() / (duration / 1000.0f);
    }

    public int getEstimatedSecondsRemaining() {
        float bps = getBlocksPerSecond();
        if (bps <= 0.05f) return -1;
        int remaining = Math.max(0, taskList.size() - taskIndex);
        return (int) (remaining / bps);
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
        this.placementHistory.clear();

        PlacementTracker.getInstance().reset();
        ObstacleRemovalManager.getInstance().reset();
        ClimbController.getInstance().clearClimbLog();

        if (taskList.isEmpty()) {
            this.state = BuildState.COMPLETED;
            notifyPlayer(TextConstants.info("No blocks to construct."));
            return;
        }

        // U5: Check for saved progress to resume seamlessly
        ModConfig config = ConfigManager.get();
        if (config.saveProgress) {
            BuildProgressSaver.ProgressData saved = BuildProgressSaver.load(schematic.getName());
            if (saved != null && saved.originX == origin.getX() && saved.originY == origin.getY() && saved.originZ == origin.getZ()) {
                if (saved.taskIndex > 0 && saved.taskIndex < taskList.size()) {
                    this.taskIndex = saved.taskIndex;
                    if (saved.climbLog != null) {
                        List<BlockPos> restored = new ArrayList<>();
                        for (int[] pos : saved.climbLog) {
                            if (pos != null && pos.length >= 3) {
                                restored.add(new BlockPos(pos[0], pos[1], pos[2]));
                            }
                        }
                        ClimbController.getInstance().setClimbLog(restored);
                    }
                    notifyPlayer(TextConstants.success("Resumed saved progress at block " + taskIndex + " / " + taskList.size()));
                }
            }
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
        if (state == BuildState.PAUSED_MANUAL || state == BuildState.PAUSED_MISSING_MATERIALS || state == BuildState.PAUSED_CHUNK_UNLOADED || state == BuildState.PAUSED_OUT_OF_REACH) {
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

    public void triggerCleanup() {
        if (!ClimbController.getInstance().getClimbLog().isEmpty()) {
            this.state = BuildState.CLEANING_CLIMB;
            notifyPlayer(TextConstants.info("Starting cleanup of " + ClimbController.getInstance().getClimbLog().size() + " climbing blocks..."));
        } else {
            notifyPlayer(TextConstants.info("No climbing blocks to clean."));
        }
    }

    public void tick() {
        if (!isBuilding()) {
            return;
        }

        PlacementTracker.getInstance().tick();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        ModConfig config = ConfigManager.get();

        if (world == null || player == null) {
            return;
        }

        // Periodic progress save
        if (config.saveProgress && activeSchematic != null && buildOrigin != null) {
            if (++saveCooldown >= 40) {
                saveCooldown = 0;
                BuildProgressSaver.saveAsync(activeSchematic.getName(), buildOrigin, taskIndex, taskList.size(), ClimbController.getInstance().getClimbLog());
            }
        }

        // Handle climb cleanup state
        if (state == BuildState.CLEANING_CLIMB) {
            boolean done = ClimbController.getInstance().tickCleanup();
            if (done) {
                this.state = BuildState.COMPLETED;
                if (activeSchematic != null) {
                    BuildProgressSaver.clear(activeSchematic.getName());
                }
                notifyPlayer(TextConstants.success("Climbing structure removed. Construction fully complete!"));
            }
            return;
        }

        // Handle paused states with auto-resume checks
        if (state == BuildState.PAUSED_CHUNK_UNLOADED) {
            BuildTask current = getCurrentTask();
            if (current != null && world.isChunkLoaded(current.getWorldPos().getX() >> 4, current.getWorldPos().getZ() >> 4)) {
                resumeBuild();
            }
            return;
        }

        if (state == BuildState.PAUSED_MISSING_MATERIALS && config.autoResume) {
            BuildTask current = getCurrentTask();
            if (current != null && InventoryHelper.hasItemForBlock(current.getDesiredState().getBlock())) {
                resumeBuild();
            }
            return;
        }

        if (state == BuildState.PAUSED_MANUAL) {
            return;
        }

        // Check if all blocks are placed
        if (taskIndex >= taskList.size()) {
            if (config.removeClimbBlocks && !ClimbController.getInstance().getClimbLog().isEmpty()) {
                this.state = BuildState.CLEANING_CLIMB;
                notifyPlayer(TextConstants.info("All schematic blocks placed. Cleaning climbing blocks..."));
                return;
            }

            this.state = BuildState.COMPLETED;
            CameraController.getInstance().reset();
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            if (activeSchematic != null) {
                BuildProgressSaver.clear(activeSchematic.getName());
            }
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
            case CHUNK_NOT_LOADED -> {
                if (config.pauseOnUnloadedChunk) {
                    this.state = BuildState.PAUSED_CHUNK_UNLOADED;
                    notifyPlayer(TextConstants.warning("Target chunk [" + (task.getWorldPos().getX() >> 4) + ", " + (task.getWorldPos().getZ() >> 4) + "] is not loaded. Pausing."));
                }
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
                if (config.skipUnavailable) {
                    taskIndex++;
                } else if (config.pauseOnMissing) {
                    this.state = BuildState.PAUSED_MISSING_MATERIALS;
                    notifyPlayer(TextConstants.warning("Missing material: " + BlockUtils.getBlockName(task.getDesiredState()) + ". Pausing."));
                }
                return;
            }
            case OUT_OF_REACH -> {
                // U3: Check if climbing is needed because the target is above reach
                if (config.climbMode && ClimbController.getInstance().isClimbNeeded(task, player)) {
                    this.state = BuildState.CLIMBING;
                    boolean placedClimb = ClimbController.getInstance().tickClimb(task);
                    if (placedClimb) {
                        placementCooldown = config.placementDelayTicks;
                    }
                    return;
                }
                // Otherwise wait for player to reposition
                return;
            }
            case MISSING_SUPPORT -> {
                // Wait for lower blocks or support to finish
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
            placementCooldown = config.placementDelayTicks;
            placementHistory.addLast(System.currentTimeMillis());
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

package dev.autobuilder.config;

public class ModConfig {
    public float cameraRotationSpeed = 15.0f;
    public int placementDelayTicks = 3;
    public int miningDelayTicks = 4;
    public double maxBuildReach = 4.2;
    public boolean breakObstacles = true;
    public boolean breakContainers = false;
    public boolean pauseOnMissing = true;
    public boolean skipUnavailable = false;
    public boolean autoResume = true;
    public int maxSelectionBlocks = 500000;
    public boolean smoothCamera = true;
    public boolean layerByLayer = true;

    // Tall structure & climbing settings
    public boolean climbMode = true;
    public boolean autoJump = false;
    public String climbBlock = "minecraft:scaffolding";
    public boolean removeClimbBlocks = true;
    public boolean showStandPoints = true;
    public boolean pauseOnUnloadedChunk = true;
    public boolean saveProgress = true;

    public ModConfig() {}
}

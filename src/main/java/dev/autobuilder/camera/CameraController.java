package dev.autobuilder.camera;

import dev.autobuilder.config.ConfigManager;
import dev.autobuilder.config.ModConfig;
import dev.autobuilder.util.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class CameraController {
    private static final CameraController INSTANCE = new CameraController();

    private boolean active = false;
    private float targetYaw;
    private float targetPitch;
    private static final float ALIGNMENT_THRESHOLD = 3.0f;

    private CameraController() {}

    public static CameraController getInstance() {
        return INSTANCE;
    }

    public void setTarget(Vec3d targetPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || targetPos == null) {
            this.active = false;
            return;
        }

        Vec3d eyePos = player.getEyePos();
        float[] rotations = RotationMath.calculateRotation(eyePos, targetPos);
        this.targetYaw = rotations[0];
        this.targetPitch = rotations[1];
        this.active = true;
    }

    public void reset() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAligned() {
        if (!active) return true;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return false;

        float yawDiff = RotationMath.getAngleDifference(player.getYaw(), targetYaw);
        float pitchDiff = Math.abs(player.getPitch() - targetPitch);

        return yawDiff <= ALIGNMENT_THRESHOLD && pitchDiff <= ALIGNMENT_THRESHOLD;
    }

    public void tick() {
        if (!active) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            active = false;
            return;
        }

        ModConfig config = ConfigManager.get();
        if (!config.smoothCamera) {
            player.setYaw(targetYaw);
            player.setPitch(targetPitch);
            return;
        }

        float speed = config.cameraRotationSpeed;
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        float newYaw = MathUtils.approachDegrees(currentYaw, targetYaw, speed);
        float pitchDiff = targetPitch - currentPitch;
        float newPitch;
        if (Math.abs(pitchDiff) <= speed) {
            newPitch = targetPitch;
        } else {
            newPitch = currentPitch + Math.signum(pitchDiff) * speed;
        }

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }
}

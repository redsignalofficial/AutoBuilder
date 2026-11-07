package dev.autobuilder.building;

public enum BuildState {
    IDLE,
    CALCULATING,
    ROTATING,
    PLACING,
    MINING_OBSTACLE,
    WAITING_SERVER_ACK,
    PAUSED_MISSING_MATERIALS,
    PAUSED_MANUAL,
    COMPLETED,
    CANCELLED
}

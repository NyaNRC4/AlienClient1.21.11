package dev.luminous.api.events.impl;

public class SendMovementPacketsEvent {
    public static final SendMovementPacketsEvent instance = new SendMovementPacketsEvent();

    private float originalYaw;
    private float originalPitch;
    private float yaw;
    private float pitch;

    private SendMovementPacketsEvent() {
    }

    public static SendMovementPacketsEvent get(float yaw, float pitch) {
        SendMovementPacketsEvent.instance.originalYaw = yaw;
        SendMovementPacketsEvent.instance.originalPitch = pitch;
        SendMovementPacketsEvent.instance.yaw = yaw;
        SendMovementPacketsEvent.instance.pitch = pitch;
        return instance;
    }

    public boolean isModified() {
        return Float.compare(yaw, originalYaw) != 0 || Float.compare(pitch, originalPitch) != 0;
    }

    public float getOriginalYaw() {
        return originalYaw;
    }

    public float getOriginalPitch() {
        return originalPitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}

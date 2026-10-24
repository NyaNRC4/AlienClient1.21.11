package dev.luminous.api.events.impl;

import net.minecraft.entity.LivingEntity;

public class LerpToEvent {
    private LerpToEvent() {
    }

    private static final LerpToEvent instance = new LerpToEvent();

    private LivingEntity entity;
    private double x;
    private double y;
    private double z;
    private float yRot;
    private float xRot;
    private long lastLerp;

    public static LerpToEvent get(LivingEntity entity, double x, double y, double z, float yRot, float xRot, long lastLerp) {
        instance.entity = entity;
        instance.x = x;
        instance.y = y;
        instance.z = z;
        instance.yRot = yRot;
        instance.xRot = xRot;
        instance.lastLerp = lastLerp;
        return instance;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public float getYRot() {
        return yRot;
    }

    public float getXRot() {
        return xRot;
    }

    public long getLastLerp() {
        return lastLerp;
    }
}

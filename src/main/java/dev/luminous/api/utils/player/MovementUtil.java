package dev.luminous.api.utils.player;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.asm.accessors.IInput;
import dev.luminous.asm.accessors.IVec3d;
import dev.luminous.mod.modules.impl.movement.HoleSnap;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

public class MovementUtil
        implements Wrapper {
    public static boolean isMoving() {
        PlayerInput pi = MovementUtil.mc.player.input.playerInput;
        return pi.forward() || pi.backward() || pi.left() || pi.right() || HoleSnap.INSTANCE.isOn();
    }

    public static boolean isStatic() {
        return MovementUtil.mc.player.getVelocity().getX() == 0.0 && MovementUtil.mc.player.isOnGround() && MovementUtil.mc.player.getVelocity().getZ() == 0.0;
    }

    public static boolean isJumping() {
        return MovementUtil.mc.player.input.playerInput.jump();
    }

    public static float getMovementForward() {
        PlayerInput pi = MovementUtil.mc.player.input.playerInput;
        return (pi.forward() ? 1.0F : 0.0F) - (pi.backward() ? 1.0F : 0.0F);
    }

    public static float getMovementSideways() {
        PlayerInput pi = MovementUtil.mc.player.input.playerInput;
        return (pi.left() ? 1.0F : 0.0F) - (pi.right() ? 1.0F : 0.0F);
    }

    public static void setMovement(PlayerEntity player, float forward, float sideways) {
        Input input = ((ClientPlayerEntity)player).input;
        PlayerInput pi = input.playerInput;
        input.playerInput = new PlayerInput(forward > 0.0F, forward < 0.0F, sideways > 0.0F, sideways < 0.0F, pi.jump(), pi.sneak(), pi.sprint());
        setMovementVector(player, forward, sideways);
    }

    public static void setMovementVector(PlayerEntity player, float forward, float sideways) {
        Vec2f vector = new Vec2f(sideways, forward);
        if (vector.lengthSquared() > 0.0F) {
            vector = vector.normalize();
        }
        ((IInput)((ClientPlayerEntity)player).input).setMovementVector(vector);
    }

    public static void setSneaking(PlayerEntity player, boolean sneaking) {
        PlayerInput pi = ((ClientPlayerEntity)player).input.playerInput;
        ((ClientPlayerEntity)player).input.playerInput = new PlayerInput(pi.forward(), pi.backward(), pi.left(), pi.right(), pi.jump(), sneaking, pi.sprint());
    }

    public static void resetInput(PlayerEntity player) {
        ((ClientPlayerEntity)player).input.playerInput = PlayerInput.DEFAULT;
    }

    public static double getDistance2D() {
        double xDist = MovementUtil.mc.player.getX() - MovementUtil.mc.player.lastRenderX;
        double zDist = MovementUtil.mc.player.getZ() - MovementUtil.mc.player.lastRenderZ;
        return Math.sqrt(xDist * xDist + zDist * zDist);
    }

    public static double getJumpSpeed() {
        double defaultSpeed = 0.0;
        if (MovementUtil.mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            int amplifier = ((StatusEffectInstance)MovementUtil.mc.player.getActiveStatusEffects().get(StatusEffects.JUMP_BOOST)).getAmplifier();
            defaultSpeed += (double)(amplifier + 1) * 0.1;
        }
        return defaultSpeed;
    }

    public static double[] directionSpeed(double speed) {
        float forward = MovementUtil.getMovementForward();
        float side = MovementUtil.getMovementSideways();
        float yaw = MovementUtil.mc.player.lastYaw + (MovementUtil.mc.player.getYaw() - MovementUtil.mc.player.lastYaw) * mc.getRenderTickCounter().getTickProgress(true);
        return MovementUtil.directionSpeed(speed, yaw, forward, side);
    }

    public static double[] directionSpeed(double speed, float yaw) {
        float forward = MovementUtil.getMovementForward();
        float side = MovementUtil.getMovementSideways();
        return MovementUtil.directionSpeed(speed, yaw, forward, side);
    }

    private static double[] directionSpeed(double speed, float yaw, float forward, float side) {
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += (float)(forward > 0.0f ? -45 : 45);
            } else if (side < 0.0f) {
                yaw += (float)(forward > 0.0f ? 45 : -45);
            }
            side = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        double posX = (double)forward * speed * cos + (double)side * speed * sin;
        double posZ = (double)forward * speed * sin - (double)side * speed * cos;
        return new double[]{posX, posZ};
    }

    public static double getMotionX() {
        return MovementUtil.mc.player.getVelocity().x;
    }

    public static void setMotionX(double x) {
        ((IVec3d)MovementUtil.mc.player.getVelocity()).setX(x);
    }

    public static double getMotionY() {
        return MovementUtil.mc.player.getVelocity().y;
    }

    public static void setMotionY(double y) {
        ((IVec3d)MovementUtil.mc.player.getVelocity()).setY(y);
    }

    public static double getMotionZ() {
        return MovementUtil.mc.player.getVelocity().z;
    }

    public static void setMotionZ(double z) {
        ((IVec3d)MovementUtil.mc.player.getVelocity()).setZ(z);
    }

    public static double getSpeed(boolean slowness) {
        double defaultSpeed = 0.2873;
        return MovementUtil.getSpeed(slowness, defaultSpeed);
    }

    public static double getSpeed(boolean slowness, double defaultSpeed) {
        int amplifier;
        if (MovementUtil.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            amplifier = ((StatusEffectInstance)MovementUtil.mc.player.getActiveStatusEffects().get(StatusEffects.SPEED)).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (double)(amplifier + 1);
        }
        if (slowness && MovementUtil.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            amplifier = ((StatusEffectInstance)MovementUtil.mc.player.getActiveStatusEffects().get(StatusEffects.SLOWNESS)).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (double)(amplifier + 1);
        }
        if (MovementUtil.mc.player.isSneaking()) {
            defaultSpeed /= 5.0;
        }
        return defaultSpeed;
    }
}


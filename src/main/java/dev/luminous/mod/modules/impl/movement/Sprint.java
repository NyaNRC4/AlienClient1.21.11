package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.*;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Sprint extends Module {
    public static Sprint INSTANCE;
    public final EnumSetting<Mode> mode =
            add(new EnumSetting<>("Mode", Mode.Legit));
    public final BooleanSetting inWaterPause =
            add(new BooleanSetting("InWaterPause", true));
    public final BooleanSetting inWebPause =
            add(new BooleanSetting("InWebPause", true));
    public final BooleanSetting sneakingPause =
            add(new BooleanSetting("SneakingPause", false));
    public final BooleanSetting blindnessPause =
            add(new BooleanSetting("BlindnessPause", true));
    public final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", false));
    public final BooleanSetting lagPause =
            add(new BooleanSetting("LagPause", true));

    public Sprint() {
        super("Sprint", "Permanently keeps player in sprinting mode.", Category.Movement);
        setChinese("强制疾跑");
        INSTANCE = this;
    }

    public static float getSprintYaw(float yaw) {
        if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                yaw -= 45f;
            } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                yaw += 45f;
            }
        } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
            yaw += 180f;
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                yaw += 45f;
            } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                yaw -= 45f;
            }
        } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 90f;
        } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 90f;
        }
        return MathHelper.wrapDegrees(yaw);
    }

    @Override
    public String getInfo() {
        return mode.getValue().name();
    }

    boolean pause = false;
    private Vec3d jumpVelocity;
    private float jumpTargetYaw;
    private boolean jumpCorrection;

    @EventListener
    public void onPacket(PacketEvent.Receive event) {
        if (lagPause.getValue() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            pause = true;
        }
    }

    public boolean inWater() {
        return inWaterPause.getValue()
                && (mc.player.isTouchingWater()
                || mc.player.isInLava()
                || mc.player.isHoldingOntoLadder());
    }

    @EventListener
    public void onTickMovement(TickMovementEvent event) {
        if (mc.player == null || BaritoneUtil.isPathing()) {
            return;
        }

        if (mode.is(Mode.PressKey)) {
            mc.options.sprintKey.setPressed(true);
            return;
        }

        if (inWater()) {
            return;
        }

        if (!shouldSprint()) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            return;
        }

        if (!mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        } else if (mc.player.horizontalCollision && !mc.player.collidedSoftly) {
            resendStartSprinting();
        }
    }

    @EventListener
    public void onJump(JumpEvent event) {
        if (mc.player == null) {
            return;
        }

        if (event.isPost()) {
            if (!jumpCorrection) {
                return;
            }
            jumpCorrection = false;

            Vec3d before = jumpVelocity;
            Vec3d after = mc.player.getVelocity();
            if (after.y <= before.y + 1.0E-4) {
                return;
            }

            double sin = Math.sin(Math.toRadians(jumpTargetYaw));
            double cos = Math.cos(Math.toRadians(jumpTargetYaw));
            double actualX = after.x - before.x;
            double actualZ = after.z - before.z;
            mc.player.addVelocity(-sin * 0.2 - actualX, 0.0, cos * 0.2 - actualZ);
            return;
        }

        if (!mode.is(Mode.Rotation) || !canRotateMovement() || !mc.player.isSprinting()) {
            return;
        }

        this.jumpVelocity = mc.player.getVelocity();
        this.jumpTargetYaw = AntiCheat.INSTANCE.movementSync()
                ? RotationManager.fixYaw
                : getSprintYaw(mc.player.getYaw());
        this.jumpCorrection = true;
    }

    @EventListener
    public void tick(TickEvent event) {
        if (event.isPost()) {
            pause = false;
        }
    }

    @EventListener
    public void sprint(SprintEvent event) {
        if (BaritoneUtil.isPathing() || mode.is(Mode.PressKey)) {
            return;
        }
        if (event.getCurrent()) {
            return;
        }
        if (!mode.is(Mode.Rage)) {
            return;
        }
        if (inWater() || !shouldSprint()) {
            return;
        }

        event.cancel();
        event.setSprint(true);
        resendStartSprinting();
    }

    private void resendStartSprinting() {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
    }

    private boolean shouldSprint() {
        if (mc.player == null) {
            return false;
        }
        if (!(mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative())) {
            return false;
        }
        if (!MovementUtil.isMoving() || pause) {
            return false;
        }
        if (mc.player.isSneaking() && sneakingPause.getValue()) {
            return false;
        }
        if (Alien.PLAYER.isInWeb(mc.player) && inWebPause.getValue()) {
            return false;
        }
        if (mc.player.isUsingItem() && usingPause.getValue()) {
            return false;
        }
        if (mc.player.isRiding() || mc.player.isGliding()) {
            return false;
        }
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && blindnessPause.getValue()) {
            return false;
        }
        if (inWater()) {
            return false;
        }

        return switch (mode.getValue()) {
            case Legit -> mc.player.input.hasForwardMovement()
                    && (!mc.player.horizontalCollision || mc.player.collidedSoftly);
            case Rotation -> {
                boolean collideOk = !mc.player.horizontalCollision || mc.player.collidedSoftly;
                boolean groundOk = mc.player.isOnGround() || mc.player.isSprinting();
                if (AntiCheat.INSTANCE.movementSync()) {
                    yield mc.player.forwardSpeed > 0.0f && collideOk && groundOk;
                }
                yield (HoleSnap.INSTANCE.isOn()
                        || MathHelper.angleBetween(getSprintYaw(mc.player.getYaw()), Alien.ROTATION.rotationYaw) < 40)
                        && collideOk && groundOk;
            }
            case Rage -> true;
            case PressKey -> false;
        };
    }

    @EventListener(priority = EventPriority.LOW)
    public void rotate(UpdateRotateEvent event) {
        if (!canRotateMovement()) {
            return;
        }
        if (mode.is(Mode.Rotation) && !event.isModified()) {
            event.setYaw(getSprintYaw(mc.player.getYaw()));
        }
    }

    private boolean canRotateMovement() {
        return mc.player != null
                && !BaritoneUtil.isPathing()
                && (mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative())
                && MovementUtil.isMoving()
                && !Freecam.INSTANCE.isOn()
                && !mc.player.isGliding()
                && (!Alien.PLAYER.isInWeb(mc.player) || !inWebPause.getValue())
                && (!mc.player.isSneaking() || !sneakingPause.getValue())
                && !mc.player.isRiding()
                && (!mc.player.isUsingItem() || !usingPause.getValue())
                && !mc.player.isInFluid()
                && (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !blindnessPause.getValue());
    }

    public enum Mode {
        PressKey,
        Legit,
        Rage,
        Rotation
    }
}

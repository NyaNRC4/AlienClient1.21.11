package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.*;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;

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
            add(new BooleanSetting("BlindnessPause", false));
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

    @EventListener
    public void onPacket(PacketEvent.Receive event) {
        if (lagPause.getValue() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            pause = true;
        }
    }

    public boolean inWater() {
        return inWaterPause.getValue() && mc.player.isInFluid();
    }

    @EventListener
    public void onMove(TickMovementEvent event) {
        if (BaritoneUtil.isPathing()) return;
        if (mc.player == null) return;
        if (inWater()) {
            return;
        }
        if (mode.getValue() == Mode.PressKey) {
            mc.options.sprintKey.setPressed(true);
            return;
        }
        if (pause) {
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
            return;
        }
        if (!mc.player.isSprinting() && shouldSprint()) {
            mc.player.setSprinting(true);
        }
    }

    @EventListener
    public void onMoved(MovedEvent event) {
        if (BaritoneUtil.isPathing()) return;
        if (mc.player == null) return;
        if (inWater()) {
            return;
        }
        if (mode.getValue() != Mode.PressKey && mc.player.isSprinting() && !shouldSprint()) {
            mc.player.setSprinting(false);
        }
    }

    @EventListener
    public void tick(TickEvent event) {
        if (event.isPost()) {
            pause = false;
        }
    }

    @EventListener
    public void sprint(SprintEvent event) {
        if (BaritoneUtil.isPathing() || mode.is(Mode.PressKey)) return;
        if (inWater()) {
            return;
        }
        event.cancel();
        event.setSprint(shouldSprint());
    }

    private boolean shouldSprint() {
        if ((mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative())
                && MovementUtil.isMoving()
                && !pause
                && (!mc.player.isSneaking() || !sneakingPause.getValue())
                && (!Alien.PLAYER.isInWeb(mc.player) || !inWebPause.getValue())
                && (!mc.player.isUsingItem() || !usingPause.getValue())
                && !mc.player.isRiding()
                && (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !blindnessPause.getValue())) {
            switch (mode.getValue()) {
                case Legit -> {
                    return mc.player.input.hasForwardMovement()
                            && (!mc.player.horizontalCollision || mc.player.collidedSoftly);
                }
                case Rotation -> {
                    boolean collideOk = !mc.player.horizontalCollision || mc.player.collidedSoftly;
                    boolean groundOk = mc.player.isOnGround() || mc.player.isSprinting();
                    if (AntiCheat.INSTANCE.movementSync()) {
                        return Sprint.mc.player.forwardSpeed > 0.0f && collideOk && groundOk;
                    } else {
                        return (HoleSnap.INSTANCE.isOn() || MathHelper.angleBetween(getSprintYaw(mc.player.getYaw()), Alien.ROTATION.rotationYaw) < 40)
                                && collideOk && groundOk;
                    }
                }
                case Rage -> {
                    return true;
                }
            }
        }
        return false;
    }

    @EventListener(priority = EventPriority.LOW)
    public void rotate(UpdateRotateEvent event) {
        if (BaritoneUtil.isPathing()) return;
        if ((mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.isCreative())
                && MovementUtil.isMoving()
                && !Freecam.INSTANCE.isOn()
                && !mc.player.isGliding()
                && (!Alien.PLAYER.isInWeb(mc.player) || !inWebPause.getValue())
                && (!mc.player.isSneaking() || !sneakingPause.getValue())
                && !mc.player.isRiding()
                && (!mc.player.isUsingItem() || !usingPause.getValue())
                && !mc.player.isInFluid()
                && Freecam.INSTANCE.isOff()
                && (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !blindnessPause.getValue())) {
            if (mode.is(Mode.Rotation) && !event.isModified()) {
                event.setYaw(getSprintYaw(mc.player.getYaw()));
            }
        }
    }

    public enum Mode {
        PressKey,
        Legit,
        Rage,
        Rotation
    }
}

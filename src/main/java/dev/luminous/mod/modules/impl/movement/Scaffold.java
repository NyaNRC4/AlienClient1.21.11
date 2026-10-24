package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Scaffold extends Module {
    private static Vec3d lastVec3d;
    public final SliderSetting rotateTime = add(new SliderSetting("KeepRotate", 1000, 0, 3000, 10));
    private final BooleanSetting tower =
            add(new BooleanSetting("Tower", true));
    private final BooleanSetting packetPlace =
            add(new BooleanSetting("PacketPlace", false));
    private final BooleanSetting safeWalk =
            add(new BooleanSetting("SafeWalk", false));
    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true).setParent());
    private final BooleanSetting yawStep =
            add(new BooleanSetting("YawStep", false, () -> rotate.isOpen() && rotate.isOpen()).setParent());
    private final BooleanSetting whenElytra =
            add(new BooleanSetting("FallFlying", true, () -> rotate.isOpen() && yawStep.isOpen() && rotate.isOpen()));
    private final SliderSetting steps =
            add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && yawStep.isOpen()));
    private final BooleanSetting checkFov =
            add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen() && yawStep.isOpen()).setParent());
    private final SliderSetting fov =
            add(new SliderSetting("Fov", 20, 0, 360, 0.1, () -> checkFov.isOpen() && rotate.isOpen() && yawStep.isOpen()));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10, 0, 100, () -> rotate.isOpen() && yawStep.isOpen()));
    private final BooleanSetting render = add(new BooleanSetting("Render", true).setParent());
    public final ColorSetting color = add(new ColorSetting("Color", new Color(255, 255, 255, 100), render::isOpen));
    public final ColorSetting outlineColor = add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 100), render::isOpen));
    public final SliderSetting sliderSpeed = add(new SliderSetting("SliderSpeed", 0.2, 0.01, 1, 0.01, render::isOpen));
    private final BooleanSetting esp = add(new BooleanSetting("ESP", true, render::isOpen));
    private final BooleanSetting fill = add(new BooleanSetting("Fill", true, render::isOpen));
    private final BooleanSetting outline = add(new BooleanSetting("Box", true, render::isOpen));
    private final Timer timer = new Timer();
    private final Timer towerTimer = new Timer();
    private Vec3d vec;
    private BlockPos pos;

    public Scaffold() {
        super("Scaffold", Category.Movement);
        setChinese("自动搭路");
    }

    @EventListener(priority = EventPriority.LOW)
    public void onMove(MoveEvent event) {
        if (!safeWalk.getValue()) return;
        SafeWalk.INSTANCE.onMove(event);
    }

    @EventListener
    public void onRotation(RotationEvent event) {
        if (rotate.getValue() && !timer.passed(rotateTime.getValueInt()) && vec != null) {
            event.setTarget(vec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @Override
    public void onEnable() {
        lastVec3d = null;
        pos = null;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (render.getValue()) {
            if (esp.getValue()) {
                double temp = 0.01;
                for (double i = 0; i < 0.8; i += temp) {
                    HoleSnap.drawCircle(matrixStack, ColorUtil.injectAlpha(color.getValue(), (int) Math.min(color.getValue().getAlpha() * 2 / (0.8 / temp), 255)), i, new Vec3d(MathUtil.interpolate(mc.player.lastRenderX, mc.player.getX(), mc.getRenderTickCounter().getDynamicDeltaTicks()), MathUtil.interpolate(mc.player.lastRenderY, mc.player.getY(), mc.getRenderTickCounter().getDynamicDeltaTicks()), MathUtil.interpolate(mc.player.lastRenderZ, mc.player.getZ(), mc.getRenderTickCounter().getDynamicDeltaTicks())), 5);
                }
            }
            if (pos != null) {
                Vec3d cur = pos.toCenterPos();
                if (lastVec3d == null) {
                    lastVec3d = cur;
                } else {
                    lastVec3d = new Vec3d(AnimateUtil.animate(lastVec3d.getX(), cur.x, sliderSpeed.getValue()),
                            AnimateUtil.animate(lastVec3d.getY(), cur.y, sliderSpeed.getValue()),
                            AnimateUtil.animate(lastVec3d.getZ(), cur.z, sliderSpeed.getValue()));
                }
                Render3DUtil.draw3DBox(matrixStack, new Box(lastVec3d.add(0.5, 0.5, 0.5), lastVec3d.add(-0.5, -0.5, -0.5)), ColorUtil.injectAlpha(color.getValue(), color.getValue().getAlpha()), ColorUtil.injectAlpha(outlineColor.getValue(), outlineColor.getValue().getAlpha()), outline.getValue(), fill.getValue());
            }
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        int block = InventoryUtil.findBlock();
        if (block == -1) return;
        BlockPos placePos = mc.player.getBlockPos().down();
        if (BlockUtil.clientCanPlace(placePos, false)) {
            int old = mc.player.getInventory().selectedSlot;
            if (BlockUtil.getPlaceSide(placePos) == null) {
                double distance = 1000;
                BlockPos bestPos = null;
                for (Direction i : Direction.values()) {
                    if (i == Direction.UP) continue;
                    if (BlockUtil.canPlace(placePos.offset(i))) {
                        if (bestPos == null || mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos()) < distance) {
                            bestPos = placePos.offset(i);
                            distance = mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos());
                        }
                    }
                }
                if (bestPos != null) {
                    placePos = bestPos;
                } else {
                    return;
                }
            }
            if (rotate.getValue()) {
                Direction side = BlockUtil.getPlaceSide(placePos);
                vec = (placePos.offset(side).toCenterPos().add(side.getOpposite().getVector().getX() * 0.5, side.getOpposite().getVector().getY() * 0.5, side.getOpposite().getVector().getZ() * 0.5));
                timer.reset();
                if (!faceVector(vec)) return;
            }
            InventoryUtil.switchToSlot(block);
            BlockUtil.placeBlock(placePos, false, packetPlace.getValue());
            InventoryUtil.switchToSlot(old);
            if (rotate.getValue()) {
                Alien.ROTATION.snapBack();
            }
            pos = placePos;
            if (tower.getValue() && mc.options.jumpKey.isPressed() && !MovementUtil.isMoving()) {
                MovementUtil.setMotionY(0.42);
                MovementUtil.setMotionX(0);
                MovementUtil.setMotionZ(0);
                if (this.towerTimer.passed(1500L)) {
                    MovementUtil.setMotionY(-0.28);
                    this.towerTimer.reset();
                }
            } else {
                this.towerTimer.reset();
            }
        }
    }

    private boolean shouldYawStep() {
        if (!whenElytra.getValue() && (mc.player.isGliding() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isGliding()))
            return false;
        return yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!shouldYawStep()) {
            Alien.ROTATION.lookAt(directionVec);
            return true;
        } else {
            if (Alien.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }
}

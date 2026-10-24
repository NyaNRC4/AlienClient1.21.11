package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    public static Freecam INSTANCE;
    private final SliderSetting speed = add(new SliderSetting("HSpeed", 1, 0.0, 3));
    private final SliderSetting hspeed = add(new SliderSetting("VSpeed", 0.42, 0.0, 3));
    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true));
    private float fakeYaw;
    private float fakePitch;
    private float prevFakeYaw;
    private float prevFakePitch;

    private double fakeX;
    private double fakeY;
    private double fakeZ;
    private double prevFakeX;
    private double prevFakeY;
    private double prevFakeZ;

    private float playerYaw;
    private float playerPitch;

    public Freecam() {
        super("Freecam", Category.Player);
        setChinese("自由相机");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        playerYaw = getYaw();
        playerPitch = getPitch();

        fakePitch = getPitch();
        fakeYaw = getYaw();

        prevFakePitch = fakePitch;
        prevFakeYaw = fakeYaw;

        fakeX = mc.player.getX();
        fakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        fakeZ = mc.player.getZ();

        prevFakeX = fakeX;
        prevFakeY = fakeY;
        prevFakeZ = fakeZ;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (rotate.getValue() && mc.crosshairTarget != null && mc.crosshairTarget.getPos() != null) {
            float[] angle = RotationManager.getRotation(
                    new Vec3d(fakeX, fakeY, fakeZ),
                    mc.crosshairTarget.getPos()
            );
            playerYaw = angle[0];
            playerPitch = angle[1];
        }
        if (BaritoneUtil.isPathing()) {
            double[] motion = MovementUtil.directionSpeed(speed.getValue());

            prevFakeX = fakeX;
            prevFakeY = fakeY;
            prevFakeZ = fakeZ;

            fakeX += motion[0];
            fakeZ += motion[1];

            if (mc.options.jumpKey.isPressed())
                fakeY += hspeed.getValue();

            if (mc.options.sneakKey.isPressed())
                fakeY -= hspeed.getValue();
        }
    }

    @EventListener(priority = EventPriority.HIGHEST)
    public void onRotate(UpdateRotateEvent event) {
        if (BaritoneUtil.isPathing()) return;
        if (event.isModified()) return;
        event.setYawWithoutSync(playerYaw);
        event.setPitchWithoutSync(playerPitch);
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        prevFakeYaw = fakeYaw;
        prevFakePitch = fakePitch;

        fakeYaw = getYaw();
        fakePitch = getPitch();
    }

    private float getYaw() {
        return mc.player.getYaw();
    }

    private float getPitch() {
        return mc.player.getPitch();
    }

    @EventListener
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (Freecam.mc.player == null) {
            return;
        }
        double[] motion = MovementUtil.directionSpeed(this.speed.getValue());
        this.prevFakeX = this.fakeX;
        this.prevFakeY = this.fakeY;
        this.prevFakeZ = this.fakeZ;
        this.fakeX += motion[0];
        this.fakeZ += motion[1];
        if (Freecam.mc.options.jumpKey.isPressed()) {
            this.fakeY += this.hspeed.getValue();
        }
        if (Freecam.mc.options.sneakKey.isPressed()) {
            this.fakeY -= this.hspeed.getValue();
        }
        MovementUtil.resetInput(Freecam.mc.player);



    }

    public float getFakeYaw() {
        return MathUtil.interpolate(this.prevFakeYaw, this.fakeYaw, mc.getRenderTickCounter().getTickProgress(true));
    }

    public float getFakePitch() {
        return MathUtil.interpolate(this.prevFakePitch, this.fakePitch, mc.getRenderTickCounter().getTickProgress(true));
    }

    public double getFakeX() {
        return MathUtil.interpolate(this.prevFakeX, this.fakeX, (double)mc.getRenderTickCounter().getTickProgress(true));
    }

    public double getFakeY() {
        return MathUtil.interpolate(this.prevFakeY, this.fakeY, (double)mc.getRenderTickCounter().getTickProgress(true));
    }

    public double getFakeZ() {
        return MathUtil.interpolate(this.prevFakeZ, this.fakeZ, (double)mc.getRenderTickCounter().getTickProgress(true));
    }
}

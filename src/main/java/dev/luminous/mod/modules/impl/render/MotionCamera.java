package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class MotionCamera extends Module {
    public static MotionCamera INSTANCE;

    public final BooleanSetting noFirstPerson = add(new BooleanSetting("NoFirstPerson", true));
    public final SliderSetting firstPersonSpeed = add(new SliderSetting("FirstPersonSpeed", 0.6, 0, 1, 0.01));
    public final SliderSetting speed = add(new SliderSetting("Speed", 0.3, 0, 1, 0.01));

    public MotionCamera() {
        super("MotionCamera", Category.Render);
        INSTANCE = this;
        setChinese("运动相机");
    }

    private double fakeX;
    private double fakeY;
    private double fakeZ;
    private double prevFakeX;
    private double prevFakeY;
    private double prevFakeZ;

    public boolean on() {
        return isOn() && (!noFirstPerson.getValue() || !mc.options.getPerspective().isFirstPerson());
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        fakeX = mc.player.getX();
        fakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        fakeZ = mc.player.getZ();

        prevFakeX = fakeX;
        prevFakeY = fakeY;
        prevFakeZ = fakeZ;
    }

    @EventListener
    public void onUpdate(ClientTickEvent event) {
        if (event.isPre() || nullCheck()) return;
        prevFakeX = fakeX;
        prevFakeY = fakeY;
        prevFakeZ = fakeZ;

        double speed = mc.options.getPerspective().isFirstPerson() ? firstPersonSpeed.getValue() : this.speed.getValue();
        fakeX = AnimateUtil.animate(fakeX, mc.player.getX(), speed);
        fakeY = AnimateUtil.animate(fakeY, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), speed);
        fakeZ = AnimateUtil.animate(fakeZ, mc.player.getZ(), speed);
    }

    public double getFakeX() {
        return MathUtil.interpolate(prevFakeX, fakeX, mc.getRenderTickCounter().getTickProgress(true));
    }

    public double getFakeY() {
        return MathUtil.interpolate(prevFakeY, fakeY, mc.getRenderTickCounter().getTickProgress(true));
    }

    public double getFakeZ() {
        return MathUtil.interpolate(prevFakeZ, fakeZ, mc.getRenderTickCounter().getTickProgress(true));
    }
}

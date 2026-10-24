package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class FastSwim
extends Module {
    public static FastSwim INSTANCE;
    public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.2, 0.0, 1.0, 0.01));
    public final SliderSetting downFactor = this.add(new SliderSetting("DownFactor", 0.0, 0.0, 1.0, 1.0E-6));
    private final SliderSetting sneakDownSpeed = this.add(new SliderSetting("DownSpeed", 0.2, 0.0, 1.0, 0.01));
    private final SliderSetting upSpeed = this.add(new SliderSetting("UpSpeed", 0.2, 0.0, 1.0, 0.01));
    private MoveEvent event;

    public FastSwim() {
        super("FastSwim", Module.Category.Movement);
        this.setChinese("快速游泳");
        INSTANCE = this;
    }

    @EventListener
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (FastSwim.mc.player.isInFluid()) {
            MovementUtil.setSneaking(FastSwim.mc.player, false);
        }
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (FastSwim.nullCheck()) {
            return;
        }
        if (FastSwim.mc.player.isInFluid()) {
            this.event = event;
            if (!FastSwim.mc.options.sneakKey.isPressed() || !FastSwim.mc.player.input.playerInput.jump()) {
                if (FastSwim.mc.options.sneakKey.isPressed()) {
                    this.setY(-this.sneakDownSpeed.getValue());
                } else if (FastSwim.mc.player.input.playerInput.jump()) {
                    this.setY(this.upSpeed.getValue());
                } else {
                    this.setY(-this.downFactor.getValue());
                }
            } else {
                this.setY(0.0);
            }
            double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
            this.setX(dir[0]);
            this.setZ(dir[1]);
        }
    }

    private void setX(double f) {
        this.event.setX(f);
        MovementUtil.setMotionX(f);
    }

    private void setY(double f) {
        this.event.setY(f);
        MovementUtil.setMotionY(f);
    }

    private void setZ(double f) {
        this.event.setZ(f);
        MovementUtil.setMotionZ(f);
    }
}


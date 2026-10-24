package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Fly extends Module {
    public static Fly INSTANCE;
    private final SliderSetting speedConfig = add(new SliderSetting("Speed", 2.5, 0.1, 10.0));
    private final SliderSetting vspeedConfig = add(new SliderSetting("VerticalSpeed", 1.0, 0.1, 5.0));
    private final BooleanSetting antiKickConfig = add(new BooleanSetting("AntiKick", true).setParent());
    private final BooleanSetting up = add(new BooleanSetting("Up", true, antiKickConfig::isOpen));
    private final BooleanSetting allowSneak = add(new BooleanSetting("AllowSneak", false));

    private final Timer antiKickTimer = new Timer();
    private final Timer antiKick2Timer = new Timer();

    public Fly() {
        super("Fly", Category.Movement);
        setChinese("飞行");
        INSTANCE = this;
    }

    @Override
    public void onLogin() {
        antiKickTimer.reset();
        antiKick2Timer.reset();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        antiKickTimer.reset();
        antiKick2Timer.reset();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (antiKickTimer.passed(3900) && antiKickConfig.getValue() && !mc.player.isOnGround()) {
            MovementUtil.setMotionY(-0.04);
            antiKickTimer.reset();
        } else if (antiKick2Timer.passed(4000) && antiKickConfig.getValue() && !mc.player.isOnGround() && up.getValue()) {
            MovementUtil.setMotionY(0.04);
            antiKick2Timer.reset();
        } else {
            MovementUtil.setMotionY(0.0);
            if (mc.options.jumpKey.isPressed()) {
                MovementUtil.setMotionY(vspeedConfig.getValue());
            } else if (mc.options.sneakKey.isPressed()) {
                MovementUtil.setMotionY(-vspeedConfig.getValue());
            }
        }
        double[] move = MovementUtil.directionSpeed(speedConfig.getValueFloat());
        MovementUtil.setMotionX(move[0]);
        MovementUtil.setMotionZ(move[1]);
    }

    @EventListener(priority = EventPriority.LOW)
    public void keyboard(KeyboardInputEvent event) {
        if (!this.allowSneak.getValue()) {
            MovementUtil.setSneaking(Fly.mc.player, false);
        }
    }
}

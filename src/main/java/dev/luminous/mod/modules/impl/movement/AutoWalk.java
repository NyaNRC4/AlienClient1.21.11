package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;

public class AutoWalk extends Module {
    public static AutoWalk INSTANCE;
    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Forward));
    boolean start = false;

    public AutoWalk() {
        super("AutoWalk", Category.Movement);
        setChinese("自动前进");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        start = false;
    }

    @Override
    public void onLogout() {
        disable();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mode.is(Mode.Forward)) {
            mc.options.forwardKey.setPressed(true);
        } else if (mode.is(Mode.Path)) {
            if (!start) {
                BaritoneUtil.forward();
                start = true;
            } else if (!BaritoneUtil.isActive()) {
                disable();
            }
        }
    }

    @Override
    public void onDisable() {
        BaritoneUtil.cancelEverything();
    }

    public boolean forward() {
        return isOn() && mode.is(Mode.Forward);
    }

    public enum Mode {
        Forward,
        Path
    }
}

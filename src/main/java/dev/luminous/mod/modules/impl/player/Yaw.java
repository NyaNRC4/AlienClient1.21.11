package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.LookDirectionEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Yaw extends Module {

    public static Yaw INSTANCE;
    private final BooleanSetting yawLock =
            add(new BooleanSetting("YawLock", true));
    private final BooleanSetting smart =
            add(new BooleanSetting("Smart", true));
    private final SliderSetting yaw =
            add(new SliderSetting("Yaw", 0, -180, 180, .1, () -> !smart.getValue()));
    private final BooleanSetting pitchLock =
            add(new BooleanSetting("PitchLock", true));
    private final SliderSetting pitch =
            add(new SliderSetting("Pitch", 0, -90, 90, .1));
    private final BooleanSetting lock =
            add(new BooleanSetting("Lock", true));

    public Yaw() {
        super("Yaw", Category.Player);
        setChinese("视角锁定");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (yawLock.getValue())
            mc.player.setYaw(smart.getValue() ? Math.round((mc.player.getYaw() + 1f) / 45f) * 45f : yaw.getValueFloat());
        if (pitchLock.getValue()) mc.player.setPitch(pitch.getValueFloat());
    }

    @EventListener
    public void onLookDirection(LookDirectionEvent event) {
        if (lock.getValue()) {
            event.cancel();
        }
    }
}

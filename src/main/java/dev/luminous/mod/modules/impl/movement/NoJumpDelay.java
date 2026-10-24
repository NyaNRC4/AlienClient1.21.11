package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;

public class NoJumpDelay
        extends Module {
    public static NoJumpDelay INSTANCE;

    public NoJumpDelay() {
        super("NoJumpDelay", Category.Movement);
        setChinese("无跳跃冷却");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        ((ILivingEntity) mc.player).setLastJumpCooldown(0);
    }
}

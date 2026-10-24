package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;

public class MovementSync extends Module {
    public static MovementSync INSTANCE;

    public MovementSync() {
        super("MovementSync", Category.Movement);
        setChinese("移动同步");
        INSTANCE = this;
    }

    @EventListener(priority=100)
    public void keyboard(KeyboardInputEvent event) {
        mc.options.jumpKey.setPressed(ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode()));
        mc.options.forwardKey.setPressed(AutoWalk.INSTANCE.forward() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()));
        boolean pf = mc.options.forwardKey.isPressed();
        boolean pb = mc.options.backKey.isPressed();
        boolean pl = mc.options.leftKey.isPressed();
        boolean pr = mc.options.rightKey.isPressed();
        mc.player.input.playerInput = new PlayerInput(
                pf && !pb,
                pb && !pf,
                pl && !pr,
                pr && !pl,
                mc.options.jumpKey.isPressed(),
                mc.options.sneakKey.isPressed(),
                mc.options.sprintKey.isPressed()
        );
    }
}

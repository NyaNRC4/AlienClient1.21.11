package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.interfaces.IMouseHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Mouse.class})
public class MixinMouse
        implements IMouseHook {
    @Shadow
    private boolean cursorLocked;
    @Final
    @Shadow
    private MinecraftClient client;
    @Shadow
    private double x;
    @Shadow
    private double y;

    @Inject(method={"onMouseButton"}, at={@At(value="HEAD")})
    private void onMouse(long window, MouseInput input, int action, CallbackInfo ci) {
        int key = -(input.button() + 2);
        if (action == 1) {
            Alien.MODULE.onKeyPressed(key);
        }
        if (action == 0) {
            Alien.MODULE.onKeyReleased(key);
        }
    }

    @Override
    public void alienClient$lock() {
        if (this.client.isWindowFocused() && !this.cursorLocked) {
            if (true) {
                KeyBinding.updatePressedStates();
            }
            this.cursorLocked = true;
            this.x = (double)this.client.getWindow().getWidth() / 2.0;
            this.y = (double)this.client.getWindow().getHeight() / 2.0;
            InputUtil.setCursorParameters(this.client.getWindow(), InputUtil.GLFW_CURSOR_DISABLED, (double)this.x, (double)this.y);
        }
    }
}

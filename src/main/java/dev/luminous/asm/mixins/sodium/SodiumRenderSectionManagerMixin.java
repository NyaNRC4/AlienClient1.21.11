package dev.luminous.asm.mixins.sodium;

import dev.luminous.mod.modules.impl.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class SodiumRenderSectionManagerMixin {

    @Inject(method = "shouldUseOcclusionCulling", at = @At("HEAD"), cancellable = true)
    private void onShouldUseOcclusionCulling(Camera camera, boolean spectator, CallbackInfoReturnable<Boolean> info) {
        if (Xray.INSTANCE.isOn()) {
            info.setReturnValue(false);
        }
    }
}

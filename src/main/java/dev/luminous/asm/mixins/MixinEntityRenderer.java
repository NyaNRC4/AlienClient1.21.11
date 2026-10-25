package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NameTags;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = {"updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V"},
            at = @At(value = "TAIL"))
    private void updateRenderState(Entity entity, EntityRenderState entityRenderState, float partialTicks, CallbackInfo ci) {
        if (NameTags.INSTANCE.isOn() && entity instanceof PlayerEntity) {
            entityRenderState.displayName = null;
        }
    }

    @Inject(method = {"hasLabel(Lnet/minecraft/entity/Entity;D)Z"},
            at = @At(value = "HEAD"), cancellable = true)
    private void hasLabel(Entity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (NameTags.INSTANCE.isOn() && entity instanceof PlayerEntity) {
            cir.setReturnValue(false);
        }
    }
}

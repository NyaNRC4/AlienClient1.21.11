package dev.luminous.asm.mixins;

import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Unique
    private boolean alien$rotating;
    @Unique
    private float alien$originalYaw;
    @Unique
    private float alien$originalHeadYaw;
    @Unique
    private float alien$originalBodyYaw;
    @Unique
    private float alien$originalPitch;
    @Unique
    private float alien$originalLastYaw;
    @Unique
    private float alien$originalLastHeadYaw;
    @Unique
    private float alien$originalLastBodyYaw;
    @Unique
    private float alien$originalLastPitch;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    private void alien$beforeUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickProgress, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        this.alien$rotating = mc.player != null && entity == mc.player && ClientSetting.INSTANCE.rotations.getValue();
        if (!this.alien$rotating) {
            return;
        }

        this.alien$originalYaw = entity.getYaw();
        this.alien$originalHeadYaw = entity.headYaw;
        this.alien$originalBodyYaw = entity.bodyYaw;
        this.alien$originalPitch = entity.getPitch();
        this.alien$originalLastYaw = entity.lastYaw;
        this.alien$originalLastHeadYaw = entity.lastHeadYaw;
        this.alien$originalLastBodyYaw = entity.lastBodyYaw;
        this.alien$originalLastPitch = entity.lastPitch;

        entity.setYaw(RotationManager.getRenderYawOffset());
        entity.headYaw = RotationManager.getRotationYawHead();
        entity.bodyYaw = RotationManager.getRenderYawOffset();
        entity.setPitch(RotationManager.getRenderPitch());
        entity.lastYaw = RotationManager.getPrevRenderYawOffset();
        entity.lastHeadYaw = RotationManager.getPrevRotationYawHead();
        entity.lastBodyYaw = RotationManager.getPrevRenderYawOffset();
        entity.lastPitch = RotationManager.getPrevRenderPitch();
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void alien$afterUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (!this.alien$rotating) {
            return;
        }
        this.alien$rotating = false;

        entity.setYaw(this.alien$originalYaw);
        entity.headYaw = this.alien$originalHeadYaw;
        entity.bodyYaw = this.alien$originalBodyYaw;
        entity.setPitch(this.alien$originalPitch);
        entity.lastYaw = this.alien$originalLastYaw;
        entity.lastHeadYaw = this.alien$originalLastHeadYaw;
        entity.lastBodyYaw = this.alien$originalLastBodyYaw;
        entity.lastPitch = this.alien$originalLastPitch;
    }
}

package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.LightmapGammaEvent;
import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager {
    @Shadow private boolean dirty;
    @Shadow private float flickerIntensity;
    @Final @Shadow private GameRenderer renderer;
    @Final @Shadow private MinecraftClient client;

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/LightmapTextureManager;getDarkness(Lnet/minecraft/entity/LivingEntity;FF)F"),
            cancellable = true)
    private void hookNoRenderDarkness(float tickProgress, CallbackInfo ci) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.darkness.getValue()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 4)
    private float modifyDarknessFactor(float value) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.darkness.getValue()) {
            return 0.0F;
        }
        return value;
    }

    @ModifyVariable(method = "update", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private Vector3f modifyWorldColorVector(Vector3f vector3f) {
        if (Ambience.INSTANCE.isOn()) {
            int argb = Ambience.INSTANCE.worldColor.getValue().getRGB();
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;
            return new Vector3f(r,g,b);
        }
        return vector3f;
    }

    @ModifyArgs(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 6
            )
    )
    private void hookLightmapGamma(Args args) {
        float originalGamma = args.get(0);
        LightmapGammaEvent event = new LightmapGammaEvent(originalGamma);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            args.set(0, event.getGamma());
        }
    }

    @Shadow
    private float getDarkness(LivingEntity entity, float factor, float tickProgress) {
        return 0;
    }

    @Shadow
    public static float getBrightness(float ambientLight, int lightLevel) {
        return 0;
    }
}

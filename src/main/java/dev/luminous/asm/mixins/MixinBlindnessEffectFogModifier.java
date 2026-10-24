package dev.luminous.asm.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.luminous.mod.modules.impl.render.NoRender;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {BlindnessEffectFogModifier.class})
public class MixinBlindnessEffectFogModifier {

    @Inject(
            method = {"applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V"},
            at = {@At(value = "TAIL")})
    private void removeBlindnessFog(FogData fogData, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.blindness.getValue()) {
            float far = 1.0E9f;
            fogData.environmentalStart = far;
            fogData.environmentalEnd = far;
            fogData.renderDistanceStart = far;
            fogData.renderDistanceEnd = far;
            fogData.skyEnd = far;
            fogData.cloudEnd = far;
        }
    }
}

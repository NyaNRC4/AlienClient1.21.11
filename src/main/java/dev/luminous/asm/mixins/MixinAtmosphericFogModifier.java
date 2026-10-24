package dev.luminous.asm.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.Xray;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {AtmosphericFogModifier.class})
public class MixinAtmosphericFogModifier {

    private static final float SAFE_FAR = 102400F;

    @Inject(
            method = {"applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V"},
            at = {@At(value = "TAIL")}
    )
    private void applyFogSettings(FogData fogData, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        if ((NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fog.getValue()) || Xray.INSTANCE.isOn()) {
            fogData.environmentalStart = 0.0f;
            fogData.environmentalEnd = SAFE_FAR;

            fogData.renderDistanceStart = 0.0f;
            fogData.renderDistanceEnd = SAFE_FAR;

            fogData.skyEnd = SAFE_FAR;
            fogData.cloudEnd = SAFE_FAR;
            return;
        }

        Ambience ambience = Ambience.INSTANCE;
        if (ambience.isOn() && ambience.fogDistance.getValue()) {
            float customStart = ambience.fogStart.getValueFloat();
            float customEnd = ambience.fogEnd.getValueFloat();

            if (customStart >= customEnd || customEnd > SAFE_FAR) {
                customStart = 0f;
                customEnd = 128f;
            }
            fogData.environmentalStart = customStart;
            fogData.environmentalEnd = customEnd;
        }
    }
}

package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.LerpToEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PositionInterpolator.class)
public abstract class MixinPositionInterpolator {
    @Shadow
    private Entity entity;

    private long lastLerp = 0;

    @Inject(method = "refreshPositionAndAngles", at = @At("HEAD"))
    private void lerpToHook(Vec3d pow, float yaw, float pitch, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            Alien.EVENT_BUS.post(LerpToEvent.get(livingEntity, pow.x, pow.y, pow.z, yaw, pitch, lastLerp));
            lastLerp = System.currentTimeMillis();
        }
    }
}

package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.JumpEvent;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEntity.class)
public class MixinPlayerEntity implements Wrapper {

    @Inject(method = "canChangeIntoPose", at = @At("RETURN"), cancellable = true)
    private void poseNotCollide(EntityPose pose, CallbackInfoReturnable<Boolean> cir) {
        if (PlayerEntity.class.cast(this) == mc.player) {
            if (!ClientSetting.INSTANCE.crawl.getValue() && pose == EntityPose.SWIMMING) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
        if (InteractTweaks.INSTANCE.reach()) {
            cir.setReturnValue(InteractTweaks.INSTANCE.blockRange.getValue());
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
        if (InteractTweaks.INSTANCE.reach()) {
            cir.setReturnValue(InteractTweaks.INSTANCE.entityRange.getValue());
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = PlayerEntity.class.cast(this);
        if (player != mc.player)
            return;

        TravelEvent event = TravelEvent.get(Event.Stage.Pre, player);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
            event = TravelEvent.get(Event.Stage.Post, player);
            Alien.EVENT_BUS.post(event);
        }
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = PlayerEntity.class.cast(this);
        if (player != mc.player)
            return;

        TravelEvent event = TravelEvent.get(Event.Stage.Post, player);
        Alien.EVENT_BUS.post(event);
    }
}

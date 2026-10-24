package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.player.FreeLook;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.render.CameraClip;
import dev.luminous.mod.modules.impl.render.MotionCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={Camera.class})
public abstract class MixinCamera {
    @Shadow
    private boolean thirdPerson;

    @Shadow
    protected abstract float clipToSpace(float desiredCameraDistance);

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    private void modifyCameraDistance(Args args) {
        if (CameraClip.INSTANCE.isOn()) {
            args.set(0, -clipToSpace((float) CameraClip.INSTANCE.getDistance()));
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
        if (CameraClip.INSTANCE.isOn()) {
            cir.setReturnValue((float) CameraClip.INSTANCE.getDistance());
        }
    }

    @Inject(method={"update"}, at={@At(value="TAIL")})
    private void updateHook(net.minecraft.world.World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
            this.thirdPerson = true;
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void setRotationHook(Args args) {
        if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
            args.setAll(Freecam.INSTANCE.getFakeYaw(), Freecam.INSTANCE.getFakePitch());
        } else if (FreeLook.INSTANCE.isOn()) {
            args.setAll(FreeLook.INSTANCE.getFakeYaw(), FreeLook.INSTANCE.getFakePitch());
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void setPosHook(Args args) {
        if (Freecam.INSTANCE != null && Freecam.INSTANCE.isOn()) {
            args.setAll(Freecam.INSTANCE.getFakeX(), Freecam.INSTANCE.getFakeY(), Freecam.INSTANCE.getFakeZ());
        } else if (MotionCamera.INSTANCE.on()) {
            args.setAll(MotionCamera.INSTANCE.getFakeX(), MotionCamera.INSTANCE.getFakeY(), MotionCamera.INSTANCE.getFakeZ());
        }
    }
}


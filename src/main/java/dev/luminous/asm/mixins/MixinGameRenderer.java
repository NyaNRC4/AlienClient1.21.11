package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.mod.modules.impl.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    MinecraftClient client;
    @Shadow
    private float fovMultiplier;
    @Shadow
    private float lastFovMultiplier;
    private boolean renderingPanorama;

    @Unique
    private Entity cameraEntity;
    @Unique
    private float originalYaw;
    @Unique
    private float originalPitch;

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.totem.getValue()) {
            info.cancel();
        }
    }

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"), require = 0)
    private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.nausea.getValue()) return 0;
        return MathHelper.lerp(delta, first, second);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.hurtCam.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    public void hookOutline(CallbackInfoReturnable<Boolean> cir) {
        if (HighLight.INSTANCE.isOn()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("HEAD"), cancellable = true)
    public void getFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> ci) {
        if (this.renderingPanorama) {
            return;
        }
        if (!Fov.INSTANCE.isOn() && !Zoom.on) {
            return;
        }

        boolean usingItem = camera.getFocusedEntity() instanceof LivingEntity living && living.isUsingItem();

        if (Fov.INSTANCE.isOn()) {
            float fov = usingItem ? Fov.INSTANCE.itemFov.getValueFloat()
                    : Fov.INSTANCE.fov.getValueFloat();
            if (Zoom.on) {
                fov = Math.min(Math.max(fov - Zoom.INSTANCE.currentFov, 1f), 177f);
            }
            ci.setReturnValue(fov);
            return;
        }

        float d = 70.0f;
        if (changingFov) {
            d = ((Number) this.client.options.getFov().getValue()).floatValue();
            d *= MathHelper.lerp(tickProgress, this.lastFovMultiplier, this.fovMultiplier);
        }
        if (Zoom.on) {
            d = Math.min(Math.max(d - Zoom.INSTANCE.currentFov, 1f), 177f);
        }
        ci.setReturnValue(d);
    }

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void getNightVisionStrengthHook(LivingEntity entity, float tickProgress, CallbackInfoReturnable<Float> cir) {
        StatusEffectInstance statusEffectInstance = entity.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (statusEffectInstance == null)
            cir.setReturnValue(0f);
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    public void postRender3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
        Alien.SHADER.renderShaders();
    }

    @Inject(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (AspectRatio.INSTANCE.isOn()) {
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.peek().getPositionMatrix().identity();
            if (Zoom.on) {
                matrixStack.translate(Zoom.INSTANCE.zoomX, -Zoom.INSTANCE.zoomY, 0.0f);
                matrixStack.scale(Zoom.INSTANCE.zoom, Zoom.INSTANCE.zoom, 1.0f);
            }
            float farPlane = Math.max(MinecraftClient.getInstance().options.getClampedViewDistance() * 16.0F, ((Number) MinecraftClient.getInstance().options.getCloudRenderDistance().getValue()).floatValue() * 16);
            matrixStack.peek().getPositionMatrix().mul(new Matrix4f().setPerspective((float) (fov * Math.PI / 180F), AspectRatio.INSTANCE.ratio.getValueFloat(), 0.05f, farPlane));
            cir.setReturnValue(matrixStack.peek().getPositionMatrix());
        }
    }
}

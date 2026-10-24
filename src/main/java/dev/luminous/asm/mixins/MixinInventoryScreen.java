package dev.luminous.asm.mixins;

import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
    @Unique
    private static LivingEntity alien$previewEntity;

    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD")
    )
    private static void alien$capturePreviewEntity(DrawContext context, int x1, int y1, int x2, int y2,
                                                   int size, float scale, float mouseX, float mouseY,
                                                   LivingEntity entity, CallbackInfo ci) {
        alien$previewEntity = entity;
    }

    @ModifyArgs(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;addEntity(Lnet/minecraft/client/render/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V"
            )
    )
    private static void alien$applyRenderRotation(Args args) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || alien$previewEntity != mc.player || !ClientSetting.INSTANCE.rotations.getValue()) {
            return;
        }
        EntityRenderState state = args.get(0);
        if (!(state instanceof LivingEntityRenderState livingState)) {
            return;
        }

        float bodyYaw = RotationManager.getRenderYawOffset();
        livingState.bodyYaw = bodyYaw;
        livingState.relativeHeadYaw = MathHelper.wrapDegrees(RotationManager.getRotationYawHead() - bodyYaw);
        livingState.pitch = RotationManager.getRenderPitch();
    }
}

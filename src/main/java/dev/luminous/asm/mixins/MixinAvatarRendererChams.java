package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Chams;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public class MixinAvatarRendererChams {

    @Redirect(
            method = "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Lnet/minecraft/client/model/ModelPart;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"
            )
    )
    private void submitChamsHand(OrderedRenderCommandQueue orderedRenderCommandQueue,
                                 ModelPart modelPart,
                                 MatrixStack matrixStack,
                                 RenderLayer renderLayer,
                                 int n,
                                 int n2,
                                 Sprite sprite) {
        Chams chams = Chams.INSTANCE;
        if (chams != null && chams.isOn() && chams.hand.booleanValue) {
            orderedRenderCommandQueue.submitModelPart(
                    modelPart,
                    matrixStack,
                    renderLayer,
                    n,
                    n2,
                    sprite,
                    false,
                    false,
                    chams.getHandArgb(),
                    null,
                    0
            );
        } else {
            orderedRenderCommandQueue.submitModelPart(modelPart, matrixStack, renderLayer, n, n2, sprite);
        }
    }
}

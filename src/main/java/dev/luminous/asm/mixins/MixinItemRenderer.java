package dev.luminous.asm.mixins;

import dev.luminous.api.utils.render.HandTintUtil;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @ModifyVariable(
            method = "renderItem",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static VertexConsumerProvider alien$tintHandItem(
            VertexConsumerProvider provider,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider originalProvider,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            RenderLayer layer,
            ItemRenderState.Glint glint
    ) {
        Chams chams = Chams.INSTANCE;
        if (chams != null && chams.isOn() && chams.hand.booleanValue && displayContext.isFirstPerson()) {
            return HandTintUtil.wrap(provider, chams.hand.getValue());
        }
        return provider;
    }

    @Inject(
            method = "renderItem",
            at = @At("HEAD")
    )
    private static void filterGroundItemQuads(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            net.minecraft.client.render.RenderLayer layer,
            net.minecraft.client.render.item.ItemRenderState.Glint glint,
            CallbackInfo ci
    ) {
        if (NoRender.INSTANCE.isOn()
                && NoRender.INSTANCE.fastItem.getValue()
                && !NoRender.INSTANCE.renderSidesOfItems.getValue()
                && displayContext == ItemDisplayContext.GROUND) {
            List<BakedQuad> filtered = new ArrayList<>();
            for (BakedQuad quad : quads) {
                if (quad.face() == null) {
                    filtered.add(quad);
                }
            }
            quads.clear();
            quads.addAll(filtered);
        }
    }
}

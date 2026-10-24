package dev.luminous.asm.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luminous.mod.modules.impl.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import java.util.List;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockModelRenderer.class})
public abstract class MixinBlockModelRenderer {
    @Inject(method={"renderSmooth", "renderFlat"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderSmooth(BlockRenderView world, List<BlockModelPart> model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, int overlay, CallbackInfo info) {
        if (Xray.shouldBlock(state)) {
            info.cancel();
        }
    }

    @ModifyReturnValue(
            method = "shouldDrawFace",
            at = @At("RETURN")
    )
    private static boolean hookShouldDrawFace(boolean original, BlockRenderView world, BlockState state, boolean cull, Direction side, BlockPos pos) {
        Xray xray = Xray.INSTANCE;
        if (xray.isOn()) {
            return xray.modifyDrawSideAtNeighbor(state, world, pos, side, original);
        }
        return original;
    }
}

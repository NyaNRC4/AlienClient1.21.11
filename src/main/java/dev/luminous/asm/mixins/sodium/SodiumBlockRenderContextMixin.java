package dev.luminous.asm.mixins.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luminous.mod.modules.impl.render.Xray;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class SodiumBlockRenderContextMixin {

    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @Shadow
    protected BlockRenderView level;

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private boolean onShouldDrawSide(boolean original, Direction facing) {
        Xray xray = Xray.INSTANCE;
        if (!xray.isOn() || state == null || pos == null || level == null) {
            return original;
        }

        return xray.modifyDrawSide(state, level, pos, facing, original);
    }
}

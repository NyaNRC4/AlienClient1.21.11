package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackEntityRenderState.class)
public abstract class MixinItemStackEntityRenderState {

    @Shadow
    public int renderedAmount;

    @Inject(method = "update", at = @At("TAIL"))
    private void modifyRenderAmount(Entity entity, ItemStack stack, ItemModelManager itemModelManager, CallbackInfo ci) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fastItem.getValue()) {
            int count = stack.getCount();
            if (count <= 1) {
                renderedAmount = 1;
            } else if (count <= 16) {
                renderedAmount = 2;
            } else if (count <= 32) {
                renderedAmount = 3;
            } else {
                renderedAmount = count <= 48 ? 4 : 5;
            }
        }
    }
}

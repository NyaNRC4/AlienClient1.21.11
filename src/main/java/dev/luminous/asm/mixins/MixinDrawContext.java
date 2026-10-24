package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DrawContext.class})
public class MixinDrawContext {
    @Unique
    private static final ItemStack[] ITEMS = new ItemStack[27];
    @Final
    @Shadow
    private Matrix3x2fStack matrices;

    @Inject(method={"drawStackCount(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"}, at={@At(value="TAIL")})
    public void onDrawItem(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        BlockItem blockItem;
        ItemStack[] itemStackArray;
        if (ShulkerViewer.INSTANCE.isOn() && ShulkerViewer.INSTANCE.icon.getValue() && stack.getItem() instanceof BlockItem && (blockItem = (BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
            ShulkerViewer.getItemsInContainerItem(stack, ITEMS);
            for (ItemStack itemStack : ITEMS) {
                if (itemStack.isEmpty()) continue;
                Matrix3x2fStack matrixStack = this.matrices;
                matrixStack.pushMatrix();
                matrixStack.scale(0.5f, 0.5f);
                this.drawItem(itemStack, x * 2 + 20, y * 2 + 20);
                matrixStack.popMatrix();
                return;
            }
        }
    }

    @Shadow
    public void drawItem(ItemStack item, int x, int y) {
    }
}

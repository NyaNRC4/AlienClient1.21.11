package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.util.function.Consumer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ItemStack.class})
public class MixinShulkerBoxBlock {
    @Inject(method={"appendTooltip"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAppendTooltip(Item.TooltipContext context, TooltipDisplayComponent displayComponent, net.minecraft.entity.player.PlayerEntity player, TooltipType type, Consumer<Text> textConsumer, CallbackInfo ci) {
        if (((ItemStack)(Object)this).getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock && ShulkerViewer.INSTANCE.isOn() && ShulkerViewer.INSTANCE.toolTips.getValue()) {
            ci.cancel();
        }
    }
}

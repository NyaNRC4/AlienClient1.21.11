package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={HandledScreen.class})
public abstract class MixinHandledScreen<T extends ScreenHandler>
        extends Screen
        implements ScreenHandlerProvider<T> {
    @Unique
    private static final ItemStack[] ITEMS = new ItemStack[27];
    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Shadow
    protected int x;
    @Shadow
    protected int y;

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Inject(method={"mouseClicked"}, at={@At(value="HEAD")}, cancellable=true)
    private void mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        ItemStack itemStack;
        if (click.button() == 2 && this.focusedSlot != null && !this.focusedSlot.getStack().isEmpty() && Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty() && ShulkerViewer.INSTANCE.isOn() && (ShulkerViewer.hasItems(itemStack = this.focusedSlot.getStack()) || itemStack.getItem() == Items.ENDER_CHEST && Alien.PLAYER.known)) {
            cir.setReturnValue(ShulkerViewer.openContainer(this.focusedSlot.getStack(), ITEMS, false));
        }
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ShulkerViewer.INSTANCE.isOn() && ShulkerViewer.INSTANCE.toolTips.getValue() && this.focusedSlot != null && !this.focusedSlot.getStack().isEmpty() && this.client.player.playerScreenHandler.getCursorStack().isEmpty() && (ShulkerViewer.hasItems(this.focusedSlot.getStack()) || this.focusedSlot.getStack().getItem() == Items.ENDER_CHEST && Alien.PLAYER.known)) {
            ShulkerViewer.renderShulkerToolTip(context, mouseX, mouseY, this.focusedSlot.getStack());
        }
    }

    @Shadow
    public abstract void renderBackground(DrawContext var1, int var2, int var3, float var4);
}


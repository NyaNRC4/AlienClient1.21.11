package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.ClickBlockEvent;
import dev.luminous.api.events.impl.InteractBlockEvent;
import dev.luminous.api.events.impl.InteractItemEvent;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Shadow
    private ItemStack selectedStack;

    @ModifyVariable(method = "isCurrentlyBreaking", at = @At("STORE"))
    private ItemStack stack(ItemStack stack) {
        return InteractTweaks.INSTANCE.noReset() ? this.selectedStack : stack;
    }

    @ModifyConstant(method = "updateBlockBreakingProgress", constant = @Constant(intValue = 5))
    private int MiningCooldownFix(int value) {
        return InteractTweaks.INSTANCE.noDelay() ? 0 : value;
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void hookInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        InteractItemEvent event = InteractItemEvent.getPre(hand);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactItem", at = @At("RETURN"))
    private void hookInteractItemReturn(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Alien.EVENT_BUS.post(InteractItemEvent.getPost(hand));
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void hookInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        InteractBlockEvent event = InteractBlockEvent.getPre(hand);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void hookInteractBlockReturn(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        Alien.EVENT_BUS.post(InteractBlockEvent.getPost(hand));
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void hookCancelBlockBreaking(CallbackInfo callbackInfo) {
        if (InteractTweaks.INSTANCE.noAbort())
            callbackInfo.cancel();
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ClickBlockEvent event = ClickBlockEvent.get(pos, direction);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}

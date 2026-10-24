package dev.luminous.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.luminous.mod.modules.impl.misc.ExtraTab;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

import static dev.luminous.api.utils.Wrapper.mc;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {
    @Final
    @Shadow
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
    private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerListEntry>> cir) {

        if (ExtraTab.INSTANCE.isOn()) {
            cir.setReturnValue(mc.player.networkHandler.getListedPlayerListEntries()
                    .stream().sorted(ENTRY_ORDERING).limit(ExtraTab.INSTANCE.size.getValueInt()).toList());
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void hookRender(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        int totalPlayers = this.collectPlayerEntries().size();
        int newP = 1;

        int newO = (totalPlayers + newP - 1) / newP;

        while (newO > ExtraTab.INSTANCE.columns.getValueInt()) {
            newP++;
            newO = (totalPlayers + newP - 1) / newP;
        }

        o.set(newO);
        p.set(newP);
    }
}
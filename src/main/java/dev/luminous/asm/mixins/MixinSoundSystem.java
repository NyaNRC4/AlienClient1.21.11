package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PlaySoundEvent;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={SoundSystem.class})
public class MixinSoundSystem {
    @Inject(method={"play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;"}, at={@At(value="HEAD")}, cancellable=true)
    private void onPlay(SoundInstance soundInstance, CallbackInfoReturnable<SoundSystem.PlayResult> info) {
        PlaySoundEvent event = PlaySoundEvent.get(soundInstance);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            info.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }
}

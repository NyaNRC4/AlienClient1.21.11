package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Ambience;
import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(value={BiomeEffects.class})
public class MixinBiomeEffects {
}

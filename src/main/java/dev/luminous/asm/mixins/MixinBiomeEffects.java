package dev.luminous.asm.mixins;

import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {BiomeEffects.class})
public class MixinBiomeEffects {
}

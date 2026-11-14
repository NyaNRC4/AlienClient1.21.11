package dev.luminous.asm.mixins;

import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {Biome.class})
public class MixinBiome {
}

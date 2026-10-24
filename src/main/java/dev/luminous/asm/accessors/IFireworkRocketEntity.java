package dev.luminous.asm.accessors;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={FireworkRocketEntity.class})
public interface IFireworkRocketEntity {
    @Accessor(value="shooter")
    public LivingEntity getShooter();

    @Invoker(value="wasShotByEntity")
    public boolean hookWasShotByEntity();

}

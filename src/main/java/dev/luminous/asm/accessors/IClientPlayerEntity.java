package dev.luminous.asm.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ClientPlayerEntity.class})
public interface IClientPlayerEntity {
    @Accessor(value="ticksSinceLastPositionPacketSent")
    public void setTicksSinceLastPositionPacketSent(int var1);
    @Accessor(value="lastYawClient")
    public float getLastYaw();
    @Accessor(value="lastYawClient")
    public void setLastYaw(float var1);
    @Accessor(value="lastPitchClient")
    public float getLastPitch();
    @Accessor(value="lastPitchClient")
    public void setLastPitch(float var1);
}


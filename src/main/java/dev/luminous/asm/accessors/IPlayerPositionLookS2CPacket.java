package dev.luminous.asm.accessors;

import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={PlayerPositionLookS2CPacket.class})
public interface IPlayerPositionLookS2CPacket {
    @Invoker(value="change")
    public EntityPosition invokeChange();
}


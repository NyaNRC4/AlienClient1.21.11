package dev.luminous.asm.accessors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface IClientPlayerInteractionManager {

    @Accessor("lastSelectedSlot")
    int getLastSelectedSlot();

    @Accessor("lastSelectedSlot")
    void setLastSelectedSlot(int slot);
}

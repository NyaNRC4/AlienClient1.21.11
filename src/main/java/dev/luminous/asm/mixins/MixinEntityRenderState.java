package dev.luminous.asm.mixins;

import dev.luminous.api.interfaces.IEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class MixinEntityRenderState implements IEntityRenderState {
    @Unique
    private Entity alien$entity;

    @Override
    public void alien$setEntity(Entity entity) {
        this.alien$entity = entity;
    }

    @Override
    public Entity alien$getEntity() {
        return this.alien$entity;
    }
}

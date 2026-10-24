package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.EntitySpawnEvent;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.events.impl.RemoveEntityEvent;
import dev.luminous.api.events.impl.TickEntityEvent;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World {

    protected MixinClientWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }



    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    public void onTickEntity(Entity entity, CallbackInfo ci) {
        TickEntityEvent event = TickEntityEvent.get(entity);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    public void onAddEntity(Entity entity, CallbackInfo ci) {
        EntitySpawnEvent event = EntitySpawnEvent.get(entity);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At(value = "HEAD"))
    private void hookRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = getEntityById(entityId);
        if (entity == null) {
            return;
        }
        RemoveEntityEvent removeEntityEvent = RemoveEntityEvent.get(entity, removalReason);
        Alien.EVENT_BUS.post(removeEntityEvent);
    }

    @Inject(method = "addEntity", at = @At("TAIL"))
    public void onAddEntityTail(Entity entity, CallbackInfo ci) {
        EntitySpawnedEvent event = EntitySpawnedEvent.get(entity);
        Alien.EVENT_BUS.post(event);
    }

    @Override
    public float getRainGradient(float delta) {
        return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0 : super.getRainGradient(delta);
    }

    @Override
    public float getThunderGradient(float delta) {
        return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0 : super.getThunderGradient(delta);
    }
}

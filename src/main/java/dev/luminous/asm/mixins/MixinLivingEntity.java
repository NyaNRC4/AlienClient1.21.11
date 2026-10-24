package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.JumpEvent;
import dev.luminous.api.events.impl.LerpToEvent;
import dev.luminous.api.events.impl.SprintEvent;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AntiEffects;
import dev.luminous.mod.modules.impl.render.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LivingEntity.class})
public abstract class MixinLivingEntity
        extends Entity {
    @Final
    @Shadow
    private static EntityAttributeModifier SPRINTING_SPEED_BOOST;
    @Unique
    private boolean previousElytra = false;
    @Unique
    private long lastLerp = 0L;

    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    @Nullable
    public EntityAttributeInstance getAttributeInstance(RegistryEntry<EntityAttribute> attribute) {
        return this.getAttributes().getCustomInstance(attribute);
    }

    @Shadow
    public AttributeContainer getAttributes() {
        return null;
    }

    @Shadow
    public abstract void remove(Entity.RemovalReason var1);

    @Inject(method={"getHandSwingDuration"}, at={@At(value="HEAD")}, cancellable=true)
    private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
        if (ViewModel.INSTANCE.isOn() && ViewModel.INSTANCE.slowAnimation.getValue()) {
            info.setReturnValue(ViewModel.INSTANCE.slowAnimationVal.getValueInt());
        }
    }

    @Redirect(
            method = "getEffectiveGravity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"),
            require = 0
    )
    private boolean slowFallHook(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
        if (AntiEffects.INSTANCE.isOn() && effect == StatusEffects.SLOW_FALLING && AntiEffects.INSTANCE.slowFalling.getValue()) {
            return false;
        }
        return instance.hasStatusEffect(effect);
    }

    @Redirect(
            method = "travelMidAir",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/entity/effect/StatusEffectInstance;"),
            require = 0
    )
    private StatusEffectInstance levitationHook(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
        if (AntiEffects.INSTANCE.isOn() && effect == StatusEffects.LEVITATION && AntiEffects.INSTANCE.levitation.getValue()) {
            return null;
        }
        return instance.getStatusEffect(effect);
    }



    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jumpPre(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;
        JumpEvent pre = JumpEvent.get(Event.Stage.Pre);
        Alien.EVENT_BUS.post(pre);
        if (pre.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void jumpPost(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity)) return;
        JumpEvent post = JumpEvent.get(Event.Stage.Post);
        Alien.EVENT_BUS.post(post);
    }

    @Redirect(method={"applyMovementInput"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;isClimbing()Z"), require=0)
    public boolean climbingHook(LivingEntity instance) {
        if (Velocity.INSTANCE.isOn() && Velocity.INSTANCE.noClimb.getValue() && LivingEntity.class.cast((Object)this) == MinecraftClient.getInstance().player) {
            return false;
        }
        return instance.isClimbing();
    }

    @Redirect(method={"applyClimbingSpeed"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;isClimbing()Z"), require=0)
    public boolean climbingHook2(LivingEntity instance) {
        if (NoSlow.INSTANCE.climb() && LivingEntity.class.cast((Object)this) == MinecraftClient.getInstance().player) {
            return false;
        }
        return instance.isClimbing();
    }

    @Inject(method={"setSprinting"}, at={@At(value="HEAD")}, cancellable=true)
    public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
        if (LivingEntity.class.cast((Object)this) == MinecraftClient.getInstance().player) {
            SprintEvent event = SprintEvent.get();
            Alien.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                ci.cancel();
                sprinting = event.isSprint();
                super.setSprinting(sprinting);
                EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance((RegistryEntry<EntityAttribute>)EntityAttributes.MOVEMENT_SPEED);
                entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.id());
                if (sprinting) {
                    entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
                }
            }
        }
    }
}


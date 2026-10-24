package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.LookDirectionEvent;
import dev.luminous.api.events.impl.SprintEvent;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static dev.luminous.api.utils.Wrapper.mc;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw)
    {
        return null;
    }

    @Inject(method = "changeLookDirection", at = @At(value = "HEAD"), cancellable = true)
    private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (Entity.class.cast(this) == mc.player) {
            LookDirectionEvent lookDirectionEvent = LookDirectionEvent.get(
                    Entity.class.cast(this), cursorDeltaX, cursorDeltaY);
            Alien.EVENT_BUS.post(lookDirectionEvent);
            if (lookDirectionEvent.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(at = {@At("HEAD")}, method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", cancellable = true)
    private void onIsInvisibleCheck(PlayerEntity message, CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.invisible.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    void isGlowingHook(CallbackInfoReturnable<Boolean> cir) {
        if (ShaderModule.INSTANCE.isOn()) {
            cir.setReturnValue(ShaderModule.INSTANCE.shouldRender(Entity.class.cast(this)));
        }
    }

    @ModifyArgs(method = "pushAwayFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    private void pushAwayFromHook(Args args) {
        if (Entity.class.cast(this) == MinecraftClient.getInstance().player) {
            if (Velocity.INSTANCE.isOn() && Velocity.INSTANCE.entityPush.getValue()) {
                args.set(0, 0d);
                args.set(1, 0d);
                args.set(2, 0d);
            }
        }
    }

    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fireEntity.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"setSprinting"}, at = {@At("HEAD")}, cancellable = true)
    public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
        if (Entity.class.cast(this) == MinecraftClient.getInstance().player) {
            SprintEvent event = SprintEvent.get();
            Alien.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                ci.cancel();
                sprinting = event.isSprint();
                this.setFlag(SPRINTING_FLAG_INDEX, sprinting);
            }
        }
    }

    @Inject(method={"getVelocityMultiplier()F"}, at={@At(value="RETURN")}, cancellable=true)
    private void setCallbackInfoReturnable2(CallbackInfoReturnable callbackInfoReturnable) {
        if ((Object) this instanceof ClientPlayerEntity && NoSlow.INSTANCE != null && NoSlow.INSTANCE.isOn()) {
            callbackInfoReturnable.setReturnValue((Object)Float.valueOf(1.0f));
        }
    }

    @Shadow
    private static final int SPRINTING_FLAG_INDEX = 3;

    @Shadow
    protected void setFlag(int index, boolean value) {
    }
}

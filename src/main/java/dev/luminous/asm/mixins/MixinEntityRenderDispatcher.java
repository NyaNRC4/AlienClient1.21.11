package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.RenderEntityEvent;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EntityRenderManager.class})
public class MixinEntityRenderDispatcher {
    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V"}, at={@At(value="HEAD")}, cancellable=true)
    public void onRender(EntityRenderState renderState, CameraRenderState cameraState, double offsetX, double offsetY, double offsetZ, MatrixStack matrices, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        RenderEntityEvent event = RenderEntityEvent.get(renderState.entityType);
        Alien.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
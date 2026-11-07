package dev.luminous.asm.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.interfaces.IEntityRenderState;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.render.ProjectionUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.render.Chams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    public void getWorldMatrices(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f basicProjectionMatrix,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci) {
        if (Alien.MODULE == null || Alien.FPS == null || Module.nullCheck()) {
            return;
        }

        ProjectionUtil.capture(positionMatrix, basicProjectionMatrix);

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);

        TextUtil.lastProjMat.set(basicProjectionMatrix);
        TextUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
        TextUtil.lastWorldSpaceMatrix.set(matrices.peek().getPositionMatrix());

        Alien.FPS.record();
    }

    @Redirect(
            method = "pushEntityRenders",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderManager;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V"
            )
    )
    private void ChamsRender(
            EntityRenderManager renderManager,
            EntityRenderState renderState,
            CameraRenderState cameraState,
            double offsetX,
            double offsetY,
            double offsetZ,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue) {
        Entity entity = ((IEntityRenderState) renderState).alien$getEntity();
        Chams chams = Chams.INSTANCE;
        if (entity != null
                && entity != MinecraftClient.getInstance().player
                && chams != null
                && chams.cancelsVanillaRender(entity)) {
            return;
        }
        renderManager.render(renderState, cameraState, offsetX, offsetY, offsetZ, matrices, queue);
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;updateCamera(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;Z)V"
            ),
            index = 2
    )
    private boolean freecamSpectator(boolean spectator) {
        return Freecam.INSTANCE != null && Freecam.INSTANCE.isOn() || spectator;
    }
}

package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;

public final class EntityOverlayRenderer {

    private static SimpleFramebuffer throughWallDepth;

    private EntityOverlayRenderer() {
    }

    public static void flushThroughWall() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (mc.gameRenderer.getEntityRenderCommandQueue().getBatchingQueues().values().stream()
                .noneMatch(queue -> queue.hasCommands())) {
            return;
        }

        if (throughWallDepth == null
                || throughWallDepth.textureWidth != width
                || throughWallDepth.textureHeight != height) {
            if (throughWallDepth != null) {
                throughWallDepth.delete();
            }
            throughWallDepth = new SimpleFramebuffer("alien_entity_overlay_depth", width, height, true);
        }

        RenderSystem.getDevice()
                .createCommandEncoder()
                .clearDepthTexture(throughWallDepth.getDepthAttachment(), 1.0);

        GpuTextureView previousDepthOverride = RenderSystem.outputDepthTextureOverride;
        GpuTextureView previousColorOverride = RenderSystem.outputColorTextureOverride;
        RenderSystem.outputColorTextureOverride = mc.getFramebuffer().getColorAttachmentView();
        RenderSystem.outputDepthTextureOverride = throughWallDepth.getDepthAttachmentView();
        try {
            mc.gameRenderer.getEntityRenderDispatcher().render();
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
        } finally {
            RenderSystem.outputColorTextureOverride = previousColorOverride;
            RenderSystem.outputDepthTextureOverride = previousDepthOverride;
        }
    }
}

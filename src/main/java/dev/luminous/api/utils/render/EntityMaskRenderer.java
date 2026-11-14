package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SequencedMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.util.BufferAllocator;

public final class EntityMaskRenderer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alien/OffscreenEntities");
    private static boolean errorLogged;

    private static OrderedRenderCommandQueueImpl queue;
    private static RenderDispatcher dispatcher;
    private static VertexConsumerProvider.Immediate immediate;
    private static OutlineVertexConsumerProvider outlineVertexConsumers;
    private static boolean pending;

    private EntityMaskRenderer() {
    }

    public static OrderedRenderCommandQueueImpl begin() {
        if (queue == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            queue = new OrderedRenderCommandQueueImpl();
            immediate = createImmediate();
            outlineVertexConsumers = new OutlineVertexConsumerProvider();
            dispatcher = new RenderDispatcher(
                    queue,
                    client.getBlockRenderManager(),
                    immediate,
                    client.getAtlasManager(),
                    outlineVertexConsumers,
                    VertexConsumerProvider.immediate(new BufferAllocator(1536)),
                    client.textRenderer);
        }
        pending = true;
        return queue;
    }

    public static OrderedRenderCommandQueueImpl queue() {
        return queue;
    }

    public static boolean hasPending() {
        return pending;
    }

    public static boolean hasCommands() {
        if (queue == null) {
            return false;
        }
        return queue.getBatchingQueues().values().stream().anyMatch(BatchingRenderCommandQueue::hasCommands);
    }

    public static void discard() {
        if (queue != null) {
            queue.clear();
        }
        pending = false;
    }

    public static void end(Framebuffer target) {
        draw(target, true);
    }

    public static void flush(Framebuffer target) {
        draw(target, false);
    }

    private static void draw(Framebuffer target, boolean clearDepth) {
        if (queue == null || target == null || target.getColorAttachmentView() == null) {
            discard();
            return;
        }

        com.mojang.blaze3d.textures.GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
        com.mojang.blaze3d.textures.GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
        try {
            if (clearDepth && target.getDepthAttachment() != null) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthAttachment(), 1.0);
            }
            RenderSystem.outputColorTextureOverride = target.getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = target.getDepthAttachmentView();
            dispatcher.render();
            outlineVertexConsumers.draw();
            immediate.draw();
        } catch (Throwable throwable) {
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.error("Alien offscreen entity render failed", throwable);
            }
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
            queue.clear();
            pending = false;
        }
    }

    private static VertexConsumerProvider.Immediate createImmediate() {
        SequencedMap<RenderLayer, BufferAllocator> layers = new Object2ObjectLinkedOpenHashMap<>();
        assign(layers, TexturedRenderLayers.getEntitySolid());
        assign(layers, TexturedRenderLayers.getEntityCutout());
        assign(layers, TexturedRenderLayers.getItemTranslucentCull());
        assign(layers, TexturedRenderLayers.getBlockTranslucentCull());
        assign(layers, TexturedRenderLayers.getShieldPatterns());
        assign(layers, TexturedRenderLayers.getBeds());
        assign(layers, TexturedRenderLayers.getShulkerBoxes());
        assign(layers, TexturedRenderLayers.getSign());
        assign(layers, TexturedRenderLayers.getHangingSign());
        layers.put(TexturedRenderLayers.getChest(), new BufferAllocator(786432));
        assign(layers, RenderLayers.armorEntityGlint());
        assign(layers, RenderLayers.glint());
        assign(layers, RenderLayers.glintTranslucent());
        assign(layers, RenderLayers.entityGlint());
        assign(layers, RenderLayers.waterMask());
        return VertexConsumerProvider.immediate(layers, new BufferAllocator(786432));
    }

    private static void assign(SequencedMap<RenderLayer, BufferAllocator> layers, RenderLayer layer) {
        layers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
    }
}

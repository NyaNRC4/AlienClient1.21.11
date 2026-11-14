package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityRenderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("Alien/OffscreenEntityPass");

    private static SimpleFramebuffer throughWallTarget;
    private static boolean localPlayerOverlayRequested;
    private static boolean errorLogged;

    private EntityRenderUtil() {
    }

    public static OrderedRenderCommandQueue entityQueue() {
        return EntityMaskRenderer.begin();
    }

    public static void flushThroughWall() {
        if (!EntityMaskRenderer.hasPending()) {
            return;
        }
        if (!EntityMaskRenderer.hasCommands()) {
            EntityMaskRenderer.discard();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        if (main == null || main.getColorAttachment() == null || main.getColorAttachmentView() == null) {
            EntityMaskRenderer.discard();
            return;
        }

        int width = main.textureWidth;
        int height = main.textureHeight;
        if (width <= 0 || height <= 0) {
            EntityMaskRenderer.discard();
            return;
        }

        try {
            SimpleFramebuffer target = ensureTarget(width, height);
            copyColor(main, target);
            EntityMaskRenderer.end(target);
            copyColor(target, main);
        } catch (Throwable throwable) {
            EntityMaskRenderer.discard();
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.error("Alien offscreen through-wall pass failed", throwable);
            }
        }
    }

    public static void requestSelf() {
        localPlayerOverlayRequested = true;
    }

    public static void renderSelf(MatrixStack matrices) {
        if (!localPlayerOverlayRequested) {
            return;
        }
        localPlayerOverlayRequested = false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.gameRenderer == null || mc.options == null) {
            return;
        }
        if (mc.options.getPerspective().isFirstPerson() || mc.player.isInvisible() || mc.player.isSpectator()) {
            return;
        }

        CameraRenderState cameraState = mc.gameRenderer.getEntityRenderStates().cameraRenderState;
        Framebuffer main = mc.getFramebuffer();
        if (cameraState == null || main == null || main.getColorAttachmentView() == null) {
            EntityMaskRenderer.discard();
            return;
        }

        float delta = mc.getRenderTickCounter().getTickProgress(!mc.world.getTickManager().shouldSkipTick(mc.player));
        try {
            ModelPlayer.renderTexture(matrices, EntityMaskRenderer.begin(), cameraState, mc.player, delta, false);
        } finally {
            EntityMaskRenderer.flush(main);
        }
    }

    private static SimpleFramebuffer ensureTarget(int width, int height) {
        if (throughWallTarget == null) {
            throughWallTarget = new SimpleFramebuffer("alien_entity_overlay", width, height, true);
        } else if (throughWallTarget.textureWidth != width || throughWallTarget.textureHeight != height) {
            throughWallTarget.resize(width, height);
        }
        return throughWallTarget;
    }

    private static void copyColor(Framebuffer from, Framebuffer to) {
        if (from == null || to == null) {
            return;
        }
        GpuTexture source = from.getColorAttachment();
        GpuTexture destination = to.getColorAttachment();
        if (source == null || destination == null) {
            return;
        }
        int width = Math.min(from.textureWidth, to.textureWidth);
        int height = Math.min(from.textureHeight, to.textureHeight);
        if (width <= 0 || height <= 0) {
            return;
        }
        RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(source, destination, 0, 0, 0, 0, 0, width, height);
    }
}

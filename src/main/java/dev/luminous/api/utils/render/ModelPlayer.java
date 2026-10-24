package dev.luminous.api.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.asm.accessors.IModelPart;
import dev.luminous.asm.accessors.IModelPartCuboid;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModelPlayer
        extends PlayerEntityModel {
    public final PlayerEntity player;
    private final SkinTextures skin;
    private static final Vector4f pos1 = new Vector4f();
    private static final Vector4f pos2 = new Vector4f();
    private static final Vector4f pos3 = new Vector4f();
    private static final Vector4f pos4 = new Vector4f();

    public ModelPlayer(PlayerEntity player) {
        super(Wrapper.mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
        this.player = player;
        this.skin = player instanceof ClientPlayerLikeEntity clientPlayer
                ? clientPlayer.getSkin()
                : DefaultSkinHelper.getSteve();
        this.leftPants.visible = false;
        this.rightPants.visible = false;
        this.leftSleeve.visible = false;
        this.rightSleeve.visible = false;
        this.jacket.visible = false;
        this.hat.visible = false;
        this.getHead().scale(new Vector3f(-0.05f, -0.05f, -0.05f));
    }

    public void render(MatrixStack matrices, ColorSetting fill, ColorSetting line) {
        this.render(matrices, fill, line, 1.0, 0.0, 1.0, 0.0, false, false);
    }

    public void render(MatrixStack matrices, ColorSetting fill, ColorSetting line, double alpha, double yOffset, double scale, double yaw, boolean noLimb, boolean forceSneaking) {
        PlayerEntityRenderState state = new PlayerEntityRenderState();

        state.bodyYaw = this.player.bodyYaw;
        //state.headYaw = this.player.headYaw;
        state.pitch = this.player.getPitch();
        state.limbSwingAmplitude = this.player.limbAnimator.getSpeed();
        state.limbSwingAnimationProgress = this.player.limbAnimator.getAnimationProgress();
        state.isInSneakingPose = this.player.isSneaking();
        state.isGliding = this.player.isGliding();
        state.isSwimming = this.player.isInSwimmingPose();
        //state.isTouchingWater = this.player.isTouchingWater();
        state.handSwingProgress = this.player.handSwingProgress;
        //state.handSwingTicks = this.player.handSwingTicks;
        state.leaningPitch = ((ILivingEntity) this.player).getLeaningPitch();

        if (forceSneaking) {
            state.isInSneakingPose = true;
        }
        if (noLimb) {
            state.limbSwingAnimationProgress = 0.0f;
            state.limbSwingAmplitude = 0.0f;
        }

        double x = this.player.getX() - Wrapper.mc.gameRenderer.getCamera().getCameraPos().getX();
        double y = this.player.getY() - Wrapper.mc.gameRenderer.getCamera().getCameraPos().getY() + yOffset;
        double z = this.player.getZ() - Wrapper.mc.gameRenderer.getCamera().getCameraPos().getZ();
        matrices.push();
        matrices.translate((float)x, (float)y, (float)z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtil.rad(180.0f - this.player.bodyYaw + (float)yaw)));
        float j = ((ILivingEntity)this.player).getLeaningPitch();
        if (this.player.isGliding()) {
            float k = this.player.getPitch();
            float l = (float)this.player.getGlidingTicks() + this.player.bodyYaw + (float)yaw;
            float m = MathHelper.clamp((float)(l * l / 100.0f), (float)0.0f, (float)1.0f);
            if (!this.player.isUsingRiptide()) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m * (-90.0f - k)));
            }
            Vec3d vec3d = this.player.getRotationVec(1.0f);
            Vec3d vec3d2 = this.player.getVelocity();
            double d = vec3d2.horizontalLengthSquared();
            double e = vec3d.horizontalLengthSquared();
            if (d > 0.0 && e > 0.0) {
                double n = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
                double o = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float)(Math.signum(o) * Math.acos(n))));
            }
        } else if (j > 0.0f) {
            float k = this.player.getPitch();
            float l = this.player.isTouchingWater() ? -90.0f - k : -90.0f;
            float m = MathHelper.lerp((float)j, (float)0.0f, (float)l);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m));
            if (this.player.isInSwimmingPose()) {
                matrices.translate(0.0f, -1.0f, 0.3f);
            }
        }
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0.0f, -1.401f, 0.0f);
        matrices.scale((float)scale * 0.93f, (float)scale * 0.93f, (float)scale * 0.93f);
        this.setAngles(state);
        GlStateManager._enableBlend();
        GlStateManager._disableDepthTest();
        ModelPlayer.render(matrices, this.head, fill, line, alpha, false);
        ModelPlayer.render(matrices, this.body, fill, line, alpha, false);
        ModelPlayer.render(matrices, this.leftArm, fill, line, alpha, false);
        ModelPlayer.render(matrices, this.rightArm, fill, line, alpha, false);
        ModelPlayer.render(matrices, this.leftLeg, fill, line, alpha, false);
        ModelPlayer.render(matrices, this.rightLeg, fill, line, alpha, false);
        matrices.pop();
        GlStateManager._disableBlend();
        GlStateManager._enableDepthTest();
    }



    public static void render(MatrixStack matrices, ModelPart part, ColorSetting fill, ColorSetting line, double alpha, boolean texture) {
        if (!part.visible || ((IModelPart)(Object)part).getCuboids().isEmpty() && ((IModelPart)(Object)part).getChildren().isEmpty()) {
            return;
        }
        matrices.push();
        part.applyTransform(matrices);
        for (ModelPart.Cuboid cuboid : ((IModelPart)(Object)part).getCuboids()) {
            ModelPlayer.render(matrices, cuboid, fill, line, alpha, texture);
        }
        for (ModelPart child : ((IModelPart)(Object)part).getChildren().values()) {
            ModelPlayer.render(matrices, child, fill, line, alpha, texture);
        }
        matrices.pop();
    }

    public static void render(MatrixStack matrices, ModelPart.Cuboid cuboid, ColorSetting fill, ColorSetting line, double alpha, boolean texture) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (ModelPart.Quad quad : ((IModelPartCuboid)(Object)cuboid).getSides()) {
            BufferBuilder buffer;
            float elementCodec;
            float g;
            float r;
            float keyCodec;
            Color color;
            pos1.set(quad.vertices()[0].x() / 16.0f, quad.vertices()[0].y() / 16.0f, quad.vertices()[0].z() / 16.0f, 1.0f);
            pos1.mul((Matrix4fc)matrix);
            pos2.set(quad.vertices()[1].x() / 16.0f, quad.vertices()[1].y() / 16.0f, quad.vertices()[1].z() / 16.0f, 1.0f);
            pos2.mul((Matrix4fc)matrix);
            pos3.set(quad.vertices()[2].x() / 16.0f, quad.vertices()[2].y() / 16.0f, quad.vertices()[2].z() / 16.0f, 1.0f);
            pos3.mul((Matrix4fc)matrix);
            pos4.set(quad.vertices()[3].x() / 16.0f, quad.vertices()[3].y() / 16.0f, quad.vertices()[3].z() / 16.0f, 1.0f);
            pos4.mul((Matrix4fc)matrix);
            if (fill.booleanValue) {
                color = fill.getValue();
                keyCodec = (float)((double)((float)color.getAlpha() / 255.0f) * alpha);
                r = (float)color.getRed() / 255.0f;
                g = (float)color.getGreen() / 255.0f;
                elementCodec = (float)color.getBlue() / 255.0f;
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, texture ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR);
                buffer.vertex(ModelPlayer.pos1.x, ModelPlayer.pos1.y, ModelPlayer.pos1.z).texture(quad.vertices()[0].u(), quad.vertices()[0].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos2.x, ModelPlayer.pos2.y, ModelPlayer.pos2.z).texture(quad.vertices()[1].u(), quad.vertices()[1].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos2.x, ModelPlayer.pos2.y, ModelPlayer.pos2.z).texture(quad.vertices()[1].u(), quad.vertices()[1].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos3.x, ModelPlayer.pos3.y, ModelPlayer.pos3.z).texture(quad.vertices()[2].u(), quad.vertices()[2].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos3.x, ModelPlayer.pos3.y, ModelPlayer.pos3.z).texture(quad.vertices()[2].u(), quad.vertices()[2].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos4.x, ModelPlayer.pos4.y, ModelPlayer.pos4.z).texture(quad.vertices()[3].u(), quad.vertices()[3].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos1.x, ModelPlayer.pos1.y, ModelPlayer.pos1.z).texture(quad.vertices()[0].u(), quad.vertices()[0].v()).color(r, g, elementCodec, keyCodec);
                buffer.vertex(ModelPlayer.pos1.x, ModelPlayer.pos1.y, ModelPlayer.pos1.z).texture(quad.vertices()[0].u(), quad.vertices()[0].v()).color(r, g, elementCodec, keyCodec);
                Render3DUtil.endBuilding(buffer);
            }
            if (!line.booleanValue) continue;
            color = line.getValue();
            keyCodec = (float)((double)((float)color.getAlpha() / 255.0f) * alpha);
            r = (float)color.getRed() / 255.0f;
            g = (float)color.getGreen() / 255.0f;
            elementCodec = (float)color.getBlue() / 255.0f;
            buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, texture ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR);
            buffer.vertex(ModelPlayer.pos1.x, ModelPlayer.pos1.y, ModelPlayer.pos1.z).texture(quad.vertices()[0].u(), quad.vertices()[0].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos2.x, ModelPlayer.pos2.y, ModelPlayer.pos2.z).texture(quad.vertices()[1].u(), quad.vertices()[1].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos2.x, ModelPlayer.pos2.y, ModelPlayer.pos2.z).texture(quad.vertices()[1].u(), quad.vertices()[1].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos3.x, ModelPlayer.pos3.y, ModelPlayer.pos3.z).texture(quad.vertices()[2].u(), quad.vertices()[2].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos3.x, ModelPlayer.pos3.y, ModelPlayer.pos3.z).texture(quad.vertices()[2].u(), quad.vertices()[2].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos4.x, ModelPlayer.pos4.y, ModelPlayer.pos4.z).texture(quad.vertices()[3].u(), quad.vertices()[3].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos4.x, ModelPlayer.pos4.y, ModelPlayer.pos4.z).texture(quad.vertices()[3].u(), quad.vertices()[3].v()).color(r, g, elementCodec, keyCodec);
            buffer.vertex(ModelPlayer.pos1.x, ModelPlayer.pos1.y, ModelPlayer.pos1.z).texture(quad.vertices()[0].u(), quad.vertices()[0].v()).color(r, g, elementCodec, keyCodec);
            Render3DUtil.endBuilding(buffer);
        }
    }

    public void renderPlayerSkin(MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, float tickDelta) {
        if (!(this.player instanceof AbstractClientPlayerEntity clientPlayer)) {
            return;
        }

        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer) Wrapper.mc.getEntityRenderDispatcher().getRenderer(clientPlayer);
        PlayerEntityRenderState state = (PlayerEntityRenderState) playerRenderer.createRenderState();
        playerRenderer.updateRenderState(clientPlayer, state, tickDelta);
        // Keep the skin that was captured while the player was still online.
        state.skinTextures = this.skin;

        EntityRenderer renderer = Wrapper.mc.getEntityRenderDispatcher().getRenderer(state);
        Vec3d cameraPos = Wrapper.mc.gameRenderer.getCamera().getCameraPos();
        Vec3d offset = renderer.getPositionOffset(state);
        matrices.push();
        // Use the interpolated render position from the state, not the raw tick
        // position.  Otherwise the chams copy snaps once per tick while the
        // regular entity render moves smoothly (visible stuttering/one-frame lag).
        matrices.translate(
                state.x - cameraPos.x + offset.x,
                state.y - cameraPos.y + offset.y,
                state.z - cameraPos.z + offset.z
        );
        renderer.render(state, matrices, queue, cameraState);
        matrices.pop();
    }

    public static void renderTexture(MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, Entity entity, float tickDelta) {
        EntityRenderer renderer = Wrapper.mc.getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = renderer.createRenderState();
        renderer.updateRenderState(entity, state, tickDelta);

        Vec3d cameraPos = Wrapper.mc.gameRenderer.getCamera().getCameraPos();
        Vec3d offset = renderer.getPositionOffset(state);
        matrices.push();
        matrices.translate(
                state.x - cameraPos.x + offset.x,
                state.y - cameraPos.y + offset.y,
                state.z - cameraPos.z + offset.z
        );
        renderer.render(state, matrices, queue, cameraState);
        matrices.pop();
    }
}

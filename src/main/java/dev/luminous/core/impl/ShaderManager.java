package dev.luminous.core.impl;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.EntityMaskRenderer;
import dev.luminous.api.utils.render.ProjectionUtil;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import satin.ManagedShaderEffect;
import satin.PostEffect;
import satin.ShaderEffectManager;

public class ShaderManager
        implements Wrapper {
    private static final String[] POST_EFFECTS = new String[]{"outline", "pulse", "smoke", "gradient", "snow", "flow", "rainbow"};

    static final Timer timer = new Timer();
    private final Map<Shader, ManagedShaderEffect> effects = new EnumMap<Shader, ManagedShaderEffect>(Shader.class);
    private final List<RenderTask> tasks = new ArrayList<RenderTask>();
    public float time = 0.0f;
    private Framebuffer sceneBuffer;
    private boolean compositeErrorLogged;

    public void renderShader(Runnable runnable, Shader mode) {
        this.tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        if (this.tasks.isEmpty()) {
            return;
        }
        for (RenderTask task : this.tasks) {
            this.applyShader(task.task(), task.shader());
        }
        this.tasks.clear();
    }

    public void applyShader(Runnable runnable, Shader mode) {
        if (this.fullNullCheck()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null) {
            return;
        }
        if (this.sceneBuffer.textureWidth != main.textureWidth || this.sceneBuffer.textureHeight != main.textureHeight) {
            this.sceneBuffer.resize(main.textureWidth, main.textureHeight);
        }
        this.clearSceneBuffer();

        GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = this.sceneBuffer.getColorAttachmentView();
        RenderSystem.outputDepthTextureOverride = this.sceneBuffer.useDepthAttachment ? this.sceneBuffer.getDepthAttachmentView() : null;
        try {
            runnable.run();
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
        }

        this.setupShader(mode, this.getShader(mode), this.sceneBuffer);
    }

    public boolean overrideEntityOutline(Shader mode) {
        ManagedShaderEffect effect = this.getShader(mode);
        return effect != null && effect.getShaderEffect() != null;
    }

    public void renderOutline(Shader mode, Framebuffer outlineBuffer) {
        this.setupShader(mode, this.getShaderOutline(mode), outlineBuffer);
    }

    public void renderEntityShader(Shader mode) {
        ShaderModule module = ShaderModule.INSTANCE;
        if (module == null || !module.isOn()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        if (this.fullNullCheck()) {
            return;
        }
        Framebuffer main = client.getFramebuffer();
        if (main == null || this.sceneBuffer == null || this.sceneBuffer.getColorAttachmentView() == null) {
            return;
        }
        if (this.sceneBuffer.textureWidth != main.textureWidth || this.sceneBuffer.textureHeight != main.textureHeight) {
            this.sceneBuffer.resize(main.textureWidth, main.textureHeight);
        }
        if (!ProjectionUtil.isValid()) {
            return;
        }

        float tick = client.getRenderTickCounter().getTickProgress(true);
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();

        CameraRenderState cameraState = new CameraRenderState();
        cameraState.initialized = true;
        cameraState.pos = cameraPos;
        cameraState.blockPos = camera.getBlockPos();
        cameraState.entityPos = camera.getFocusedEntity() != null ? camera.getFocusedEntity().getEntityPos() : cameraPos;
        cameraState.orientation = new Quaternionf(camera.getRotation());

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(ProjectionUtil.positionMatrix());

        this.clearSceneBuffer();
        EntityMaskRenderer.begin();
        RenderSystem.backupProjectionMatrix();
        org.joml.Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        try {
            RenderSystem.setProjectionMatrix(ProjectionUtil.projectionSlice(), ProjectionType.PERSPECTIVE);
            for (Entity entity : client.world.getEntities()) {
                if (entity == null || !module.shouldRender(entity)) {
                    continue;
                }
                if (entity == client.player && client.options.getPerspective().isFirstPerson()) {
                    continue;
                }
                matrices.push();
                try {
                    EntityRenderState state = client.getEntityRenderDispatcher().getAndUpdateRenderState(entity, tick);
                    state.shadowRadius = 0.0f;
                    if (state.shadowPieces != null) {
                        state.shadowPieces.clear();
                    }
                    state.displayName = null;
                    double dx = MathHelper.lerp(tick, entity.lastRenderX, entity.getX()) - cameraPos.x;
                    double dy = MathHelper.lerp(tick, entity.lastRenderY, entity.getY()) - cameraPos.y;
                    double dz = MathHelper.lerp(tick, entity.lastRenderZ, entity.getZ()) - cameraPos.z;
                    client.getEntityRenderDispatcher().render(state, cameraState, dx, dy, dz, matrices, EntityMaskRenderer.queue());
                } catch (Throwable ignored) {
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            EntityMaskRenderer.end(this.sceneBuffer);
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }

        this.setupShader(mode, this.getShader(mode), this.sceneBuffer);
    }

    public void renderEntityShaderPass() {
        ShaderModule module = ShaderModule.INSTANCE;
        if (module == null || !module.isOn()) {
            return;
        }
        this.renderEntityShader(module.mode.getValue());
    }

    private void clearSceneBuffer() {
        GpuTextureView depth = this.sceneBuffer.useDepthAttachment ? this.sceneBuffer.getDepthAttachmentView() : null;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "alien_shader_scene_clear",
                this.sceneBuffer.getColorAttachmentView(),
                OptionalInt.of(0),
                depth,
                depth == null ? OptionalDouble.empty() : OptionalDouble.of(1.0))) {
        }
    }

    public ManagedShaderEffect getShader(Shader mode) {
        this.ensureManaged();
        ManagedShaderEffect effect = this.effects.get(mode);
        return effect != null ? effect : this.effects.get(Shader.Solid);
    }

    public ManagedShaderEffect getShaderOutline(Shader mode) {
        return this.getShader(mode);
    }

    private void ensureManaged() {
        if (this.effects.size() == Shader.values().length) {
            return;
        }
        ShaderEffectManager manager = ShaderEffectManager.getInstance();
        for (Shader shader : Shader.values()) {
            if (this.effects.containsKey(shader)) {
                continue;
            }
            Identifier id = Identifier.of("minecraft", "shaders/post/" + POST_EFFECTS[shader.ordinal()] + ".json");
            this.effects.put(shader, manager.manage(id));
        }
    }

    public boolean setupShader(Shader shader, ManagedShaderEffect effect, Framebuffer scene) {
        if (effect == null) {
            return false;
        }
        ShaderModule module = ShaderModule.INSTANCE;
        if (module == null) {
            return false;
        }
        Color color = module.fill.getValue();
        this.time = (float) timer.getMs() / 5.0f * module.speed.getValueFloat() * 0.004f;

        float width = (float) mc.getWindow().getScaledWidth();
        float height = (float) mc.getWindow().getScaledHeight();
        float radius = module.radius.getValueFloat();
        float quality = module.smoothness.getValueFloat();
        float divider = module.divider.getValueFloat();
        float maxSample = module.maxSample.getValueFloat();
        float alpha = (float) color.getAlpha() / 255.0f;

        effect.setScene(scene);

        switch (shader) {
            case Rainbow: {
                effect.setUniformValue("alpha2", alpha);
                this.putGlow(effect, radius, quality, divider, maxSample);
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                effect.setUniformValue("time", this.time);
                break;
            }
            case Gradient: {
                effect.setUniformValue("alpha2", alpha);
                this.putColor(effect, "rgb", module.smoke1.getValue());
                this.putColor(effect, "rgb1", module.smoke2.getValue());
                this.putColor(effect, "rgb2", module.smoke3.getValue());
                this.putColor(effect, "rgb3", module.smoke4.getValue());
                effect.setUniformValue("stepSize", module.step.getValueFloat() * 300.0f);
                this.putGlow(effect, radius, quality, divider, maxSample);
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                effect.setUniformValue("time", this.time * 300.0f);
                break;
            }
            case Smoke: {
                effect.setUniformValue("alpha1", alpha);
                this.putGlow(effect, radius, quality, divider, maxSample);
                Color first = module.smoke1.getValue();
                effect.setUniformValue(
                        "first",
                        (float) first.getRed() / 255.0f,
                        (float) first.getGreen() / 255.0f,
                        (float) first.getBlue() / 255.0f,
                        (float) first.getAlpha() / 255.0f);
                this.putColor(effect, "second", module.smoke2.getValue());
                this.putColor(effect, "third", module.smoke3.getValue());
                effect.setUniformValue("oct", module.octaves.getValueInt());
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                effect.setUniformValue("time", this.time);
                break;
            }
            case Solid: {
                effect.setUniformValue("mixFactor", alpha);
                effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0f);
                this.putGlow(effect, radius, quality, divider, maxSample);
                this.putColor(effect, "color", color);
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                break;
            }
            case Pulse: {
                effect.setUniformValue("mixFactor", alpha);
                effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0f);
                this.putGlow(effect, radius, quality, divider, maxSample);
                this.putColor(effect, "color", color);
                this.putColor(effect, "color2", module.pulse.getValue());
                effect.setUniformValue("time", this.time);
                effect.setUniformValue("size", module.pulseSpeed.getValueFloat());
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                break;
            }
            case Snow: {
                effect.setUniformValue(
                        "color",
                        (float) color.getRed() / 255.0f,
                        (float) color.getGreen() / 255.0f,
                        (float) color.getBlue() / 255.0f,
                        alpha);
                this.putGlow(effect, radius, quality, divider, maxSample);
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                effect.setUniformValue("time", this.time);
                break;
            }
            case Flow: {
                effect.setUniformValue("mixFactor", alpha);
                this.putGlow(effect, radius, quality, divider, maxSample);
                effect.setUniformValue("resolution", width, height, 0.0f, 0.0f);
                effect.setUniformValue("time", this.time);
                break;
            }
        }

        effect.render(mc.getRenderTickCounter().getTickProgress(true));
        this.composite(effect, "swap");
        return true;
    }

    private void composite(ManagedShaderEffect effect, String targetName) {
        if (effect == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null) {
            return;
        }
        PostEffect postEffect = effect.getShaderEffect();
        if (postEffect == null) {
            return;
        }
        Framebuffer output = postEffect.getFramebuffer(targetName);
        if (output == null || output.getColorAttachmentView() == null) {
            return;
        }
        try {
            output.drawBlit(main.getColorAttachmentView());
        } catch (Throwable throwable) {
            if (!this.compositeErrorLogged) {
                this.compositeErrorLogged = true;
                System.err.println("[Alien/Shader] composite failed: " + throwable);
            }
        }
    }

    private void putGlow(ManagedShaderEffect effect, float radius, float quality, float divider, float maxSample) {
        effect.setUniformValue("radius", radius);
        effect.setUniformValue("quality", quality);
        effect.setUniformValue("divider", divider);
        effect.setUniformValue("maxSample", maxSample);
    }

    private void putColor(ManagedShaderEffect effect, String name, Color color) {
        effect.setUniformValue(
                name,
                (float) color.getRed() / 255.0f,
                (float) color.getGreen() / 255.0f,
                (float) color.getBlue() / 255.0f,
                1.0f);
    }

    public void reloadShaders() {
        this.ensureManaged();
    }

    public boolean fullNullCheck() {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null) {
            return true;
        }
        if (this.sceneBuffer == null) {
            RenderSystem.assertOnRenderThread();
            this.sceneBuffer = new SimpleFramebuffer("alien_shader_scene", main.textureWidth, main.textureHeight, true);
            this.ensureManaged();
            return true;
        }
        return false;
    }

    public record RenderTask(Runnable task, Shader shader) {
    }

    public static enum Shader {
        Solid,
        Pulse,
        Smoke,
        Gradient,
        Snow,
        Flow,
        Rainbow;
    }
}

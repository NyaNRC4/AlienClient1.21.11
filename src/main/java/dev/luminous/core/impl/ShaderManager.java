package dev.luminous.core.impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;

public class ShaderManager
implements Wrapper {
    private static final int UBO_SIZE = 112;
    private static final String[] SHADER_NAMES = new String[]{"solid", "pulse", "smoke", "gradient", "snow", "flow", "rainbow"};
    private static final RenderPipeline[] PIPELINES = new RenderPipeline[Shader.values().length];
    static final Timer timer = new Timer();
    private static final List<RenderTask> tasks = new ArrayList<RenderTask>();
    public float time = 0.0f;
    private Framebuffer shaderBuffer;

    static {
        for (Shader shader : Shader.values()) {
            String name = SHADER_NAMES[shader.ordinal()];
            RenderPipeline pipeline = RenderPipeline.builder().withLocation(Identifier.of("alienclient", "pipeline/" + name)).withVertexShader(Identifier.of("alienclient", "core/blit")).withFragmentShader(Identifier.of("alienclient", "core/" + name)).withSampler("InSampler").withUniform("AlienParams", UniformType.UNIFORM_BUFFER).withBlend(BlendFunction.ENTITY_OUTLINE_BLIT).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build();
            PIPELINES[shader.ordinal()] = RenderPipelines.register(pipeline);
        }
    }

    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        tasks.forEach(t -> this.applyShader(t.task(), t.shader()));
        tasks.clear();
    }

    public void applyShader(Runnable runnable, Shader mode) {
        if (this.fullNullCheck()) {
            return;
        }
        RenderSystem.assertOnRenderThread();
        Framebuffer mcBuffer = MinecraftClient.getInstance().getFramebuffer();
        if (this.shaderBuffer.textureWidth != mcBuffer.textureWidth || this.shaderBuffer.textureHeight != mcBuffer.textureHeight) {
            this.shaderBuffer.resize(mcBuffer.textureWidth, mcBuffer.textureHeight);
        }
        GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = this.shaderBuffer.getColorAttachmentView();
        RenderSystem.outputDepthTextureOverride = this.shaderBuffer.getDepthAttachmentView();
        this.clearBuffer();
        try {
            runnable.run();
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
        }
        this.blitToScreen(mode, mcBuffer);
    }

    private void clearBuffer() {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "alien_shader_clear", this.shaderBuffer.getColorAttachmentView(), OptionalInt.of(0), this.shaderBuffer.getDepthAttachmentView(), OptionalDouble.of(1.0))) {
        }
    }

    private void blitToScreen(Shader mode, Framebuffer target) {
        GpuTextureView depthView = target.useDepthAttachment ? target.getDepthAttachmentView() : null;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "alien_shader_blit", target.getColorAttachmentView(), OptionalInt.empty(), depthView, OptionalDouble.empty()); GpuBuffer uniforms = this.buildUniforms(mode)) {
            pass.setPipeline(PIPELINES[mode.ordinal()]);
            pass.setUniform("AlienParams", uniforms);
            pass.bindTexture("InSampler", this.shaderBuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    private GpuBuffer buildUniforms(Shader shader) {
        ShaderModule module = ShaderModule.INSTANCE;
        Color color = module.fill.getValue();
        this.time = (float)timer.getMs() / 5.0f * module.speed.getValueFloat() * 0.004f;
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        float mixFactor = (float)color.getAlpha() / 255.0f;
        float glowRadius = module.radius.getValueFloat();
        float glowQuality = module.smoothness.getValueFloat();
        float glowDivider = module.divider.getValueFloat();
        float glowMaxSample = module.maxSample.getValueFloat();
        Std140Builder builder = Std140Builder.intoBuffer(ByteBuffer.allocateDirect(UBO_SIZE));
        switch (shader) {
            case Solid: {
                this.putColor(builder, color, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4((float)width, (float)height, 0.0f, mixFactor);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(module.alpha.getValueFloat() / 255.0f, 0.0f, 0.0f, 0.0f);
                break;
            }
            case Pulse: {
                this.putColor(builder, color, 0.0f);
                this.putColor(builder, module.pulse.getValue(), 0.0f);
                builder.putVec4((float)width, (float)height, this.time, mixFactor);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(module.alpha.getValueFloat() / 255.0f, module.pulseSpeed.getValueFloat(), 0.0f, 0.0f);
                break;
            }
            case Smoke: {
                this.putColor(builder, module.smoke1.getValue(), mixFactor);
                this.putColor(builder, module.smoke2.getValue(), 0.0f);
                builder.putVec4((float)width, (float)height, this.time, 0.0f);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                this.putColor(builder, module.smoke3.getValue(), (float)module.octaves.getValue());
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                break;
            }
            case Gradient: {
                this.putColor(builder, module.smoke1.getValue(), mixFactor);
                this.putColor(builder, module.smoke2.getValue(), 0.0f);
                builder.putVec4((float)width, (float)height, this.time * 30.0f, 0.0f);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                this.putColor(builder, module.smoke3.getValue(), (float)module.octaves.getValue());
                this.putColor(builder, module.smoke4.getValue(), 0.0f);
                builder.putVec4(module.step.getValueFloat(), 1.0f, 0.0f, 0.0f);
                break;
            }
            case Snow: {
                this.putColor(builder, color, mixFactor);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4((float)width, (float)height, this.time, 0.0f);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                break;
            }
            case Flow: {
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4((float)width, (float)height, this.time, mixFactor);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                break;
            }
            default: {
                builder.putVec4(0.0f, 0.0f, 0.0f, mixFactor);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4((float)width, (float)height, this.time, 0.0f);
                builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
            }
        }
        ByteBuffer data = builder.get();
        return RenderSystem.getDevice().createBuffer(() -> "alien_shader_uniforms", GpuBuffer.USAGE_UNIFORM, data);
    }

    private void putColor(Std140Builder builder, Color color, float w) {
        builder.putVec4((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, w);
    }

    public void reloadShaders() {
        this.shaderBuffer = null;
    }

    public boolean fullNullCheck() {
        if (this.shaderBuffer != null) {
            return false;
        }
        if (mc.getFramebuffer() == null) {
            return true;
        }
        RenderSystem.assertOnRenderThread();
        this.shaderBuffer = new SimpleFramebuffer("alien_shader", mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight, true);
        return true;
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

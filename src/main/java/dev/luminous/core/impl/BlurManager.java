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
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;

public class BlurManager
implements Wrapper {
    private static final int UBO_SIZE = 112;
    private static final RenderPipeline BLUR_PIPELINE = RenderPipelines.register(RenderPipeline.builder().withLocation(Identifier.of("alienclient", "pipeline/blur")).withVertexShader(Identifier.of("alienclient", "core/blit")).withFragmentShader(Identifier.of("alienclient", "core/blur")).withSampler("InSampler").withUniform("AlienParams", UniformType.UNIFORM_BUFFER).withBlend(BlendFunction.ENTITY_OUTLINE_BLIT).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build());
    private static final RenderPipeline COPY_PIPELINE = RenderPipelines.register(RenderPipeline.builder().withLocation(Identifier.of("alienclient", "pipeline/copy")).withVertexShader(Identifier.of("alienclient", "core/blit")).withFragmentShader(Identifier.of("alienclient", "core/copy")).withSampler("InSampler").withBlend(BlendFunction.ENTITY_OUTLINE_BLIT).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build());
    private Framebuffer blurBuffer;

    public void applyBlur(float radius, float startX, float startY, float width, float height) {
        RenderSystem.assertOnRenderThread();
        Framebuffer main = mc.getFramebuffer();
        if (main == null) {
            return;
        }
        if (this.blurBuffer == null) {
            this.blurBuffer = new SimpleFramebuffer("alien_blur", main.textureWidth, main.textureHeight, false);
        }
        if (this.blurBuffer.textureWidth != main.textureWidth || this.blurBuffer.textureHeight != main.textureHeight) {
            this.blurBuffer.resize(main.textureWidth, main.textureHeight);
        }
        float factor = (float)mc.getWindow().getScaleFactor();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "alien_blur_pass", this.blurBuffer.getColorAttachmentView(), OptionalInt.of(0)); GpuBuffer uniforms = this.buildUniforms(radius, startX * factor, startY * factor, width * factor, height * factor)) {
            pass.setPipeline(BLUR_PIPELINE);
            pass.setUniform("AlienParams", uniforms);
            pass.bindTexture("InSampler", main.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
        GpuTextureView depthView = main.useDepthAttachment ? main.getDepthAttachmentView() : null;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "alien_blur_copy", main.getColorAttachmentView(), OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(COPY_PIPELINE);
            pass.bindTexture("InSampler", this.blurBuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    private GpuBuffer buildUniforms(float radius, float x, float y, float w, float h) {
        Std140Builder builder = Std140Builder.intoBuffer(ByteBuffer.allocateDirect(UBO_SIZE));
        builder.putVec4(x, y, w, h);
        builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
        builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
        builder.putVec4(radius, 0.0f, 0.0f, 0.0f);
        builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
        builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
        builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
        return RenderSystem.getDevice().createBuffer(() -> "alien_blur_uniforms", GpuBuffer.USAGE_UNIFORM, builder.get());
    }
}

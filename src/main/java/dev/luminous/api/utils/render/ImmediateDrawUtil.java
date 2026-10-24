package dev.luminous.api.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class ImmediateDrawUtil {
	private static final ProjectionMatrix2 GUI_PROJECTION_MATRIX = new ProjectionMatrix2("alien_gui", 1000.0F, 11000.0F, true);
	private static float colorModulatorAlpha = 1.0F;
	private static final RenderPipeline POS_COLOR_QUADS_NO_DEPTH = registerPosColorNoDepth("quads", VertexFormat.DrawMode.QUADS);
	private static final RenderPipeline POS_COLOR_LINES_NO_DEPTH = registerPosColorNoDepth("lines", VertexFormat.DrawMode.LINES);
	private static final RenderPipeline POS_COLOR_DEBUG_LINES_NO_DEPTH = registerPosColorNoDepth("debug_lines", VertexFormat.DrawMode.DEBUG_LINES);
	private static final RenderPipeline POS_COLOR_DEBUG_LINE_STRIP_NO_DEPTH = registerPosColorNoDepth("debug_line_strip", VertexFormat.DrawMode.DEBUG_LINE_STRIP);
	private static final RenderPipeline POS_COLOR_TRIANGLE_FAN_NO_DEPTH = registerPosColorNoDepth("triangle_fan", VertexFormat.DrawMode.TRIANGLE_FAN);
	private static final RenderPipeline POS_COLOR_TRIANGLE_STRIP_NO_DEPTH = registerPosColorNoDepth("triangle_strip", VertexFormat.DrawMode.TRIANGLE_STRIP);
	private static final RenderPipeline POS_TEX_COLOR_QUADS_NO_DEPTH = registerPosTexColorNoDepth("quads", VertexFormat.DrawMode.QUADS);
	private static final RenderPipeline POS_TEX_COLOR_DEBUG_LINES_NO_DEPTH = registerPosTexColorNoDepth("debug_lines", VertexFormat.DrawMode.DEBUG_LINES);

	private static final RenderPipeline LINES_WIDE_NO_DEPTH = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
					.withLocation(Identifier.of("alienclient", "pipeline/lines_wide_no_depth"))
					.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
					.withDepthWrite(false)
					.build()
	);

	private ImmediateDrawUtil() {
	}

	public static void setColorModulatorAlpha(float alpha) {
		colorModulatorAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
	}

	public static void resetColorModulator() {
		colorModulatorAlpha = 1.0F;
	}

	private static RenderPipeline registerPosColorNoDepth(String name, VertexFormat.DrawMode drawMode) {
		return RenderPipelines.register(
				RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
						.withLocation(Identifier.of("alienclient", "pipeline/pos_color_no_depth_" + name))
						.withCull(false)
						.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
						.withDepthWrite(false)
						.withVertexFormat(VertexFormats.POSITION_COLOR, drawMode)
						.build()
		);
	}

	private static RenderPipeline registerPosTexColorNoDepth(String name, VertexFormat.DrawMode drawMode) {
		return RenderPipelines.register(
				RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
						.withLocation(Identifier.of("alienclient", "pipeline/pos_tex_color_no_depth_" + name))
						.withCull(false)
						.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
						.withDepthWrite(false)
						.withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, drawMode)
						.build()
		);
	}

	public static void drawGui(BufferBuilder builder) {
		drawGui(builder, null);
	}

	public static void drawGuiTextured(BufferBuilder builder, GpuTextureView textureView) {
		drawGui(builder, textureView);
	}

	private static void drawGui(BufferBuilder builder, GpuTextureView textureView) {
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = client.getWindow();
		RenderSystem.backupProjectionMatrix();
		try {
			RenderSystem.setProjectionMatrix(
					GUI_PROJECTION_MATRIX.set((float) window.getFramebufferWidth() / window.getScaleFactor(), (float) window.getFramebufferHeight() / window.getScaleFactor()),
					ProjectionType.ORTHOGRAPHIC
			);
			draw(builder, new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F), textureView);
		} finally {
			RenderSystem.restoreProjectionMatrix();
		}
	}

	public static void drawWorld(BufferBuilder builder) {
		drawWorld(builder, null);
	}

	public static void drawWorldTextured(BufferBuilder builder, GpuTextureView textureView) {
		drawWorld(builder, textureView);
	}

	private static void drawWorld(BufferBuilder builder, GpuTextureView textureView) {
		draw(builder, new Matrix4f(), textureView);
	}

	private static void draw(BufferBuilder builder, Matrix4f modelView, GpuTextureView textureView) {
		try (BuiltBuffer builtBuffer = builder.end()) {
			BuiltBuffer.DrawParameters parameters = builtBuffer.getDrawParameters();
			RenderPipeline pipeline = pipelineFor(parameters.mode(), parameters.format());
			Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
			RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(parameters.mode());
			GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(parameters.indexCount());
			VertexFormat.IndexType indexType = shapeIndexBuffer.getIndexType();

			GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
					.write(modelView, new Vector4f(1.0F, 1.0F, 1.0F, colorModulatorAlpha), new Vector3f(), new Matrix4f());
			GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(builtBuffer.getBuffer());

			GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
					? RenderSystem.outputColorTextureOverride
					: framebuffer.getColorAttachmentView();

			final GpuTextureView depthView = null;

			try (RenderPass renderPass = RenderSystem.getDevice()
					.createCommandEncoder()
					.createRenderPass(() -> "alien_immediate_nodepth", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
				RenderSystem.bindDefaultUniforms(renderPass);
				renderPass.setUniform("DynamicTransforms", dynamicTransforms);
				renderPass.setPipeline(pipeline);
				if (textureView != null) {
					renderPass.bindTexture("Sampler0", textureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
				}
				renderPass.setVertexBuffer(0, vertexBuffer);
				renderPass.setIndexBuffer(indexBuffer, indexType);
				renderPass.drawIndexed(0, 0, parameters.indexCount(), 1);
			}
		}
	}

	private static RenderPipeline pipelineFor(VertexFormat.DrawMode drawMode, VertexFormat format) {
		if (format == VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH) {
			return LINES_WIDE_NO_DEPTH;
		}
		if (format == VertexFormats.POSITION_TEXTURE_COLOR) {
			return switch (drawMode) {
				case QUADS -> POS_TEX_COLOR_QUADS_NO_DEPTH;
				case DEBUG_LINES -> POS_TEX_COLOR_DEBUG_LINES_NO_DEPTH;
				default -> throw new IllegalArgumentException("Unsupported textured draw mode: " + drawMode);
			};
		}
		return switch (drawMode) {
			case QUADS -> POS_COLOR_QUADS_NO_DEPTH;
			case LINES -> POS_COLOR_LINES_NO_DEPTH;
			case DEBUG_LINES -> POS_COLOR_DEBUG_LINES_NO_DEPTH;
			case DEBUG_LINE_STRIP -> POS_COLOR_DEBUG_LINE_STRIP_NO_DEPTH;
			case TRIANGLE_FAN -> POS_COLOR_TRIANGLE_FAN_NO_DEPTH;
			case TRIANGLE_STRIP -> POS_COLOR_TRIANGLE_STRIP_NO_DEPTH;
			default -> throw new IllegalArgumentException("Unsupported no-depth draw mode: " + drawMode);
		};
	}
}

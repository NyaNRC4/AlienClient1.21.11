package satin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 1.21.11 后处理运行时（Satin 的替代实现）。
 *
 * <p>旧版 Satin 依赖 {@code PostEffectProcessor} 的 {@code PostEffectPass#getProgram} +
 * {@code GlUniform#set}，这套接口在 1.21.11 已被删除。这里不复用原版
 * {@code PostEffectProcessor}（它的 uniform 在解析时就固化进 UBO，无法每帧改），
 * 而是直接使用 {@code RenderPipeline} + {@code RenderPass} 自建整屏三角形绘制：
 *
 * <ul>
 *   <li>每个 pass 对应一个 {@link RenderPipeline}（顶点格式为空、TRIANGLES）；</li>
 *   <li>uniform 以 std140 UBO 形式绑定，每帧写入 {@code USAGE_UNIFORM|USAGE_COPY_DST} 缓冲；</li>
 *   <li>输入既可以是内部 target、主 framebuffer、外部 framebuffer（离屏场景），
 *       也可以是外部注册的动态纹理（用户 shader.png）。</li>
 * </ul>
 *
 * <p>JSON 格式（{@code assets/<ns>/shaders/post/*.json}）：
 *
 * <pre>
 * {
 *   "targets": ["swap"],
 *   "passes": [{
 *     "vertex_shader": "minecraft:post/screenquad",
 *     "fragment_shader": "minecraft:post/blur",
 *     "inputs": [{"sampler": "Diffuse", "source": "scene"},
 *                {"sampler": "Image",   "source": "texture:user"}],
 *     "output": "target:swap",
 *     "block": "BlurConfig",
 *     "uniforms": [{"name": "texelSize", "type": "vec2", "value": [1.0, 1.0]},
 *                  {"name": "radius",    "type": "float", "value": 4.0}]
 *   }]
 * }
 * </pre>
 *
 * <p>source 取值：{@code main} / {@code scene} / {@code target:<name>} / {@code texture:<name>}；
 * output 取值：{@code main} / {@code target:<name>}。
 */
public final class PostEffect implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();

   private final Identifier id;
   private final List<Pass> passes = new ArrayList<>();
   private final Map<String, SimpleFramebuffer> targets = new LinkedHashMap<>();
   private final Map<String, GpuTextureView> textureInputs = new HashMap<>();
   private final Map<String, UniformDef> uniforms = new HashMap<>();
   private final Map<String, float[]> values = new HashMap<>();

   private Framebuffer scene;
   private int width = -1;
   private int height = -1;
   private int failures;
   private boolean failed;
   private boolean loggedFirstRender;

   private PostEffect(Identifier id) {
      this.id = id;
   }

   public static PostEffect load(ResourceFactory factory, Identifier id) throws IOException {
      PostEffect effect = new PostEffect(id);
      Identifier file = id.withPath(path -> path.endsWith(".json") ? path : path + ".json");
      Resource resource = factory.getResource(file).orElse(null);
      if (resource == null) {
         throw new IOException("Post effect json not found: " + file);
      }
      try (Reader reader = resource.getReader()) {
         effect.read(JsonParser.parseReader(reader).getAsJsonObject());
      }
      return effect;
   }

   private void read(JsonObject root) throws IOException {
      JsonArray targetList = root.getAsJsonArray("targets");
      if (targetList != null) {
         for (JsonElement element : targetList) {
            this.targets.put(element.getAsString(), null);
         }
      }

      JsonArray passList = root.getAsJsonArray("passes");
      if (passList == null) {
         return;
      }

      int index = 0;
      for (JsonElement element : passList) {
         JsonObject object = element.getAsJsonObject();
         String vertex = object.get("vertex_shader").getAsString();
         String fragment = object.get("fragment_shader").getAsString();
         String output = object.get("output").getAsString();
         String block = object.has("block") ? object.get("block").getAsString() : null;
         boolean blend = object.has("blend") && object.get("blend").getAsBoolean();

         List<Input> inputs = new ArrayList<>();
         JsonArray inputArray = object.getAsJsonArray("inputs");
         if (inputArray != null) {
            for (JsonElement inputElement : inputArray) {
               JsonObject inputObject = inputElement.getAsJsonObject();
               inputs.add(new Input(
                  inputObject.get("sampler").getAsString(),
                  inputObject.get("source").getAsString(),
                  inputObject.has("bilinear") && inputObject.get("bilinear").getAsBoolean()
               ));
            }
         }

         List<UniformDef> defs = new ArrayList<>();
         JsonArray uniformArray = object.getAsJsonArray("uniforms");
         if (uniformArray != null) {
            for (JsonElement uniformElement : uniformArray) {
               JsonObject uniformObject = uniformElement.getAsJsonObject();
               String name = uniformObject.get("name").getAsString();
               String type = uniformObject.get("type").getAsString();
               int count = typeCount(type);
               float[] value = new float[count];
               JsonElement defaultElement = uniformObject.get("value");
               if (defaultElement != null && defaultElement.isJsonArray()) {
                  JsonArray defaultArray = defaultElement.getAsJsonArray();
                  for (int i = 0; i < Math.min(count, defaultArray.size()); i++) {
                     value[i] = defaultArray.get(i).getAsFloat();
                  }
               } else if (defaultElement != null && defaultElement.isJsonPrimitive()) {
                  value[0] = defaultElement.getAsFloat();
               }
               UniformDef def = new UniformDef(name, type, block, value);
               defs.add(def);
               this.uniforms.putIfAbsent(name, def);
               this.values.putIfAbsent(name, value.clone());
            }
         }

         // 1.21.11：完全按工程里已验证可用的写法建管线（见旧的 BlurManager / ImmediateDrawUtil / CompatRender）：
         // 不用 RenderPipelines 的 snippet —— snippet 会带进 InSampler 之类的预声明采样器/状态，
         // 与这里自己的 sampler/uniform 声明冲突；所有状态显式给出。
         RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(this.id.withSuffixedPath("/" + index++))
            .withVertexShader(Identifier.of(vertex))
            .withFragmentShader(Identifier.of(fragment))
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false);
         for (Input input : inputs) {
            builder.withSampler(input.sampler + "Sampler");
         }
         if (block != null) {
            builder.withUniform(block, UniformType.UNIFORM_BUFFER);
         }
         if (blend) {
            builder.withBlend(BlendFunction.TRANSLUCENT);
         } else {
            builder.withoutBlend();
         }

         // 1.21.11：自定义 RenderPipeline 需要注册到 RenderPipelines 才能被 RenderPass 解析/编译。
         // 注册失败（例如同一 location 重复注册）时仍然保留未注册的实例，避免整条效果链失效。
         RenderPipeline built = builder.build();
         RenderPipeline pipeline = built;
         try {
            RenderPipelines.register(built);
         } catch (Throwable throwable) {
            LOGGER.warn("Could not register post effect pipeline for {}", this.id, throwable);
         }
         Pass pass = new Pass(pipeline, inputs, output, block, defs, blend);
         if (block != null) {
            pass.computeBufferSize();
         }
         this.passes.add(pass);
      }
   }

   private static int typeCount(String type) {
      return switch (type) {
         case "float", "int" -> 1;
         case "vec2", "ivec2" -> 2;
         case "vec3", "ivec3" -> 3;
         case "vec4", "ivec4" -> 4;
         case "matrix4x4" -> 16;
         default -> 1;
      };
   }

   public Identifier getId() {
      return this.id;
   }

   public boolean isFailed() {
      return this.failed;
   }

   /** 外部离屏场景（VY 里画进 FramebufferFramebuffer3 的那些实体）。 */
   public void setScene(Framebuffer framebuffer) {
      this.scene = framebuffer;
   }

   /** 用户图片之类的动态纹理。 */
   public void setTexture(String name, GpuTextureView view) {
      if (view == null) {
         this.textureInputs.remove(name);
      } else {
         this.textureInputs.put(name, view);
      }
   }

   public void setUniform(String name, float[] value) {
      UniformDef def = this.uniforms.get(name);
      if (def == null) {
         return;
      }
      float[] target = this.values.get(name);
      if (target == null || target.length != value.length) {
         this.values.put(name, value.clone());
      } else {
         System.arraycopy(value, 0, target, 0, value.length);
      }
   }

   public float[] getUniform(String name) {
      return this.values.get(name);
   }

   public Framebuffer getFramebuffer(String name) {
      return this.targets.get(name);
   }

   /** 按当前窗口尺寸创建/缩放内部 target。 */
   public void setup(int framebufferWidth, int framebufferHeight) {
      if (framebufferWidth <= 0 || framebufferHeight <= 0) {
         return;
      }
      if (this.width == framebufferWidth && this.height == framebufferHeight) {
         return;
      }
      this.width = framebufferWidth;
      this.height = framebufferHeight;

      for (String name : this.targets.keySet()) {
         SimpleFramebuffer framebuffer = this.targets.get(name);
         if (framebuffer == null) {
            framebuffer = new SimpleFramebuffer("alien post " + this.id.getPath() + "/" + name, framebufferWidth, framebufferHeight, false);
            this.targets.put(name, framebuffer);
         } else {
            framebuffer.resize(framebufferWidth, framebufferHeight);
         }
      }
   }

   /** 执行所有 pass；调用方负责把最终 target 合成回主 framebuffer（menu 除外，它在 JSON 里自己输出到 main）。 */
   public void render(float tickDelta) {
      if (this.failed || this.passes.isEmpty()) {
         return;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      Framebuffer main = client.getFramebuffer();
      if (main == null) {
         return;
      }
      this.setup(main.textureWidth, main.textureHeight);
      if (this.width <= 0) {
         return;
      }

      try {
         for (Pass pass : this.passes) {
            this.runPass(pass);
         }
         this.failures = 0;
      } catch (Throwable throwable) {
         // 不要第一帧出错就永久禁用：先重试若干次，方便贴图/管线晚一帧才就绪的情况自愈；
         // 连续失败到阈值才真正关掉，避免刷屏和每帧抛异常。
         this.failures++;
         if (this.failures <= 20) {
            LOGGER.error("Alien post effect {} failed (attempt {}/20)", this.id, this.failures, throwable);
         }
         if (this.failures >= 20) {
            this.failed = true;
            LOGGER.error("Alien post effect {} failed repeatedly; disabling it", this.id);
         }
      }
   }

   private void runPass(Pass pass) {
      Framebuffer output = this.resolveFramebuffer(pass.output);
      if (output == null || output.getColorAttachmentView() == null) {
         return;
      }

      // 1.21.11：uniform 每帧用「带数据的一次性 GpuBuffer」上传（和旧 BlurManager/ShaderManager 里
      // 已经跑通的写法一致）。之前那种"持久 buffer + writeToBuffer"依赖命令编码器的写入时序，
      // 一旦被丢弃/重排，shader 里读到的就是全 0：表现正好是——效果全透明、模糊区域变成 0 尺寸（等于没效果）。
      GpuBuffer uniforms = pass.block == null ? null : this.buildUniformBuffer(pass);
      try (RenderPass renderPass = RenderSystem.getDevice()
         .createCommandEncoder()
         .createRenderPass(() -> "alien post " + this.id, output.getColorAttachmentView(), OptionalInt.empty())) {
         renderPass.setPipeline(pass.pipeline);
         if (uniforms != null) {
            renderPass.setUniform(pass.block, uniforms);
         }
         for (Input input : pass.inputs) {
            GpuTextureView view = this.resolveTexture(input);
            if (view == null) {
               // 未绑定的 sampler 不能留空，否则 RenderPass 可能报错；退回主 framebuffer。
               Framebuffer fallback = MinecraftClient.getInstance().getFramebuffer();
               view = fallback == null ? null : fallback.getColorAttachmentView();
            }
            if (view == null) {
               continue;
            }
            renderPass.bindTexture(
               input.sampler + "Sampler",
               view,
               RenderSystem.getSamplerCache().get(input.bilinear ? FilterMode.LINEAR : FilterMode.NEAREST)
            );
         }

         renderPass.draw(0, 3);
      } finally {
         if (uniforms != null) {
            uniforms.close();
         }
      }

      if (!this.loggedFirstRender) {
         this.loggedFirstRender = true;
         LOGGER.info("Alien post effect {} first render: block={}, uniforms={}B, output={}",
            this.id, pass.block, pass.bufferSize, pass.output);
      }
   }

   private GpuBuffer buildUniformBuffer(Pass pass) {
      int size = Math.max(pass.bufferSize, 16);
      ByteBuffer data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
      Std140Builder builder = Std140Builder.intoBuffer(data);
      for (UniformDef def : pass.uniforms) {
         float[] value = this.values.get(def.name);
         if (value == null) {
            value = def.defaultValue;
         }
         writeUniform(builder, def.type, value);
      }
      return RenderSystem.getDevice().createBuffer(() -> "alien post uniforms " + pass.block, GpuBuffer.USAGE_UNIFORM, builder.get());
   }

   private static void writeUniform(Std140Builder builder, String type, float[] value) {
      switch (type) {
         case "int" -> builder.putInt((int) at(value, 0));
         case "ivec2" -> builder.putIVec2((int) at(value, 0), (int) at(value, 1));
         case "ivec3" -> builder.putIVec3((int) at(value, 0), (int) at(value, 1), (int) at(value, 2));
         case "ivec4" -> builder.putIVec4((int) at(value, 0), (int) at(value, 1), (int) at(value, 2), (int) at(value, 3));
         case "vec2" -> builder.putVec2(at(value, 0), at(value, 1));
         case "vec3" -> builder.putVec3(at(value, 0), at(value, 1), at(value, 2));
         case "vec4" -> builder.putVec4(at(value, 0), at(value, 1), at(value, 2), at(value, 3));
         case "matrix4x4" -> builder.putMat4f(new org.joml.Matrix4f(
            at(value, 0), at(value, 1), at(value, 2), at(value, 3),
            at(value, 4), at(value, 5), at(value, 6), at(value, 7),
            at(value, 8), at(value, 9), at(value, 10), at(value, 11),
            at(value, 12), at(value, 13), at(value, 14), at(value, 15)
         ));
         default -> builder.putFloat(at(value, 0));
      }
   }

   private static float at(float[] value, int index) {
      return value != null && index < value.length ? value[index] : 0.0F;
   }

   private Framebuffer resolveFramebuffer(String source) {
      if ("main".equals(source)) {
         return MinecraftClient.getInstance().getFramebuffer();
      }
      if ("scene".equals(source)) {
         return this.scene;
      }
      if (source.startsWith("target:")) {
         return this.targets.get(source.substring("target:".length()));
      }
      return null;
   }

   private GpuTextureView resolveTexture(Input input) {
      String source = input.source;
      if ("main".equals(source)) {
         Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
         return main == null ? null : main.getColorAttachmentView();
      }
      if ("scene".equals(source)) {
         return this.scene == null ? null : this.scene.getColorAttachmentView();
      }
      if (source.startsWith("target:")) {
         Framebuffer framebuffer = this.targets.get(source.substring("target:".length()));
         return framebuffer == null ? null : framebuffer.getColorAttachmentView();
      }
      if (source.startsWith("texture:")) {
         return this.textureInputs.get(source.substring("texture:".length()));
      }
      return null;
   }

   @Override
   public void close() {
      for (SimpleFramebuffer framebuffer : this.targets.values()) {
         if (framebuffer != null) {
            framebuffer.delete();
         }
      }
      this.targets.replaceAll((name, framebuffer) -> null);
      this.width = -1;
      this.height = -1;
      for (Pass pass : this.passes) {
         pass.close();
      }
   }

   private static final class Input {
      final String sampler;
      final String source;
      final boolean bilinear;

      Input(String sampler, String source, boolean bilinear) {
         this.sampler = sampler;
         this.source = source;
         this.bilinear = bilinear;
      }
   }

   private record UniformDef(String name, String type, String block, float[] defaultValue) {
   }

   private static final class Pass {
      final RenderPipeline pipeline;
      final List<Input> inputs;
      final String output;
      final String block;
      final List<UniformDef> uniforms;
      final boolean blend;

      int bufferSize;

      Pass(RenderPipeline pipeline, List<Input> inputs, String output, String block, List<UniformDef> uniforms, boolean blend) {
         this.pipeline = pipeline;
         this.inputs = inputs;
         this.output = output;
         this.block = block;
         this.uniforms = uniforms;
         this.blend = blend;
      }

      /**
       * 只算 std140 大小，不在解析阶段创建 GPU 资源（解析可能发生在 device 就绪之前）。
       * 真正的 uniform buffer 每帧在 {@link #buildUniformBuffer(Pass)} 里新建。
       */
      void computeBufferSize() {
         Std140SizeCalculator calculator = new Std140SizeCalculator();
         for (UniformDef def : this.uniforms) {
            switch (def.type) {
               case "int" -> calculator.putInt();
               case "ivec2" -> calculator.putIVec2();
               case "ivec3" -> calculator.putIVec3();
               case "ivec4" -> calculator.putIVec4();
               case "vec2" -> calculator.putVec2();
               case "vec3" -> calculator.putVec3();
               case "vec4" -> calculator.putVec4();
               case "matrix4x4" -> calculator.putMat4f();
               default -> calculator.putFloat();
            }
         }
         this.bufferSize = Math.max(16, (calculator.get() + 15) & ~15);
      }

      void close() {
      }
   }
}

package satin;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import satin.uniform.SamplerUniform;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ResettableManagedCoreShader extends ResettableManagedShaderBase implements ManagedCoreShader {
   private final Consumer initCallback;
   private final VertexFormat vertexFormat;
   private final Map managedSamplers = new HashMap();

   public ResettableManagedCoreShader(Identifier id, VertexFormat vertexFormat2, Consumer consumer) {
      super(id);
      this.vertexFormat = vertexFormat2;
      this.initCallback = consumer;
   }

   protected ShaderProgram parseShader(ResourceFactory resourceFactory) throws IOException {
      // TODO 1.21.11: 核心着色器改为 RenderPipeline 注册（见 pw.hachimi.client.render.CompatRender），
      // 旧的 FabricShaderProgram 已被 Fabric API 移除，这里返回 null 表示不再单独加载。
      return null;
   }

   @Override
   public void setup(int n, int n2) {
      if (this.shader == null) {
         return;
      }

      for (ManagedUniformBase managedUniformBase : (Iterable<ManagedUniformBase>)this.getManagedUniforms()) {
         this.setupUniform(managedUniformBase, (ShaderProgram)this.shader);
      }

      this.initCallback.accept(this);
   }

   @Override
   public ShaderProgram getProgram() {
      return (ShaderProgram)this.shader;
   }

   protected boolean setupUniform(ManagedUniformBase managedUniformBase, ShaderProgram shaderProgram) {
      return managedUniformBase.findUniformTarget(shaderProgram);
   }

   @Override
   public SamplerUniform findSampler(String str) {
      return (SamplerUniform)this.manageUniform(this.managedSamplers, new Function() {
         @Override
         public Object apply(Object str) {
            return new ManagedSamplerUniformV1((String)str);
         }
      }, str, "sampler");
   }

   @Override
   protected void logInitError(IOException ioException) {
      LogUtils.getLogger().error("Could not create shader program {}", this.getLocation(), ioException);
   }

   @Override
   protected boolean setupUniform(ManagedUniformBase managedUniformBase, AutoCloseable autoCloseable) {
      return this.setupUniform(managedUniformBase, (ShaderProgram)autoCloseable);
   }

   @Override
   protected AutoCloseable parseShader(ResourceFactory resourceFactory, MinecraftClient client2, Identifier id) throws IOException {
      return this.parseShader(resourceFactory);
   }
}

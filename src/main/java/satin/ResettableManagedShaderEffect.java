package satin;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import satin.uniform.SamplerUniformV2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 1.21.11 版 Satin 效果封装。
 *
 * <p>只保留旧 API 的形状（setUniformValue / render / getTarget），实现全部交给
 * {@link PostEffect}（自建 RenderPipeline + UBO）。旧的 {@code PostEffectProcessor}
 * 只在 1.21.1 存在，这里不再引用。
 */
public final class ResettableManagedShaderEffect extends ResettableManagedShaderBase implements ManagedShaderEffect {
   private final Consumer initCallback;
   private final Map<String, FramebufferWrapper> managedTargets = new HashMap<>();

   public ResettableManagedShaderEffect(Identifier id, Consumer consumer) {
      super(id);
      this.initCallback = consumer;
   }

   @Override
   public PostEffect getShaderEffect() {
      return this.getShaderOrLog();
   }

   @Override
   protected AutoCloseable parseShader(ResourceFactory factory, MinecraftClient client, Identifier id) throws IOException {
      try {
         return PostEffect.load(factory, id);
      } catch (IOException ioException) {
         throw ioException;
      } catch (Throwable throwable) {
         throw new IOException("Failed to parse post effect " + id, throwable);
      }
   }

   @Override
   public void setup(int width, int height) {
      PostEffect effect = this.getShaderOrLog();
      if (effect == null) {
         return;
      }

      effect.setup(width, height);
      this.initCallback.accept(this);
   }

   @Override
   public void render(float tickDelta) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.render(tickDelta);
      }
   }

   @Override
   public ManagedFramebuffer getTarget(String name) {
      return this.managedTargets.computeIfAbsent(name, key -> new FramebufferWrapper(key, () -> {
         PostEffect effect = this.getShaderOrLog();
         return effect == null ? null : effect.getFramebuffer(key);
      }));
   }

   @Override
   public void setUniformValue(String name, int value) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setUniform(name, new float[]{value});
      }
   }

   @Override
   public void setUniformValue(String name, float value) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setUniform(name, new float[]{value});
      }
   }

   @Override
   public void setUniformValue(String name, float x, float y) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setUniform(name, new float[]{x, y});
      }
   }

   @Override
   public void setUniformValue(String name, float x, float y, float z) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setUniform(name, new float[]{x, y, z});
      }
   }

   @Override
   public void setUniformValue(String name, float x, float y, float z, float w) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setUniform(name, new float[]{x, y, z, w});
      }
   }

   @Override
   public void setSamplerTexture(String name, GpuTextureView view) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setTexture(name, view);
      }
   }

   @Override
   public SamplerUniformV2 findSampler(String str) {
      return new ManagedSamplerUniformV2(str);
   }

   @Override
   protected boolean setupUniform(ManagedUniformBase managedUniformBase, AutoCloseable shader) {
      return false;
   }

   @Override
   protected void logInitError(IOException ioException) {
      com.mojang.logging.LogUtils.getLogger().error("Could not create screen shader {}", this.getLocation(), ioException);
   }

   private PostEffect getShaderOrLog() {
      if (!this.isInitialized() && !this.isErrored()) {
         this.initializeOrLog(MinecraftClient.getInstance().getResourceManager());
      }

      return (PostEffect) this.shader;
   }

   /** 把离屏场景（实体描边缓冲 / 手部离屏缓冲）作为 Diffuse 输入交给效果。 */
   @Override
   public void setScene(Framebuffer framebuffer) {
      PostEffect effect = this.getShaderOrLog();
      if (effect != null) {
         effect.setScene(framebuffer);
      }
   }
}

package satin;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.Framebuffer;
import satin.uniform.SamplerUniform;
import satin.uniform.SamplerUniformV2;
import satin.uniform.UniformFinder;

public interface ManagedShaderEffect extends UniformFinder {
   PostEffect getShaderEffect();

   void release();

   /** 1.21.11：把外部离屏场景（实体描边缓冲 / 手部离屏缓冲）交给效果作为 Diffuse 输入。 */
   void setScene(Framebuffer var1);

   void render(float var1);

   ManagedFramebuffer getTarget(String var1);

   void setUniformValue(String var1, int var2);

   void setUniformValue(String var1, float var2);

   void setUniformValue(String var1, float var2, float var3);

   void setUniformValue(String var1, float var2, float var3, float var4);

   void setUniformValue(String var1, float var2, float var3, float var4, float var5);

   /** 1.21.11：绑定动态纹理（用户 shader.png）到某个 sampler，取代旧的 glActiveTexture/glBindTexture。 */
   void setSamplerTexture(String var1, GpuTextureView var2);

   SamplerUniformV2 findSampler(String var1);

   default SamplerUniform findSampler_2(String str) {
      return this.findSampler(str);
   }
}

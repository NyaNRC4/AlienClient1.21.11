package satin;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;
import satin.uniform.SamplerUniformV2;

import java.util.function.IntSupplier;

/** 见 ManagedSamplerUniformBase 的迁移说明：采样器注入在新管线里不再可用。 */
public final class ManagedSamplerUniformV2 extends ManagedSamplerUniformBase implements SamplerUniformV2 {
   public ManagedSamplerUniformV2(String str) {
      super(str);
   }

   @Override
   public void set(AbstractTexture abstractTexture) {
      this.set((Object) abstractTexture);
   }

   @Override
   public void set(Framebuffer framebuffer) {
      this.set((Object) framebuffer);
   }

   @Override
   public void set(int n) {
      this.set(Integer.valueOf(n));
   }

   @Override
   protected void set(Object obj) {
      this.cachedValue = obj;
   }

   @Override
   public void set(IntSupplier intSupplier) {
      this.set((Object) intSupplier);
   }
}

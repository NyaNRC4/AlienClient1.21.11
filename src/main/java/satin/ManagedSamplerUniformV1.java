package satin;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;

/** 见 ManagedSamplerUniformBase 的迁移说明：采样器注入在新管线里不再可用。 */
public final class ManagedSamplerUniformV1 extends ManagedSamplerUniformBase {
   public ManagedSamplerUniformV1(String str) {
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
}

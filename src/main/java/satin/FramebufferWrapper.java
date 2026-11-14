package satin;

import net.minecraft.client.gl.Framebuffer;

import java.util.function.Supplier;

/**
 * 1.21.11：Satin 的 target 现在是 {@link PostEffect} 自己管理的 {@code SimpleFramebuffer}，
 * 不再从 {@code PostEffectProcessor#getSecondaryTarget} 里取。这里只做一层转发。
 */
public final class FramebufferWrapper implements ManagedFramebuffer {
   private final String name;
   private final Supplier<Framebuffer> supplier;

   FramebufferWrapper(String name, Supplier<Framebuffer> supplier) {
      this.name = name;
      this.supplier = supplier;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public Framebuffer getFramebuffer() {
      return this.supplier.get();
   }

   @Override
   public void beginWrite(boolean flag) {
      // 1.21.11 的绘制目标由 RenderPass / OutputTarget 指定，不能再切换 FBO。
   }

   @Override
   public void draw() {
   }

   @Override
   public void draw(int width, int height, boolean flag) {
   }

   @Override
   public void clear() {
   }

   @Override
   public void clear(boolean flag) {
   }
}

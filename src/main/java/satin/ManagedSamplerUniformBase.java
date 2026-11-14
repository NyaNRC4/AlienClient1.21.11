package satin;

import net.minecraft.client.gl.ShaderProgram;
import satin.uniform.SamplerUniform;

import java.util.List;

/**
 * 1.21.11 迁移说明：旧的 {@code JsonEffectShaderProgram} 与 {@code PostEffectPass#getProgram()}
 * 已被删除，后处理管线改成 RenderPipeline + PostEffectPass，采样器绑定不能再从外部注入。
 * 这里保留原有 API 形状，但不再绑定任何目标（后处理效果本次迁移暂未恢复，见 docs/PORT_1.21.11.md）。
 */
public abstract class ManagedSamplerUniformBase extends ManagedUniformBase implements SamplerUniform {
   protected SamplerAccess[] targets = new SamplerAccess[0];
   protected int[] locations = new int[0];
   protected Object cachedValue;

   public ManagedSamplerUniformBase(String str) {
      super(str);
   }

   @Override
   public boolean findUniformTargets(List list) {
      this.targets = new SamplerAccess[0];
      this.locations = new int[0];
      return false;
   }

   @Override
   public boolean findUniformTarget(ShaderProgram shaderProgram) {
      this.targets = new SamplerAccess[0];
      this.locations = new int[0];
      return false;
   }

   protected abstract void set(Object var1);
}

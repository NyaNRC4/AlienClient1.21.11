package satin;

import net.minecraft.client.gl.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import satin.uniform.*;

import java.util.List;

/**
 * 1.21.11 迁移说明：旧的 {@code PostEffectPass#getProgram()} / {@code GlUniform#set/intData}
 * 已被删除，后处理参数只能在 PostEffectPipeline 里静态声明，无法在运行时注入。
 * 这个类保留原有 API 形状（及各 Uniform 接口的实现），实际写入变成空操作；
 * 等后处理管线迁移完成后，需要在 PostEffectPass 上重新实现 uniform 上传。
 */
public final class ManagedUniform
   extends ManagedUniformBase
   implements Uniform1i,
   Uniform2i,
   Uniform3i,
   Uniform4i,
   Uniform1f,
   Uniform2f,
   Uniform3f,
   Uniform4f,
   UniformMat4 {
   private final int count;
   private int i0;
   private int i1;
   private int i2;
   private int i3;
   private float f0;
   private float f1;
   private float f2;
   private float f3;

   public ManagedUniform(String str, int n) {
      super(str);
      this.count = n;
   }

   @Override
   public boolean findUniformTargets(List list) {
      return false;
   }

   @Override
   public boolean findUniformTarget(ShaderProgram shaderProgram) {
      return false;
   }

   @Override
   public void set(int n) {
      this.i0 = n;
   }

   @Override
   public void set(int n, int n2) {
      this.i0 = n;
      this.i1 = n2;
   }

   @Override
   public void set(int n, int n2, int n3) {
      this.i0 = n;
      this.i1 = n2;
      this.i2 = n3;
   }

   @Override
   public void set(int n, int n2, int n3, int n4) {
      this.i0 = n;
      this.i1 = n2;
      this.i2 = n3;
      this.i3 = n4;
   }

   @Override
   public void set(float f) {
      this.f0 = f;
   }

   @Override
   public void set(float f, float f2) {
      this.f0 = f;
      this.f1 = f2;
   }

   @Override
   public void set(Vector2f vector2f) {
      this.set(vector2f.x, vector2f.y);
   }

   @Override
   public void set(float f, float f2, float f3) {
      this.f0 = f;
      this.f1 = f2;
      this.f2 = f3;
   }

   @Override
   public void set(Vector3f vector3f) {
      this.set(vector3f.x, vector3f.y, vector3f.z);
   }

   @Override
   public void set(float f, float f2, float f3, float f4) {
      this.f0 = f;
      this.f1 = f2;
      this.f2 = f3;
      this.f3 = f4;
   }

   @Override
   public void set(Vector4f vector4f) {
      this.set(vector4f.x, vector4f.y, vector4f.z, vector4f.w);
   }

   @Override
   public void set(Matrix4f matrix4f) {
   }

   @Override
   public void setFromArray(float[] fs) {
   }
}

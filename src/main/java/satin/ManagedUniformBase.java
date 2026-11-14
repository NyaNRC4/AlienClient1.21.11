package satin;

import net.minecraft.client.gl.ShaderProgram;

import java.util.List;

public abstract class ManagedUniformBase {
   protected final String name;

   public ManagedUniformBase(String str) {
      this.name = str;
   }

   public abstract boolean findUniformTargets(List var1);

   public abstract boolean findUniformTarget(ShaderProgram var1);

   public String getName() {
      return this.name;
   }
}

package satin;

import net.minecraft.client.gl.ShaderProgram;
import satin.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
   ShaderProgram getProgram();

   void release();
}

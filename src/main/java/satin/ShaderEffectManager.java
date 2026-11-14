package satin;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public interface ShaderEffectManager {
   static ShaderEffectManager getInstance() {
      return ReloadableShaderEffectManager.INSTANCE;
   }

   ManagedShaderEffect manage(Identifier var1);

   ManagedShaderEffect manage(Identifier var1, Consumer var2);

   ManagedCoreShader manageCoreShader(Identifier var1);

   ManagedCoreShader manageCoreShader(Identifier var1, VertexFormat var2);

   ManagedCoreShader manageCoreShader(Identifier var1, VertexFormat var2, Consumer var3);
}

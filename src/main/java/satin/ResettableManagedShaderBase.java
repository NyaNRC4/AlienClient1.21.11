package satin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import satin.uniform.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public abstract class ResettableManagedShaderBase implements UniformFinder {
   private final Identifier location;
   private final Map managedUniforms = new HashMap();
   private final List allUniforms = new ArrayList();
   protected AutoCloseable shader;
   private boolean errored;

   public ResettableManagedShaderBase(Identifier id) {
      this.location = id;
   }

   public void initializeOrLog(ResourceFactory resourceFactory) {
      try {
         this.initialize(resourceFactory);
      } catch (IOException ioException) {
         this.errored = true;
         this.logInitError(ioException);
      }
   }

   protected abstract void logInitError(IOException var1);

   protected void initialize(ResourceFactory resourceFactory) throws IOException {
      this.release();
      MinecraftClient client2 = MinecraftClient.getInstance();
      this.shader = this.parseShader(resourceFactory, client2, this.location);
      this.setup(client2.getWindow().getFramebufferWidth(), client2.getWindow().getFramebufferHeight());
   }

   protected abstract AutoCloseable parseShader(ResourceFactory var1, MinecraftClient var2, Identifier var3) throws IOException;

   public void release() {
      if (this.isInitialized()) {
         try {
            assert this.shader != null;
            this.shader.close();
            this.shader = null;
         } catch (Exception exception) {
            throw new RuntimeException("Failed to release shader " + this.location, exception);
         }
      }

      this.errored = false;
   }

   protected Collection getManagedUniforms() {
      return this.allUniforms;
   }

   protected abstract boolean setupUniform(ManagedUniformBase var1, AutoCloseable var2);

   public boolean isInitialized() {
      return this.shader != null;
   }

   public boolean isErrored() {
      return this.errored;
   }

   public Identifier getLocation() {
      return this.location;
   }

   protected ManagedUniformBase manageUniform(Map map, Function function2, String str, String str2) {
      ManagedUniformBase managedUniformBase = (ManagedUniformBase)map.get(str);
      if (managedUniformBase != null) {
         return managedUniformBase;
      }

      ManagedUniformBase managedUniformBase2 = (ManagedUniformBase)function2.apply(str);
      if (this.shader != null && !this.setupUniform(managedUniformBase2, this.shader)) {
         LogUtils.getLogger().warn("No {} found with name {} in shader {}", new Object[]{str2, str, this.location});
      }

      map.put(str, managedUniformBase2);
      this.allUniforms.add(managedUniformBase2);
      return managedUniformBase2;
   }

   @Override
   public Uniform1i findUniform1i(String str) {
      return (Uniform1i)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform1i$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform2i findUniform2i(String str) {
      return (Uniform2i)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform2i$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform3i findUniform3i(String str) {
      return (Uniform3i)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform3i$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform4i findUniform4i(String str) {
      return (Uniform4i)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform4i$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform1f findUniform1f(String str) {
      return (Uniform1f)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform1f$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform2f findUniform2f(String str) {
      return (Uniform2f)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform2f$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform3f findUniform3f(String str) {
      return (Uniform3f)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform3f$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public Uniform4f findUniform4f(String str) {
      return (Uniform4f)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniform4f$0((String)str);
         }
      }, str, "uniform");
   }

   @Override
   public UniformMat4 findUniformMat4(String str) {
      return (UniformMat4)this.manageUniform(this.managedUniforms, new Function() {
         @Override
         public Object apply(Object str) {
            return ResettableManagedShaderBase.lambda$findUniformMat4$0((String)str);
         }
      }, str, "uniform");
   }

   public abstract void setup(int var1, int var2);

   @Override
   public String toString() {
      return "%s[%s]".formatted(this.getClass().getSimpleName(), this.location);
   }

   static ManagedUniform lambda$findUniformMat4$0(String str) {
      return new ManagedUniform(str, 16);
   }

   static ManagedUniform lambda$findUniform4f$0(String str) {
      return new ManagedUniform(str, 4);
   }

   static ManagedUniform lambda$findUniform3f$0(String str) {
      return new ManagedUniform(str, 3);
   }

   static ManagedUniform lambda$findUniform2f$0(String str) {
      return new ManagedUniform(str, 2);
   }

   static ManagedUniform lambda$findUniform1f$0(String str) {
      return new ManagedUniform(str, 1);
   }

   static ManagedUniform lambda$findUniform4i$0(String str) {
      return new ManagedUniform(str, 4);
   }

   static ManagedUniform lambda$findUniform3i$0(String str) {
      return new ManagedUniform(str, 3);
   }

   static ManagedUniform lambda$findUniform2i$0(String str) {
      return new ManagedUniform(str, 2);
   }

   static ManagedUniform lambda$findUniform1i$0(String str) {
      return new ManagedUniform(str, 1);
   }
}

package satin;

import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 1.21.11 内置版 Satin 的效果注册表。
 *
 * <p>hachimi 原版通过它自己的事件总线（{@code EventBusImpl} + {@code OnResolutionChangedEvent}）
 * 在窗口尺寸变化时重建 target；本工程把这里改成被动式：尺寸变化由
 * {@link PostEffect#render(float)} 内部按主 framebuffer 尺寸自动检测并重建，
 * 需要显式重建时外部调用 {@link #onResolutionChanged(int, int)} 即可。
 */
public final class ReloadableShaderEffectManager implements ShaderEffectManager {
   public static final ReloadableShaderEffectManager INSTANCE = new ReloadableShaderEffectManager();
   private final Set managedShaders = new ReferenceOpenHashSet();

   public ReloadableShaderEffectManager() {
   }

   @Override
   public ManagedShaderEffect manage(Identifier id) {
      return this.manage(id, new Consumer() {
         @Override
         public void accept(Object id) {
            ReloadableShaderEffectManager.lambda$manage$0((ManagedShaderEffect)id);
         }
      });
   }

   @Override
   public ManagedShaderEffect manage(Identifier id, Consumer consumer) {
      ResettableManagedShaderEffect resettableManagedShaderEffect = new ResettableManagedShaderEffect(id, consumer);
      this.managedShaders.add(resettableManagedShaderEffect);
      return resettableManagedShaderEffect;
   }

   @Override
   public ManagedCoreShader manageCoreShader(Identifier id) {
      return this.manageCoreShader(id, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
   }

   @Override
   public ManagedCoreShader manageCoreShader(Identifier id, VertexFormat vertexFormat) {
      return this.manageCoreShader(id, vertexFormat, new Consumer() {
         @Override
         public void accept(Object id) {
            ReloadableShaderEffectManager.lambda$manageCoreShader$0((ManagedCoreShader)id);
         }
      });
   }

   @Override
   public ManagedCoreShader manageCoreShader(Identifier id, VertexFormat vertexFormat, Consumer consumer) {
      ResettableManagedCoreShader resettableManagedCoreShader = new ResettableManagedCoreShader(id, vertexFormat, consumer);
      this.managedShaders.add(resettableManagedCoreShader);
      return resettableManagedCoreShader;
   }

   /** 资源重载后重新解析所有效果（对应 1.21.1 里 Satin 注册的 reload listener）。 */
   public void reload(ResourceFactory resourceFactory) {
      Iterator iterator2 = this.managedShaders.iterator();

      while (iterator2.hasNext()) {
         ((ResettableManagedShaderBase)iterator2.next()).initializeOrLog(resourceFactory);
      }
   }

   public void onResolutionChanged(int n, int n2) {
      this.runShaderSetup(n, n2);
   }

   private void runShaderSetup(int n, int n2) {
      if (!this.managedShaders.isEmpty()) {
         for (ResettableManagedShaderBase resettableManagedShaderBase : (Iterable<ResettableManagedShaderBase>)this.managedShaders) {
            if (resettableManagedShaderBase.isInitialized()) {
               resettableManagedShaderBase.setup(n, n2);
            }
         }
      }
   }

   static void lambda$manageCoreShader$0(ManagedCoreShader managedCoreShader) {
   }

   static void lambda$manage$0(ManagedShaderEffect managedShaderEffect) {
   }
}

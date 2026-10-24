package dev.luminous.asm.mixins;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
    @Shadow
    public int width;
    @Shadow
    public int height;
    @Shadow
    protected MinecraftClient client;

    //Fuck Context queue draw
    //My util is immediate draw
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderInGameBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        Matrix3x2fStack stack = context.getMatrices();
        if (this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }

        if (ClientSetting.INSTANCE.darkening.getValue()) {
            Render2DUtil.rectFilled(stack, 0, 0, this.width, this.height, 0x50000000);
        }

        if (this.client.world != null) {
            if (ClickGui.getInstance().tint.booleanValue) {
                Render2DUtil.verticalGradient(
                        stack,
                        0, 0,
                        this.width, this.height,
                        ClickGui.getInstance().tint.getValue().getRGB(),
                        ClickGui.getInstance().endColor.getValue().getRGB()
                );
            }
        }
    }


/*    @Inject(method = "init()V", at = @At("TAIL"))
    public void onInit(CallbackInfo ci) {
        if ((Screen) (Object) this instanceof DownloadingTerrainScreen || (Screen) (Object) this instanceof ProgressScreen) {
            DirectionalLayoutWidget grid = DirectionalLayoutWidget.vertical();
            grid.getMainPositioner().alignHorizontalCenter().margin(10);
            grid.add(new ButtonWidget.Builder(Text.literal("Close This Screen"), button -> close()).build());

            grid.refreshPositions();
            grid.forEachChild(this::addDrawableChild);
            SimplePositioningWidget.setPos(grid, this.getNavigationFocus());
        }
    }*/

    @Shadow
    protected void renderPanoramaBackground(DrawContext context, float delta) {
    }

    @Shadow
    protected void renderDarkening(DrawContext context) {
    }

    @Shadow
    public void close() {
    }

    @Shadow
    public ScreenRect getNavigationFocus() {
        return null;
    }

    @Shadow
    protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
        return null;
    }
}

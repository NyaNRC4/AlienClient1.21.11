package dev.luminous.core.impl;

import dev.luminous.api.utils.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Identifier;
import satin.ManagedShaderEffect;
import satin.ShaderEffectManager;

public class BlurManager
        implements Wrapper {
    private ManagedShaderEffect blur;

    private ManagedShaderEffect blurEffect() {
        if (this.blur == null) {
            this.blur = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/blurarea.json"));
        }
        return this.blur;
    }

    public void applyBlur(float radius, float startX, float startY, float width, float height) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main == null || width <= 0.0f || height <= 0.0f) {
            return;
        }
        float scale = (float) client.getWindow().getScaleFactor();
        float framebufferWidth = (float) main.textureWidth;
        float framebufferHeight = (float) main.textureHeight;
        float x = startX * scale;
        float y = framebufferHeight - (startY + height) * scale;
        float w = width * scale;
        float h = height * scale;

        ManagedShaderEffect effect = this.blurEffect();
        if (effect == null) {
            return;
        }
        effect.setUniformValue("BlurXY", x, y, 0.0f, 0.0f);
        effect.setUniformValue("BlurCoord", w, h, 0.0f, 0.0f);
        effect.setUniformValue("InSize", framebufferWidth, framebufferHeight, 0.0f, 0.0f);
        effect.setUniformValue("Radius", Math.max(1.0f, radius));
        effect.render(client.getRenderTickCounter().getTickProgress(true));
    }
}

package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

public class Crosshair extends Module {
    public static Crosshair INSTANCE;
    public final SliderSetting length =
            add(new SliderSetting("Length", 5, 0.0, 20.0, 0.1));
    public final SliderSetting thickness =
            add(new SliderSetting("Thickness", 2, 0.0, 20.0, 0.1));
    public final SliderSetting interval =
            add(new SliderSetting("Interval", 2, 0.0, 20.0, 0.1));
    private final ColorSetting color = add(new ColorSetting("Color"));

    public Crosshair() {
        super("Crosshair", Category.Render);
        setChinese("准星");
        INSTANCE = this;
    }

    public void draw(DrawContext context) {
        Matrix3x2fStack matrixStack = context.getMatrices();
        float centerX = (float)mc.getWindow().getScaledWidth() / 2.0f;
        float centerY = (float)mc.getWindow().getScaledHeight() / 2.0f;
        Render2DUtil.drawRect(matrixStack, centerX - this.thickness.getValueFloat() / 2.0f, centerY - this.length.getValueFloat() - this.interval.getValueFloat(), this.thickness.getValueFloat(), this.length.getValueFloat(), this.color.getValue());
        Render2DUtil.drawRect(matrixStack, centerX - this.thickness.getValueFloat() / 2.0f, centerY + this.interval.getValueFloat(), this.thickness.getValueFloat(), this.length.getValueFloat(), this.color.getValue());
        Render2DUtil.drawRect(matrixStack, centerX + this.interval.getValueFloat(), centerY - this.thickness.getValueFloat() / 2.0f, this.length.getValueFloat(), this.thickness.getValueFloat(), this.color.getValue());
        Render2DUtil.drawRect(matrixStack, centerX - this.interval.getValueFloat() - this.length.getValueFloat(), centerY - this.thickness.getValueFloat() / 2.0f, this.length.getValueFloat(), this.thickness.getValueFloat(), this.color.getValue());
    }
}


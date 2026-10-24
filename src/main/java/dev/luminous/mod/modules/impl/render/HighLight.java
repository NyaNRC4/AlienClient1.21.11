package dev.luminous.mod.modules.impl.render;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;

public class HighLight extends Module {
    public static HighLight INSTANCE;
    private final BooleanSetting depth = this.add(new BooleanSetting("Depth", true));
    private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 0, 0, 50)).injectBoolean(true));
    private final ColorSetting boxColor = this.add(new ColorSetting("Box", new Color(255, 0, 0, 100)).injectBoolean(true));

    public HighLight() {
        super("HighLight", Module.Category.Render);
        INSTANCE = this;
        this.setChinese("方块高亮");
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
    }
}

package dev.luminous.mod.modules.impl.render;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Fov extends Module {
    public static Fov INSTANCE;

    public final SliderSetting fov = add(new SliderSetting("Fov", 90, 30, 170, 1));
    public final SliderSetting itemFov = add(new SliderSetting("ItemFov", 70, 30, 170, 1));

    public Fov() {
        super("Fov", Category.Render);
        setChinese("自定义视角");
        INSTANCE = this;
    }
}

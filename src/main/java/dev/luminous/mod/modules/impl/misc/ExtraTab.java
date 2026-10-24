package dev.luminous.mod.modules.impl.misc;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class ExtraTab extends Module {
    public static ExtraTab INSTANCE;
    public final SliderSetting size =
            add(new SliderSetting("Size", 200, 80, 1000, 1));
    public final SliderSetting columns =
            add(new SliderSetting("Columns", 20, 1, 100, 1));

    public ExtraTab() {
        super("ExtraTab", Category.Misc);
        setChinese("列表无上限");
        INSTANCE = this;
    }
}
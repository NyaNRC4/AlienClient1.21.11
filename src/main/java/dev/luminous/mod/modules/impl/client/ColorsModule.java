package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;

import java.awt.*;

public class ColorsModule extends Module {
    public static ColorsModule INSTANCE;
    public final ColorSetting clientColor = add(new ColorSetting("Color", new Color(255, 0, 0)).allowClientColor(false));

    public ColorsModule() {
        super("Colors", Category.Client);
        setChinese("颜色");
        INSTANCE = this;
    }

    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = true;
    }

    @Override
    public boolean isOn() {
        return true;
    }
}

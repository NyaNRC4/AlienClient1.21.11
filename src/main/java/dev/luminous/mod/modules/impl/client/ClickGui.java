package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.Button;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ClickGui
        extends Module {
    private static ClickGui INSTANCE;
    public final BooleanSetting autoSave = add(new BooleanSetting("AutoSave", true));
    public final BooleanSetting font = add(new BooleanSetting("Font", true));
    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    public final BooleanSetting sound = add(new BooleanSetting("Sound", true).setParent());
    public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 1, 0, 2, 0.1, sound::isOpen));
    public final SliderSetting height = this.add(new SliderSetting("Height", 3, 0, 7));
    public final SliderSetting textOffset = add(new SliderSetting("TextOffset", 0, -5, 5, 1));
    public final SliderSetting titleOffset = add(new SliderSetting("TitleOffset", -1, -5, 5, 1));

    public final SliderSetting alpha = this.add(new SliderSetting("Alpha", 180, 0, 255));
    public final SliderSetting hoverAlpha = this.add(new SliderSetting("HoverAlpha", 240, 0, 255));
    public final SliderSetting topAlpha = this.add(new SliderSetting("TopAlpha", 240, 0, 255));

    public final BooleanSetting fade = add(new BooleanSetting("Fade", true).setParent());
    public final SliderSetting length = this.add(new SliderSetting("Length", 250, 0, 1000, fade::isOpen));
    public final EnumSetting<Easing> easing = add(new EnumSetting<>("Easing", Easing.Expo, fade::isOpen));

    public final BooleanSetting blur = add(new BooleanSetting("Blur", false).setParent());
    public final SliderSetting radius = this.add(new SliderSetting("Radius", 10f, 0f, 100f, blur::isOpen));

    public final BooleanSetting elements = add(new BooleanSetting("Elements", false).setParent().injectTask(this::a));
    public final BooleanSetting line = add(new BooleanSetting("Line", true, elements::isOpen));
    public final ColorSetting gear = add(new ColorSetting("Gear", -1, elements::isOpen).injectBoolean(false));

    public final BooleanSetting colors = add(new BooleanSetting("Colors", false).setParent().injectTask(this::b));
    public final ColorSetting color = add(new ColorSetting("Color", colors::isOpen));
    public final ColorSetting hoverColor = add(new ColorSetting("HoverColor", -2007673515, colors::isOpen));
    public final ColorSetting defaultColor = add(new ColorSetting("DefaultColor", new Color(255, 255, 255, 0), colors::isOpen));
    public final ColorSetting defaultTextColor = add(new ColorSetting("DefaultTextColor", -1, colors::isOpen));
    public final ColorSetting enableTextColor = add(new ColorSetting("EnableTextColor", -1, colors::isOpen));
    public final ColorSetting topColor = add(new ColorSetting("TopColor", new Color(255, 0, 0), colors::isOpen));
    public final ColorSetting backGround = add(new ColorSetting("BackGround", 0x77000000, colors::isOpen).injectBoolean(true));
    public final ColorSetting tint = add(new ColorSetting("Tint", new Color(0, 0, 0, 36)).injectBoolean(true));
    public final ColorSetting endColor = add(new ColorSetting("End", new Color(255, 0, 0, 80), () -> tint.booleanValue));

    public double alphaValue;
    private final Animation animation = new Animation();

    public ClickGui() {
        super("ClickGui", Category.Client);
        setChinese("点击界面");
        INSTANCE = this;
        Alien.EVENT_BUS.subscribe(new FadeOut());
    }

    public static ClickGui getInstance() {
        return INSTANCE;
    }

    public void a() {
        elements.setValueWithoutTask(false);
        elements.setOpen(!elements.isOpen());
    }

    public void b() {
        colors.setValueWithoutTask(false);
        colors.setOpen(!colors.isOpen());
    }
    public static String key = "";
    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        updateColor();
        for (Component component : ClickGuiScreen.getInstance().getComponents()) {
            component.setHeight(18);
            for (Item item : component.getItems()) {
                item.setHeight(10 + height.getValueInt());
            }
        }
        mc.setScreen(ClickGuiScreen.getInstance());
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGuiScreen) {
            mc.currentScreen.close();
        }
        if (autoSave.getValue()) Alien.save();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        updateColor();
        if (!(mc.currentScreen instanceof ClickGuiScreen)) {
            this.disable();
        }
    }

    public void updateColor() {
        Button.hoverColor = hoverColor.getValue().getRGB();
        Button.defaultTextColor = defaultTextColor.getValue().getRGB();
        Button.defaultColor = defaultColor.getValue().getRGB();
        Button.enableTextColor = enableTextColor.getValue().getRGB();
    }

    public class FadeOut {
        @EventListener(priority=-99999)
        public void onRender2D(Render2DEvent event) {
            if (ClickGui.this.fade.getValue()) {
                if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
                    ClickGui.this.alphaValue = ClickGui.this.animation.get(ClickGui.this.isOn() ? 1.0 : 0.0, ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
                }
                if (ClickGui.this.alphaValue > 0.0 && !(mc.currentScreen instanceof ClickGuiScreen)) {
                    event.drawContext.getMatrices().pushMatrix();
                    ClickGuiScreen.getInstance().render(event.drawContext, 0, 0, event.tickDelta);
                    event.drawContext.getMatrices().popMatrix();
                }
            } else {
                ClickGui.this.alphaValue = 1.0;
            }
        }
    }

    public enum SnowShape {
        Snowflake,
        Block,
        Circle
    }
}
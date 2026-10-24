package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.*;

import java.awt.*;

public class ClientSetting extends Module {
    public static final Animation animation = new Animation();
    public static ClientSetting INSTANCE;
    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.Game));
    public final BooleanSetting lowVersion = add(new BooleanSetting("1.12", false, () -> page.is(Page.Game)));
    public final BooleanSetting crawl = add(new BooleanSetting("Crawl", true, () -> page.is(Page.Game)));
    public final BooleanSetting rotations = add(new BooleanSetting("ShowRotations", true, () -> page.is(Page.Game)).setParent());
    public final BooleanSetting sync = add(new BooleanSetting("Sync", false, () -> page.is(Page.Game) && rotations.isOpen()));
    public final BooleanSetting titleFix = add(new BooleanSetting("TitleFix", true, () -> page.is(Page.Game)));
    public final BooleanSetting fuckFPSLimit = add(new BooleanSetting("FuckFPSLimit", true, () -> page.is(Page.Game)));
    private final BooleanSetting portalGui = add(new BooleanSetting("BlockTickNausea", true, () -> page.is(Page.Game)));
    public final BooleanSetting optimizedCalc = add(new BooleanSetting("OptimizedCalc", false, () -> page.is(Page.Game)));
    public final BooleanSetting mioCompatible = add(new BooleanSetting("MioCompatible", false, () -> page.is(Page.Game)));

    public final StringSetting prefix = add(new StringSetting("Prefix", ";", () -> page.is(Page.Misc)));
    public final BooleanSetting chinese = add(new BooleanSetting("Chinese", false, () -> page.is(Page.Misc)));
    public final BooleanSetting titleOverride = add(new BooleanSetting("TitleOverride", true, () -> page.is(Page.Misc)).setParent());
    public final StringSetting windowTitle = add(new StringSetting("WindowTitle", Alien.NAME, () -> page.is(Page.Misc) && titleOverride.isOpen()));
    public final BooleanSetting debug = add(new BooleanSetting("DebugException", true, () -> page.is(Page.Misc)));
    public final BooleanSetting caughtException = add(new BooleanSetting("CaughtException", false, () -> page.is(Page.Misc)).setParent());
    public final BooleanSetting log = add(new BooleanSetting("Log", true, () -> page.is(Page.Misc) && caughtException.isOpen()));

    private final BooleanSetting hotbar = add(new BooleanSetting("HotbarAnim", true, () -> page.is(Page.Gui)));
    public final SliderSetting hotbarTime = add(new SliderSetting("HotbarTime", 100, 0, 1000, () -> page.is(Page.Gui)));
    public final EnumSetting<Easing> animEase = add(new EnumSetting<>("AnimEase", Easing.CubicInOut, () -> page.is(Page.Gui)));
    public final BooleanSetting darkening = add(new BooleanSetting("Darkening", true, () -> page.is(Page.Gui)));

    public final StringSetting hackName = add(new StringSetting("Notification", "[Alien]", () -> page.getValue() == Page.Notification));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(255, 38, 38), () -> page.getValue() == Page.Notification));
    public final EnumSetting<Style> messageStyle = add(new EnumSetting<>("Style", Style.Mio, () -> page.getValue() == Page.Notification));
    public final BooleanSetting toggle = add(new BooleanSetting("ModuleToggle", true, () -> page.getValue() == Page.Notification).setParent());
    public final BooleanSetting onlyOne = add(new BooleanSetting("OnlyOne", false, () -> page.getValue() == Page.Notification && toggle.isOpen()));

    public final BooleanSetting keepHistory = add(new BooleanSetting("KeepHistory", true, () -> page.getValue() == Page.ChatHud));
    public final BooleanSetting infiniteChat = add(new BooleanSetting("InfiniteChat", true, () -> page.getValue() == Page.ChatHud));
    public final BooleanSetting hideIndicator = add(new BooleanSetting("HideIndicator", true, () -> page.getValue() == Page.ChatHud));
    public final SliderSetting animationTime = add(new SliderSetting("AnimationTime", 300, 0, 1000, () -> page.getValue() == Page.ChatHud));
    public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> page.getValue() == Page.ChatHud));
    public final SliderSetting animateOffset = add(new SliderSetting("Offset", -40, -200, 100, () -> page.getValue() == Page.ChatHud));
    public final BooleanSetting fade = add(new BooleanSetting("Fade", true, () -> page.getValue() == Page.ChatHud));

    public ClientSetting() {
        super("ClientSetting", Category.Client);
        setChinese("客户端设置");
        INSTANCE = this;
    }

    public boolean portalGui() {
        return isOn() && portalGui.getValue();
    }

    public boolean hotbar() {
        return isOn() && hotbar.getValue();
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

    public enum Page {
        Game,
        Gui,
        Misc,
        Notification,
        ChatHud
    }

    public enum Style {
        Mio,
        Debug,
        Lowercase,
        Normal,
        Future,
        Earth,
        Moon,
        Melon,
        Chinese,
        None
    }
}
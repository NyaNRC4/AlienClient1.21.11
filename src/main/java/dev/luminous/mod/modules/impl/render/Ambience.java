package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.LightmapGammaEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.awt.*;

public class Ambience extends Module {

    public static Ambience INSTANCE;
    public final ColorSetting filter = add(new ColorSetting("Filter", new Color(255, 255, 255, 20)).injectBoolean(false));
    public final ColorSetting worldColor = add(new ColorSetting("WorldColor", new Color(0xFFFFFFFF, true)).injectBoolean(true));
    public final BooleanSetting customTime =
            add(new BooleanSetting("CustomTime", false).setParent());
    public final SliderSetting time =
            add(new SliderSetting("Time", 0, 0, 24000, customTime::isOpen));
    public final ColorSetting fog =
            add(new ColorSetting("FogColor", new Color(0xCC7DD5)).injectBoolean(false));
    public final ColorSetting sky =
            add(new ColorSetting("SkyColor", new Color(0x000000)).injectBoolean(false));
    public final ColorSetting cloud =
            add(new ColorSetting("CloudColor", new Color(0x000000)).injectBoolean(false));
    public final ColorSetting dimensionColor =
            add(new ColorSetting("DimensionColor", new Color(0x000000)).injectBoolean(false));
    public final BooleanSetting fogDistance =
            add(new BooleanSetting("FogDistance", false).setParent());
    public final SliderSetting fogStart =
            add(new SliderSetting("FogStart", 50, 0, 1000, fogDistance::isOpen));
    public final SliderSetting fogEnd =
            add(new SliderSetting("FogEnd", 100, 0, 1000, fogDistance::isOpen));
    public final BooleanSetting fullBright =
            add(new BooleanSetting("FullBright", false));
    public final BooleanSetting forceOverworld =
            add(new BooleanSetting("ForceOverworld", false));
    public final BooleanSetting customLuminance =
            add(new BooleanSetting("CustomLuminance", false).setParent().injectTask(() -> {
                if (!nullCheck()) {
                    mc.worldRenderer.reload();
                }
            }));
    public final SliderSetting luminance =
            add(new SliderSetting("Luminance", 15, 0, 15, customLuminance::isOpen).injectTask(() -> {
                if (!nullCheck() && customLuminance.getValue()) {
                    mc.worldRenderer.reload();
                }
            }));
    long oldTime;

    public Ambience() {
        super("Ambience", "Custom ambience", Category.Render);
        setChinese("自定义环境");
        INSTANCE = this;
    }

    @EventListener
    public void onRender2D(PreRender2DEvent event) {
        if (filter.booleanValue) {
            event.drawContext.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), filter.getValue().getRGB());
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (customTime.getValue()) {
            mc.world.getLevelProperties().setTimeOfDay((long) this.time.getValue());
        }
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        oldTime = mc.world.getTimeOfDay();
        if (customTime.getValue()) {
            mc.world.getLevelProperties().setTimeOfDay((long) this.time.getValue());
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.world.getLevelProperties().setTimeOfDay(oldTime);
    }

    @EventListener
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.getValue()) {
            this.oldTime = ((WorldTimeUpdateS2CPacket)event.getPacket()).timeOfDay();
            event.cancel();
        }
    }

    @EventListener
    public void onLightmapGamma(LightmapGammaEvent event)
    {
        if (fullBright.getValue())
        {
            event.cancel();
            event.setGamma(25);
        }
    }
}
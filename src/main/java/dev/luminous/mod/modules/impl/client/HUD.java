package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.asm.accessors.ISimpleRegistry;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

public class HUD extends Module {
    public static HUD INSTANCE;
    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    public final BooleanSetting renderingUp = add(new BooleanSetting("RenderingUp", false, () -> page.is(Page.General)));
    public final BooleanSetting font = add(new BooleanSetting("Font", true, () -> page.is(Page.General)));
    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true, () -> page.is(Page.General)));
    public final BooleanSetting lowerCase = add(new BooleanSetting("LowerCase", false, () -> page.is(Page.General)));
    public final BooleanSetting sort = add(new BooleanSetting("Sort", false, () -> page.is(Page.General)));
    public final SliderSetting xOffset = add(new SliderSetting("XOffset", 0, 0, 50, 0.1, () -> page.is(Page.General)));
    public final SliderSetting yOffset = add(new SliderSetting("YOffset", 0, 0, 50, 0.1, () -> page.is(Page.General)));
    public final SliderSetting textOffset = add(new SliderSetting("TextOffset", 0, -10, 10, 0.1, () -> page.is(Page.General)));
    public final SliderSetting interval = add(new SliderSetting("Interval", 0, 0, 15, 0.1, () -> page.is(Page.General)));
    public final SliderSetting enableLength = add(new SliderSetting("EnableLength", 200, 0, 1000, () -> page.is(Page.General)));
    public final SliderSetting disableLength = add(new SliderSetting("DisableLength", 200, 0, 1000, () -> page.is(Page.General)));
    public final SliderSetting fadeLength = add(new SliderSetting("FadeLength", 200, 0, 1000, () -> page.is(Page.General)));
    public final EnumSetting<Easing> easing = add(new EnumSetting<>("Easing", Easing.CircInOut, () -> page.is(Page.General)));

    public final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", true, () -> page.is(Page.Element)).setParent());
    public final BooleanSetting listSort = add(new BooleanSetting("ListSort", true, () -> page.is(Page.Element) && arrayList.isOpen()));
    public final BooleanSetting armor = add(new BooleanSetting("Armor", true, () -> page.is(Page.Element)).setParent());
    public final SliderSetting armorOffset = add(new SliderSetting("ArmorOffset", 1, 0, 100, -1, () -> page.is(Page.Element) && armor.isOpen()));
    public final BooleanSetting durability = add(new BooleanSetting("Durability", true, () -> page.is(Page.Element) && armor.isOpen()));
    public final BooleanSetting waterMark = add(new BooleanSetting("WaterMark", true, () -> page.is(Page.Element)).setParent());
    public final ColorSetting pulse = add(new ColorSetting("Pulse", new Color(79, 0, 0), () -> page.is(Page.Element) && waterMark.isOpen()).injectBoolean(true));
    public final StringSetting waterMarkString = add(new StringSetting("Title", "%hackname% %version%", () -> page.is(Page.Element) && waterMark.isOpen()));
    public final SliderSetting waterMarkOffset = add(new SliderSetting("Offset", 1, 0, 100, -1, () -> page.is(Page.Element) && waterMark.isOpen()));
    public final BooleanSetting fps = add(new BooleanSetting("FPS", true, () -> page.is(Page.Element)));
    public final BooleanSetting ping = add(new BooleanSetting("Ping", true, () -> page.is(Page.Element)));
    public final BooleanSetting tps = add(new BooleanSetting("TPS", true, () -> page.is(Page.Element)));
    public final BooleanSetting ip = add(new BooleanSetting("IP", false, () -> page.is(Page.Element)));
    public final BooleanSetting time = add(new BooleanSetting("Time", false, () -> page.is(Page.Element)));
    public final BooleanSetting speed = add(new BooleanSetting("Speed", true, () -> page.is(Page.Element)));
    public final BooleanSetting brand = add(new BooleanSetting("Brand", false, () -> page.is(Page.Element)));
    public final BooleanSetting potions = add(new BooleanSetting("Potions", true, () -> page.is(Page.Element)));
    public final BooleanSetting coords = add(new BooleanSetting("Coords", true, () -> page.is(Page.Element)).setParent());
    public final BooleanSetting colorSync = add(new BooleanSetting("ColorSync", true, () -> page.is(Page.Element) && coords.isOpen()));

    public final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0), () -> page.is(Page.Color)));
    private final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("ColorMode", ColorMode.Pulse, () -> page.is(Page.Color)));
    private final SliderSetting rainbowSpeed = add(new SliderSetting("RainbowSpeed", 4, 1, 10, 0.1, () -> page.is(Page.Color) && colorMode.getValue() == ColorMode.Rainbow));
    private final SliderSetting saturation = add(new SliderSetting("Saturation", 130.0f, 1.0f, 255.0f, () -> page.is(Page.Color) && colorMode.getValue() == ColorMode.Rainbow));
    private final SliderSetting rainbowDelay = add(new SliderSetting("Delay", 350, 0, 1000, () -> page.is(Page.Color) && colorMode.getValue() == ColorMode.Rainbow));
    private final ColorSetting endColor = add(new ColorSetting("SecondColor", new Color(255, 0, 0, 255), () -> page.is(Page.Color) && colorMode.getValue() == ColorMode.Pulse).injectBoolean(true));
    private final SliderSetting pulseSpeed = add(new SliderSetting("PulseSpeed", 1, 0, 5, 0.1, () -> page.is(Page.Color)));
    private final SliderSetting pulseCounter = add(new SliderSetting("Counter", 10, 1, 50, () -> page.is(Page.Color)));

    public final BooleanSetting blur = add(new BooleanSetting("Blur", false, () -> page.is(Page.Color)).setParent());
    public final SliderSetting radius = add(new SliderSetting("Radius", 10f, 0f, 100f, () -> page.is(Page.Color) && blur.isOpen()));

    private final BooleanSetting backGround = add(new BooleanSetting("BackGround", false, () -> page.is(Page.Color)).setParent());
    public final SliderSetting width = add(new SliderSetting("Width", 0f, 0f, 15f, () -> page.is(Page.Color) && backGround.isOpen()));
    private final ColorSetting bgColor = add(new ColorSetting("BGColor", new Color(0, 0, 0, 100), () -> page.is(Page.Color) && backGround.isOpen()));

    private final ColorSetting rect = add(new ColorSetting("Rect", new Color(208, 0, 0), () -> page.is(Page.Color)).injectBoolean(false));
    private final ColorSetting glow = add(new ColorSetting("Glow", new Color(208, 0, 100), () -> page.is(Page.Color)).injectBoolean(false));

    private final DecimalFormat decimal = new DecimalFormat("0.0");
    private final ArrayList<Info> infoList = new ArrayList<>();
    private final ArrayList<Info> moduleList = new ArrayList<>();

    public HUD() {
        super("HUD", Category.Client);
        setChinese("界面");
        INSTANCE = this;
        Alien.EVENT_BUS.subscribe(new InitHandler());
        for (StatusEffect potionEffect : Registries.STATUS_EFFECT) {
            try {
                //noinspection SuspiciousMethodCalls,unchecked
                RegistryEntry<StatusEffect> effectRegistryEntry = (RegistryEntry<StatusEffect>) (Object) (((ISimpleRegistry<StatusEffects>) Registries.STATUS_EFFECT).getValueToEntry().get(potionEffect));

                infoList.add(new Info(() -> {
                    StatusEffectInstance effect = mc.player.getStatusEffect(effectRegistryEntry);
                    if (effect != null) {
                        String s = potionEffect.getName().getString() + " " + (effect.getAmplifier() + 1);
                        String s2 = getDuration(effect);
                        return s + " §f" + s2;
                    } else {
                        return "";
                    }
                }, () -> mc.player.hasStatusEffect(effectRegistryEntry) && potions.getValue()));
            } catch (Exception ignored) {

            }
        }
        infoList.add(new Info(() -> "ServerBrand §f" + (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "")), brand::getValue));
        infoList.add(new Info(() -> ("Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address)), ip::getValue));
        infoList.add(new Info(() -> ("TPS §f" + Alien.SERVER.getTPS() + " [" + Alien.SERVER.getCurrentTPS() + "]"), tps::getValue));
        infoList.add(new Info(() -> {
            double x = mc.player.getX() - mc.player.lastX;
            // double y = mc.player.getY() - mc.player.prevY;
            double z = mc.player.getZ() - mc.player.lastZ;
            double dist = Math.sqrt(x * x + z * z) / 1000.0;
            double div = 0.05 / 3600.0;
            float timer = Alien.TIMER.get();
            final double playerSpeed = dist / div * timer;
            return String.format("Speed §f%skm/h",
                    decimal.format(playerSpeed));
        }, speed::getValue));
        infoList.add(new Info(() -> ("Time §f" + (new SimpleDateFormat("h:mm a", Locale.ENGLISH)).format(new Date())), time::getValue));
        infoList.add(new Info(() -> {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            String playerPing;
            if (playerListEntry == null) {
                playerPing = "Unknown";
            } else {
                playerPing = playerListEntry.getLatency() + "ms";
            }
            return "Ping §f" + playerPing;
        }, ping::getValue));
        infoList.add(new Info(() -> "FPS §f" + Alien.FPS.getFps(), fps::getValue));
    }

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "∞";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            int sec = (var1 % 1200) / 20;

            return String.format("%d:%02d", mins, sec);
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (waterMark.getValue()) {
            if (pulse.booleanValue) {
                TextUtil.drawStringPulse(drawContext, waterMarkString.getValue().replaceAll("%version%", Alien.VERSION).replaceAll("%hackname%", Alien.NAME), waterMarkOffset.getValueInt(), waterMarkOffset.getValueInt(), color.getValue(), pulse.getValue(), pulseSpeed.getValue(), pulseCounter.getValueInt(), font.getValue(), shadow.getValue());
            } else {
                TextUtil.drawString(drawContext, waterMarkString.getValue().replaceAll("%version%", Alien.VERSION).replaceAll("%hackname%", Alien.NAME), waterMarkOffset.getValueInt(), waterMarkOffset.getValueInt(), color.getValue().getRGB(), font.getValue(), shadow.getValue());
            }
        }
        int fontHeight = getHeight();
        if (coords.getValue()) {
            String coordsString = getCoords();

            drawCoord(drawContext, coordsString, mc.getWindow().getScaledHeight() - fontHeight - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
        }

        Info.onRender(drawContext, infoList, renderingUp.getValue());
        if (arrayList.getValue()) Info.onRender(drawContext, moduleList, !renderingUp.getValue());
    }

    @EventListener(priority = -999)
    public void onUpdate(ClientTickEvent event) {
        if (nullCheck()) return;
        if (event.isPost()) {
            Info.onUpdate(infoList, sort.getValue());
            if (arrayList.getValue()) Info.onUpdate(moduleList, listSort.getValue());
        }
    }

    private static String getCoords() {
        boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);

        int posX = mc.player.getBlockX();
        int posY = mc.player.getBlockY();
        int posZ = mc.player.getBlockZ();

        float factor = !inNether ? 0.125F : 8.0F;

        int anotherWorldX = (int) (mc.player.getX() * factor);
        int anotherWorldZ = (int) (mc.player.getZ() * factor);

        return "XYZ §f" + (inNether ? (posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f") : (posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]"));
    }

    private int getWidth(String s) {
        if (lowerCase.getValue()) {
            s = s.toLowerCase();
        }
        if (font.getValue()) {
            return (int) FontManager.ui.getWidth(s);
        }
        return mc.textRenderer.getWidth(s);
    }

    private int getHeight() {
        if (font.getValue()) {
            return (int) FontManager.ui.getFontHeight();
        }
        return mc.textRenderer.fontHeight;
    }

    private void drawCoord(DrawContext drawContext, String s, int y) {
        if (colorSync.getValue()) {
            if (lowerCase.getValue()) {
                s = s.toLowerCase();
            }
            TextUtil.drawString(drawContext, s, 2, y, getColor(20), font.getValue(), shadow.getValue());
        } else if (pulse.booleanValue) {
            TextUtil.drawStringPulse(drawContext, s, 2, y, color.getValue(), pulse.getValue(), pulseSpeed.getValue(), pulseCounter.getValueInt(), font.getValue(), shadow.getValue());
        } else {
            TextUtil.drawString(drawContext, s, 2, y, color.getValue().getRGB(), font.getValue(), shadow.getValue());
        }
    }

    public int getColor(double counter) {
        if (colorMode.getValue() != ColorMode.Custom) {
            return rainbow(counter).getRGB();
        }
        return color.getValue().getRGB();
    }

    private Color rainbow(double delay) {
        if (colorMode.getValue() == ColorMode.Pulse) {
            if (endColor.booleanValue) {
                return ColorUtil.pulseColor(color.getValue(), endColor.getValue(), delay, pulseCounter.getValueInt(), pulseSpeed.getValue());
            } else {
                return ColorUtil.pulseColor(color.getValue(), delay, pulseCounter.getValueInt(), pulseSpeed.getValue());
            }
        } else if (colorMode.getValue() == ColorMode.Rainbow) {
            double rainbowState = Math.ceil((System.currentTimeMillis() * rainbowSpeed.getValue() + delay * rainbowDelay.getValue()) / 20.0);
            return Color.getHSBColor((float) (rainbowState % 360.0 / 360), saturation.getValueFloat() / 255.0f, 1.0f);
        }
        return color.getValue();
    }

    private int getFontHeight() {
        if (font.getValue()) {
            return (int) FontManager.ui.getFontHeight();
        }
        return mc.textRenderer.fontHeight;
    }

    public enum Page {
        General,
        Element,
        Color
    }

    private enum ColorMode {
        Custom,
        Pulse,
        Rainbow
    }

    public class Info {
        public final Callable<String> info;
        public String string;
        public final BooleanSupplier drawn;
        public double currentX = 0;
        public boolean isOn;
        public final Animation animation = new Animation();
        public final Animation fadeAnimation = new Animation();

        public Info(Callable<String> info, BooleanSupplier drawn) {
            this.info = info;
            this.drawn = drawn;
            try {
                string = this.info.call();
            } catch (Exception ignored) {
            }
        }

        static double fontHeight;
        static double currentY;
        static int windowWidth;
        static boolean fromUp;
        static double counter;

        public static void onRender(DrawContext context, List<Info> list, boolean fromUp) {
            counter = 20;
            Info.fromUp = fromUp;
            fontHeight = INSTANCE.getFontHeight();
            currentY = fromUp ? 1 + INSTANCE.yOffset.getValue() : mc.getWindow().getScaledHeight() - fontHeight - 1 - (mc.currentScreen instanceof ChatScreen && INSTANCE.yOffset.getValue() < 12 ? 12 - INSTANCE.yOffset.getValue() + INSTANCE.interval.getValue() / 2 : 0) - INSTANCE.yOffset.getValue();
            windowWidth = mc.getWindow().getScaledWidth();
            for (Info s : list) {
                s.draw(context);
            }
        }

        public static void onUpdate(List<Info> list, boolean sort) {
            for (Info s : list) {
                s.onUpdate();
            }
            if (sort) {
                list.sort(Comparator.comparingInt(info -> (info.string == null ? 0 : -INSTANCE.getWidth(info.string))));
            }
        }

        public void onUpdate() {
            isOn = drawn.getAsBoolean();
            if (isOn) {
                try {
                    string = lowerCase.getValue() ? this.info.call().toLowerCase() : this.info.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void draw(DrawContext context) {
            if (currentX > 0 || isOn) {
                currentX = animation.get(isOn ? getWidth(string) + 1 : 0, isOn ? enableLength.getValueInt() : disableLength.getValueInt(), easing.getValue());

                double width = currentX + xOffset.getValueFloat();
                double fade = fadeAnimation.get(isOn ? 1 : 0, fadeLength.getValueInt(), easing.getValue());
                if (fade > 0.04) {
                    counter += fromUp ? fade : -fade;
                    int c = ColorUtil.injectAlpha(getColor(counter), (int) (color.getValue().getAlpha() * fade));
                    if (blur.getValue()) {
                        Alien.BLUR.applyBlur((float) (radius.getValue() * fade),
                                (float) (windowWidth - width - HUD.this.width.getValueFloat() / 2),
                                (float) currentY - 1 - interval.getValueFloat() / 2,
                                (float) width + HUD.this.width.getValueFloat() - xOffset.getValueFloat(),
                                (float) fontHeight + interval.getValueFloat());
                    }
                    if (backGround.getValue()) {
                        Render2DUtil.drawRect(context.getMatrices(),
                                (float) (windowWidth - width - HUD.this.width.getValueFloat() / 2),
                                (float) currentY - 1 - interval.getValueFloat() / 2,
                                (float) width + HUD.this.width.getValueFloat() - xOffset.getValueFloat(),
                                (float) fontHeight + interval.getValueFloat(),
                                ColorUtil.injectAlpha(bgColor.sync ? c : bgColor.getValue().getRGB(), (int) (bgColor.getValue().getAlpha() * fade))
                        );
                    }
                    if (glow.booleanValue) {
                        Render2DUtil.drawGlow(context.getMatrices(),
                                (float) (windowWidth - width - HUD.this.width.getValueFloat() / 2),
                                (float) currentY - 1 - interval.getValueFloat() / 2,
                                (float) width + HUD.this.width.getValueFloat() - xOffset.getValueFloat(),
                                (float) fontHeight + interval.getValueFloat(),
                                ColorUtil.injectAlpha(glow.sync ? c : glow.getValue().getRGB(), (int) (glow.getValue().getAlpha() * fade))
                        );
                    }
                    TextUtil.drawString(context, string, windowWidth - width, currentY + textOffset.getValueFloat(), c, font.getValue(), shadow.getValue());
                    if (rect.booleanValue) {
                        Render2DUtil.drawRect(context.getMatrices(),
                                (windowWidth + HUD.this.width.getValueFloat() / 2 - xOffset.getValueFloat()),
                                (float) currentY - 1 - interval.getValueFloat() / 2,
                                1f,
                                (float) fontHeight + interval.getValueFloat(),
                                rect.sync ? c : ColorUtil.injectAlpha(rect.getValue(), (int) (rect.getValue().getAlpha() * fade)).getRGB()
                        );
                    }
                    currentY += fromUp ? (fontHeight + interval.getValue()) * fade : -(fontHeight + interval.getValue()) * fade;
                }
            }
        }
    }

    public class InitHandler {
        @EventListener
        public void onInit(InitEvent event) {
            for (Module module : Alien.MODULE.getModules()) {
                moduleList.add(new Info(module::getArrayName, () -> module.isOn() && module.drawn.getValue()));
            }
            Alien.EVENT_BUS.unsubscribe(this);
        }
    }
}

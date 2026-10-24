package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextRadar extends Module {
    public static TextRadar INSTANCE;

    public TextRadar() {
        super("TextRadar", Category.Client);
        setChinese("文字雷达");
        INSTANCE = this;
    }

    private final DecimalFormat df = new DecimalFormat("0.0");

    private final BooleanSetting font = add(new BooleanSetting("Font", true));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    private final SliderSetting x = add(new SliderSetting("X", 0, 0, 1500));
    private final SliderSetting y = add(new SliderSetting("Y", 100, 0, 1000));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(255, 255, 255)));
    private final ColorSetting friend = add(new ColorSetting("Friend").injectBoolean(true));
    private final BooleanSetting doubleBlank = add(new BooleanSetting("Double", false));
    private final BooleanSetting health = add(new BooleanSetting("Health", true));
    private final BooleanSetting pops = add(new BooleanSetting("Pops", true));
    public final BooleanSetting red = add(new BooleanSetting("Red", false));
    private final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    private final BooleanSetting effects = add(new BooleanSetting("Effects", true));

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        int currentY = y.getValueInt();
        List<AbstractClientPlayerEntity> players = new ArrayList<>(mc.world.getPlayers());

        players.sort(Comparator.comparingDouble((player) -> mc.player.distanceTo(player)));

        for (PlayerEntity player : players) {
            if (player == mc.player) continue;
            StringBuilder stringBuilder = new StringBuilder();
            String blank = doubleBlank.getValue() ? "  " : " ";
            if (health.getValue()) {
                stringBuilder.append(getHealthColor(player));
                stringBuilder.append(df.format(player.getHealth() + player.getAbsorptionAmount()));
                stringBuilder.append(blank);
            }
            stringBuilder.append(Formatting.RESET);
            stringBuilder.append(player.getName().getString());
            if (distance.getValue()) {
                stringBuilder.append(blank);
                stringBuilder.append(Formatting.WHITE);
                stringBuilder.append(df.format(mc.player.distanceTo(player)));
                stringBuilder.append("m");
            }
            if (effects.getValue()) {
                if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    stringBuilder.append(blank);
                    stringBuilder.append(Formatting.GRAY);
                    stringBuilder.append("Lv.");
                    stringBuilder.append(player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
                    stringBuilder.append(blank);
                    stringBuilder.append(player.getStatusEffect(StatusEffects.SLOWNESS).getDuration() / 20 + 1);
                    stringBuilder.append("s");
                }
                if (player.hasStatusEffect(StatusEffects.SPEED)) {
                    stringBuilder.append(blank);
                    stringBuilder.append(Formatting.AQUA);
                    stringBuilder.append("Lv.");
                    stringBuilder.append(player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
                    stringBuilder.append(blank);
                    stringBuilder.append(player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
                    stringBuilder.append("s");
                }
                if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                    stringBuilder.append(blank);
                    stringBuilder.append(Formatting.DARK_RED);
                    stringBuilder.append("Lv.");
                    stringBuilder.append(player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1);
                    stringBuilder.append(blank);
                    stringBuilder.append(player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
                    stringBuilder.append("s");
                }
                if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                    stringBuilder.append(blank);
                    stringBuilder.append(Formatting.BLUE);
                    stringBuilder.append("Lv.");
                    stringBuilder.append(player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
                    stringBuilder.append(blank);
                    stringBuilder.append(player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                    stringBuilder.append("s");
                }
            }
            if (pops.getValue()) {
                int totemPopped = Alien.POP.getPop(player);
                if (totemPopped > 0) {
                    stringBuilder.append(blank);
                    stringBuilder.append(getPopColor(totemPopped));
                    stringBuilder.append("-");
                    stringBuilder.append(totemPopped);
                }
            }

            boolean isFriend = Alien.FRIEND.isFriend(player);
            if (isFriend && !friend.booleanValue) continue;
            int color = isFriend ? friend.getValue().getRGB() : this.color.getValue().getRGB();
            if (font.getValue()) {
                FontManager.ui.drawString(drawContext.getMatrices(), stringBuilder.toString(), x.getValueInt(), currentY, color, shadow.getValue());
            } else {
                drawContext.drawText(mc.textRenderer, stringBuilder.toString(), x.getValueInt(), currentY, color, shadow.getValue());
            }
            currentY += font.getValue() ? (int) FontManager.ui.getFontHeight() : mc.textRenderer.fontHeight;
        }
    }

    public static Formatting getHealthColor(PlayerEntity player) {
        double health = player.getHealth() + player.getAbsorptionAmount();
        if (health > 18) {
            return Formatting.GREEN;
        } else if (health > 16) {
            return Formatting.DARK_GREEN;
        } else if (health > 12) {
            return Formatting.YELLOW;
        } else if (health > 8) {
            return Formatting.GOLD;
        } else if (health > 4) {
            return Formatting.RED;
        } else {
            return Formatting.DARK_RED;
        }
    }

    public static Formatting getPopColor(int totems) {
        if (INSTANCE.red.getValue()) {
            return Formatting.RED;
        }

        if (totems > 10) {
            return Formatting.DARK_RED;
        } else if (totems > 8) {
            return Formatting.RED;
        } else if (totems > 6) {
            return Formatting.GOLD;
        } else if (totems > 4) {
            return Formatting.YELLOW;
        } else if (totems > 2) {
            return Formatting.DARK_GREEN;
        } else {
            return Formatting.GREEN;
        }
    }
}
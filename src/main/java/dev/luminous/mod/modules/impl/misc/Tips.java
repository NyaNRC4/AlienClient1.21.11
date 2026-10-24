package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.*;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.text.DecimalFormat;

public class Tips extends Module {
    public static Tips INSTANCE;
    public final BooleanSetting visualRange =
            add(new BooleanSetting("VisualRange", false).setParent());
    public final BooleanSetting friends =
            add(new BooleanSetting("Friends", false, visualRange::isOpen));
    public final BooleanSetting popCounter =
            add(new BooleanSetting("PopCounter", true));
    public final BooleanSetting deathCoords =
            add(new BooleanSetting("DeathCoords", true));
    public final BooleanSetting serverLag =
            add(new BooleanSetting("ServerLag", true));
    public final BooleanSetting lagBack =
            add(new BooleanSetting("LagBack", true));
    public final BooleanSetting potion =
            add(new BooleanSetting("Potion", true).setParent());
    public final BooleanSetting resistanceLevelCheck =
            add(new BooleanSetting("ResistanceLevelCheck", true, potion::isOpen));
    private final SliderSetting yOffset = add(new SliderSetting("YOffset", 0, -200, 200, potion::isOpen));
    final DecimalFormat df = new DecimalFormat("0.0");
    final int color = new Color(190, 0, 0).getRGB();
    private final Timer lagTimer = new Timer();
    private final Timer lagBackTimer = new Timer();
    int turtles = 0;

    public Tips() {
        super("Tips", Category.Misc);
        setChinese("提示");
        INSTANCE = this;
    }

    @EventListener
    public void onAddEntity(EntitySpawnEvent event) {
        if (!visualRange.getValue() || !(event.getEntity() instanceof PlayerEntity) || event.getEntity().getDisplayName() == null) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Alien.FRIEND.isFriend(playerName);
        if (isFriend && !friends.getValue() || event.getEntity() == mc.player) {
            return;
        }
        CommandManager.sendMessageId((isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f entered your visual range.", event.getEntity().getId() + 777);
        mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100f, 1.9f);
    }

    @EventListener
    public void onRemoveEntity(RemoveEntityEvent event) {
        if (!visualRange.getValue() || !(event.getEntity() instanceof PlayerEntity) || event.getEntity().getDisplayName() == null) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Alien.FRIEND.isFriend(playerName);
        if (isFriend && !friends.getValue() || event.getEntity() == mc.player) {
            return;
        }
        CommandManager.sendMessageId((isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f left your visual range.", event.getEntity().getId() + 777);
        mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100f, 1.9f);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (potion.getValue()) {
            turtles = InventoryUtil.getPotionCount(StatusEffects.RESISTANCE.value());
        }
    }

    @EventListener
    public void onPacketEvent(PacketEvent.Receive event) {
        lagTimer.reset();
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            lagBackTimer.reset();
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (serverLag.getValue() && lagTimer.passedS(1.4)) {
            String text = "Server not responding (" + df.format(lagTimer.getMs() / 1000d) + "s)";
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 10 + mc.textRenderer.fontHeight, color, true);
        }
        if (lagBack.getValue() && !lagBackTimer.passedS(1.5)) {
            String text = "Lagback (" + df.format((1500 - lagBackTimer.getMs()) / 1000d) + "s)";
            drawContext.drawText(mc.textRenderer, text, mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2, 10 + mc.textRenderer.fontHeight * 2, color, true);
        }
        if (potion.getValue()) {
            StringBuilder stringBuilder = new StringBuilder();
            if (turtles > 0) {
                stringBuilder.append("§e").append(turtles);
            }
            if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                if (!resistanceLevelCheck.getValue() || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0) {
                    if (!stringBuilder.isEmpty()) {
                        stringBuilder.append(" ");
                    }
                    stringBuilder.append("§9").append(mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                }
            }
            if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append("§4").append(mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
            }
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append("§b").append(mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
            }
            drawContext.drawText(mc.textRenderer, stringBuilder.toString(), mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(stringBuilder.toString()) / 2, mc.getWindow().getScaledHeight() / 2 + mc.textRenderer.fontHeight - yOffset.getValueInt(), -1, true);
        }
    }

    @EventListener
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player = event.getPlayer();
        if (popCounter.getValue()) {
            if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
                int l_Count = Alien.POP.popContainer.get(player.getName().getString());
                if (l_Count == 1) {
                    if (player.equals(mc.player)) {
                        sendMessage("§fYou§r died after popping " + "§f" + l_Count + "§r totem.", player.getId());
                    } else {
                        sendMessage("§f" + player.getName().getString() + "§r died after popping " + "§f" + l_Count + "§r totem.", player.getId());
                    }
                } else {
                    if (player.equals(mc.player)) {
                        sendMessage("§fYou§r died after popping " + "§f" + l_Count + "§r totems.", player.getId());
                    } else {
                        sendMessage("§f" + player.getName().getString() + "§r died after popping " + "§f" + l_Count + "§r totems.", player.getId());
                    }
                }
            } else {
                if (player.equals(mc.player)) {
                    sendMessage("§fYou§r died.", player.getId());
                } else {
                    sendMessage("§f" + player.getName().getString() + "§r died.", player.getId());
                }
            }
        }
        if (deathCoords.getValue() && player == mc.player) {
            sendMessage("§4You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
        }
    }

    @EventListener
    public void onTotem(TotemEvent event) {
        if (popCounter.getValue()) {
            PlayerEntity player = event.getPlayer();
            int l_Count = 1;
            if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
                l_Count = Alien.POP.popContainer.get(player.getName().getString());
            }
            if (l_Count == 1) {
                if (player.equals(mc.player)) {
                    sendMessage("§fYou§r popped " + "§f" + l_Count + "§r totem.", player.getId());
                } else {
                    sendMessage("§f" + player.getName().getString() + " §rpopped " + "§f" + l_Count + "§r totems.", player.getId());
                }
            } else {
                if (player.equals(mc.player)) {
                    sendMessage("§fYou§r popped " + "§f" + l_Count + "§r totem.", player.getId());
                } else {
                    sendMessage("§f" + player.getName().getString() + " §rhas popped " + "§f" + l_Count + "§r totems.", player.getId());
                }
            }
        }
    }

    public void sendMessage(String message, int id) {
        if (!nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                CommandManager.sendMessageId("§f[" + "§3" + getName() + "§f] " + message, id);
                return;
            }
            CommandManager.sendMessageId(message, id);
        }
    }
}

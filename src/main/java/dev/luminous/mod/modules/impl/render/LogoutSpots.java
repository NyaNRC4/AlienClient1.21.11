package dev.luminous.mod.modules.impl.render;

import com.google.common.collect.Maps;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogoutSpots extends Module {
    private final ColorSetting box = add(new ColorSetting("Box", new Color(255, 255, 255, 100)).injectBoolean(true));
    private final ColorSetting fill = add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
    private final ColorSetting text = add(new ColorSetting("Text", new Color(255, 255, 255, 255)).injectBoolean(true));
    private final ColorSetting chamsFill = add(new ColorSetting("ChamsFill", new Color(255, 255, 255, 100)).injectBoolean(true));
    private final ColorSetting chamsLine = add(new ColorSetting("ChamsLine", new Color(255, 255, 255, 100)).injectBoolean(true));
    private final BooleanSetting renderSkin = add(new BooleanSetting("Texture", true));
    final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
    final Map<UUID, ModelPlayer> logoutCache = Maps.newConcurrentMap();
    private final BooleanSetting health = add(new BooleanSetting("Health", true));
    private final BooleanSetting totem = add(new BooleanSetting("Totem", true));
    private final BooleanSetting message = add(new BooleanSetting("Message", true));

    public LogoutSpots() {
        super("LogoutSpots", Category.Render);
        setChinese("退出记录");
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                    if (addedPlayer.gameMode() != GameMode.SPECTATOR) {
                        for (UUID uuid : logoutCache.keySet()) {
                            if (!uuid.equals(addedPlayer.profile().id())) continue;
                            PlayerEntity player = logoutCache.get(uuid).player;
                            if (message.getValue())
                                mc.execute(() ->
                                        sendMessage("§f" + player.getName().getString() + " §rLogged back at §f" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ())
                                );
                            logoutCache.remove(uuid);
                        }
                    }
                }
            }
        } else if (event.getPacket() instanceof PlayerRemoveS2CPacket(List<UUID> profileIds)) {
            for (UUID uuid2 : profileIds) {
                for (UUID uuid : playerCache.keySet()) {
                    if (!uuid.equals(uuid2)) continue;
                    final PlayerEntity player = playerCache.get(uuid);
                    if (!logoutCache.containsKey(uuid) && player != null) {
                        ModelPlayer modelPlayer = new ModelPlayer(player);
                        if (message.getValue())
                            mc.execute(() ->
                                    sendMessage("§f" + player.getName().getString() + " §rLogged out at §f" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ())
                            );
                        logoutCache.put(uuid, modelPlayer);
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        playerCache.clear();
        logoutCache.clear();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        playerCache.clear();
        for (AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
            if (player == null || player.equals(mc.player)) continue;
            playerCache.put(player.getGameProfile().id(), player);
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        for (ModelPlayer data : logoutCache.values()) {
            PlayerEntity player = data.player;
            Box box = ((IEntity) player).getDimensions().getBoxAt(player.getEntityPos());
            if (this.box.booleanValue) {
                Render3DUtil.drawBox(matrixStack, box, this.box.getValue());
            }
            if (this.fill.booleanValue) {
                Render3DUtil.drawFill(matrixStack, box, this.fill.getValue());
            }
            if (chamsFill.booleanValue || chamsLine.booleanValue) {
                data.render(matrixStack, chamsFill, chamsLine);
            }
            if (text.booleanValue) {
                Render3DUtil.drawText3D(player.getName().getString() + (health.getValue() ? TextRadar.getHealthColor(player) + " " + round2(player.getHealth() + player.getAbsorptionAmount()) : "") + (totem.getValue() && Alien.POP.getPop(player) > 0 ? TextRadar.getPopColor(Alien.POP.getPop(player)) + " -" + (Alien.POP.getPop(player)) : ""), new Vec3d(player.getX(), ((IEntity) player).getDimensions().getBoxAt(player.getEntityPos()).maxY + 0.5, player.getZ()), text.getValue());
            }
        }
    }

    @Override
    public boolean onRender3DQueue(MatrixStack matrixStack) {
        if (!renderSkin.getValue() || logoutCache.isEmpty()) {
            return false;
        }

        OrderedRenderCommandQueue queue = mc.gameRenderer.getEntityRenderCommandQueue();
        CameraRenderState cameraRenderState = mc.gameRenderer.getEntityRenderStates().cameraRenderState;
        float delta = mc.getRenderTickCounter().getTickProgress(true);
        for (ModelPlayer data : logoutCache.values()) {
            data.renderPlayerSkin(matrixStack, queue, cameraRenderState, delta);
        }
        return true;
    }

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}
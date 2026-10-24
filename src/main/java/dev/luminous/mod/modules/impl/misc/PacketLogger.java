package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;

public class PacketLogger extends Module {

    public static PacketLogger INSTANCE;
    private final BooleanSetting moveFull = add(new BooleanSetting("MoveFull", true));
    private final BooleanSetting movePos = add(new BooleanSetting("MovePosition", true));
    private final BooleanSetting moveLook = add(new BooleanSetting("MoveLook", true));
    private final BooleanSetting moveGround = add(new BooleanSetting("MoveGround", true));
    private final BooleanSetting vehicleMove = add(new BooleanSetting("VehicleMove", true));
    private final BooleanSetting playerAction = add(new BooleanSetting("PlayerAction", true));
    private final BooleanSetting updateSlot = add(new BooleanSetting("UpdateSlot", true));
    private final BooleanSetting handSwing = add(new BooleanSetting("HandSwing", true));
    private final BooleanSetting pong = add(new BooleanSetting("Pong", true));
    private final BooleanSetting interactEntity = add(new BooleanSetting("InteractEntity", true));
    private final BooleanSetting interactBlock = add(new BooleanSetting("InteractBlock", true));
    private final BooleanSetting interactItem = add(new BooleanSetting("InteractItem", true));
    private final BooleanSetting closeScreen = add(new BooleanSetting("CloseScreen", true));
    private final BooleanSetting command = add(new BooleanSetting("ClientCommand", true));
    private final BooleanSetting status = add(new BooleanSetting("ClientStatus", true));
    private final BooleanSetting clickSlot = add(new BooleanSetting("ClickSlot", true));
    private final BooleanSetting pickInventory = add(new BooleanSetting("PickInventory", true));
    private final BooleanSetting teleportConfirm = add(new BooleanSetting("TeleportConfirm", true));

    private final BooleanSetting s2cVelocity = add(new BooleanSetting("S2cVelocity", true));

    public PacketLogger() {
        super("PacketLogger", Category.Misc);
        setChinese("数据包记录");
        INSTANCE = this;
    }

    private void logPacket(String msg, Object... args) {
        String s = String.format(msg, args);
        CommandManager.sendMessage(s);
    }

    @EventListener(priority = 999999)
    public void velocity(EntityVelocityUpdateEvent event) {
        if (s2cVelocity.getValue() && event.getEntity() == mc.player) {
            logPacket("S2C Velocity, x: %s, y: %s, z: %s, isExplosion: %s", event.getX(), event.getY(), event.getZ(), event.isExplosion());
        }
    }

    @EventListener(priority = -999999)
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket.Full packet && moveFull.getValue()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove Full - ");
            if (packet.changesPosition()) {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            if (packet.changesLook()) {
                builder.append("yaw: ").append(packet.getYaw(0.0f)).append(", pitch: ").append(packet.getPitch(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(builder.toString());
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.PositionAndOnGround packet && movePos.getValue()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove PosGround - ");
            if (packet.changesPosition()) {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(builder.toString());
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround packet && moveLook.getValue()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove LookGround - ");
            if (packet.changesLook()) {
                builder.append("yaw: ").append(packet.getYaw(0.0f)).append(", pitch: ").append(packet.getPitch(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(builder.toString());
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.OnGroundOnly packet && moveGround.getValue()) {
            String s = "PlayerMove Ground - onground: " + packet.isOnGround();
            logPacket(s);
        }
        if (event.getPacket() instanceof VehicleMoveC2SPacket packet && vehicleMove.getValue()) {
            logPacket("VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s", packet.position().getX(), packet.position().getY(), packet.position().getZ(), packet.yaw(), packet.pitch());
        }
        if (event.getPacket() instanceof PlayerActionC2SPacket packet && playerAction.getValue()) {
            if (packet.getDirection() != null) {
                logPacket("PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString());
            }
        }
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet && updateSlot.getValue()) {
            logPacket("UpdateSlot - slot: %d", packet.getSelectedSlot());
        }
        if (event.getPacket() instanceof HandSwingC2SPacket packet && handSwing.getValue()) {
            logPacket("HandSwing - hand: %s", packet.getHand().name());
        }
        if (event.getPacket() instanceof CommonPongC2SPacket packet && pong.getValue()) {
            logPacket("Pong - %d", packet.getParameter());
        }
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && interactEntity.getValue()) {
            logPacket("InteractEntity - Entity: %s, id: %s", Criticals.getEntity(packet).getName().getString(), Criticals.getEntity(packet).getId());
        }
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet && interactBlock.getValue()) {
            BlockHitResult blockHitResult = packet.getBlockHitResult();
            logPacket("InteractBlock - pos: %s, dir: %s, hand: %s", blockHitResult.getBlockPos().toShortString(), blockHitResult.getSide().name(), packet.getHand().name());
        }
        if (event.getPacket() instanceof PlayerInteractItemC2SPacket packet && interactItem.getValue()) {
            logPacket("InteractItem - hand: %s", packet.getHand().name());
        }
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket packet && closeScreen.getValue()) {
            logPacket("CloseScreen - id: %s", packet.getSyncId());
        }
        if (event.getPacket() instanceof ClientCommandC2SPacket packet && command.getValue()) {
            logPacket("ClientCommand - mode: %s", packet.getMode().name());
        }
        if (event.getPacket() instanceof ClientStatusC2SPacket packet && status.getValue()) {
            logPacket("ClientStatus - mode: %s", packet.getMode().name());
        }
        if (event.getPacket() instanceof ClickSlotC2SPacket packet && clickSlot.getValue()) {
            logPacket("ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.actionType().name(), packet.slot(), packet.button(), packet.syncId());
        }
        if (event.getPacket() instanceof ClickSlotC2SPacket packet && packet.actionType() == SlotActionType.CLONE && packet.button() == 2 && pickInventory.getValue()) {
            logPacket("PickInventory - slot: %s", packet.slot());
        }

        if (event.getPacket() instanceof TeleportConfirmC2SPacket packet && teleportConfirm.getValue()) {
            logPacket("TeleportConfirm - id: %s", packet.getTeleportId());
        }
    }
}

package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.InteractItemEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import net.minecraft.util.PlayerInput;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class NoSlow
extends Module {
    public static NoSlow INSTANCE;
    final Queue<ClickSlotC2SPacket> storedClicks = new LinkedList<ClickSlotC2SPacket>();
    final AtomicBoolean pause = new AtomicBoolean();
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<Mode>("Mode", Mode.Vanilla));
    private final BooleanSetting soulSand = this.add(new BooleanSetting("SoulSand", true));
    private final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
    private final BooleanSetting climb = this.add(new BooleanSetting("Climb", false));
    private final BooleanSetting gui = this.add(new BooleanSetting("Gui", true));
    private final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", false, this.gui::getValue));
    private final EnumSetting<Bypass> clickBypass = this.add(new EnumSetting<Bypass>("GuiMoveBypass", Bypass.None));
    boolean using = false;
    int delay = 0;

    public NoSlow() {
        super("NoSlow", Category.Movement);
        setChinese("无减速");
        INSTANCE = this;
    }

    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.using = NoSlow.mc.player.isUsingItem();
        --this.delay;
        if (this.using) {
            this.delay = 2;
        }
        if (this.using && !NoSlow.mc.player.isRiding() && !NoSlow.mc.player.isGliding()) {
            switch (this.mode.getValue().ordinal()) {
                case 1: {
                    mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(NoSlow.mc.player.getInventory().selectedSlot));
                    break;
                }
                case 2: {
                    if (NoSlow.mc.player.getActiveHand() == Hand.OFF_HAND) {
                        NoSlow.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                        break;
                    }
                    NoSlow.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                    break;
                }
                case 3: {
                    NoSlow.mc.interactionManager.clickSlot(NoSlow.mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, (PlayerEntity)NoSlow.mc.player);
                    if (NoSlow.mc.player.getActiveHand() == Hand.OFF_HAND) {
                        NoSlow.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                        break;
                    }
                    NoSlow.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                }
            }
        }
        if (this.gui.getValue() && !(NoSlow.mc.currentScreen instanceof ChatScreen)) {
            for (KeyBinding k : new KeyBinding[]{NoSlow.mc.options.backKey, NoSlow.mc.options.leftKey, NoSlow.mc.options.rightKey}) {
                k.setPressed(InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
            }
            NoSlow.mc.options.jumpKey.setPressed(ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.jumpKey.getBoundKeyTranslationKey()).getCode()));
            NoSlow.mc.options.forwardKey.setPressed(AutoWalk.INSTANCE.forward() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()));
            NoSlow.mc.options.sprintKey.setPressed(Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.sprintKey.getBoundKeyTranslationKey()).getCode()));
            if (this.allowSneak.getValue()) {
                NoSlow.mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.sneakKey.getBoundKeyTranslationKey()).getCode()));
            }
        }
    }

    @EventListener(priority=100)
    public void keyboard(KeyboardInputEvent event) {
        if (this.sneak.getValue()) {
            event.cancel();
        }
        if (this.gui.getValue() && !(NoSlow.mc.currentScreen instanceof ChatScreen)) {
            for (KeyBinding k : new KeyBinding[]{NoSlow.mc.options.backKey, NoSlow.mc.options.leftKey, NoSlow.mc.options.rightKey}) {
                k.setPressed(InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
            }
            NoSlow.mc.options.jumpKey.setPressed(ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.jumpKey.getBoundKeyTranslationKey()).getCode()));
            NoSlow.mc.options.forwardKey.setPressed(AutoWalk.INSTANCE.forward() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()));
            NoSlow.mc.options.sprintKey.setPressed(Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater() || InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.sprintKey.getBoundKeyTranslationKey()).getCode()));
            if (this.allowSneak.getValue()) {
                NoSlow.mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(mc.getWindow(), InputUtil.fromTranslationKey(NoSlow.mc.options.sneakKey.getBoundKeyTranslationKey()).getCode()));
            }
            boolean pf = NoSlow.mc.options.forwardKey.isPressed();
            boolean pb = NoSlow.mc.options.backKey.isPressed();
            boolean pl = NoSlow.mc.options.leftKey.isPressed();
            boolean pr = NoSlow.mc.options.rightKey.isPressed();
            NoSlow.mc.player.input.playerInput = new PlayerInput(
                pf && !pb,
                pb && !pf,
                pl && !pr,
                pr && !pl,
                NoSlow.mc.options.jumpKey.isPressed(),
                NoSlow.mc.options.sneakKey.isPressed(),
                NoSlow.mc.options.sprintKey.isPressed()
            );
        }
    }

    @EventListener
    public void onUse(InteractItemEvent event) {
        if (event.isPre()) {
            if (this.delay > 0) {
                NoSlow.mc.itemUseCooldown = 0;
                event.cancel();
            } else if (this.mode.is(Mode.GrimPacket) && NoSlow.mc.player != null && NoSlow.mc.player.getStackInHand(event.hand).getItem().getComponents().contains(DataComponentTypes.FOOD)) {
                NoSlow.mc.interactionManager.clickSlot(NoSlow.mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, (PlayerEntity)NoSlow.mc.player);
            }
        }
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send e) {
        PlayerInteractItemC2SPacket packet;
        Packet<?> packet2;
        if (NoSlow.nullCheck()) {
            return;
        }
        if (this.mode.is(Mode.Drop) && (packet2 = e.getPacket()) instanceof PlayerInteractItemC2SPacket && (packet = (PlayerInteractItemC2SPacket)packet2).getHand() == Hand.MAIN_HAND && NoSlow.mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            mc.getNetworkHandler().sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            return;
        }
        if (!MovementUtil.isMoving() || this.pause.get()) {
            return;
        }
        packet2 = e.getPacket();
        if (packet2 instanceof ClickSlotC2SPacket) {
            ClickSlotC2SPacket click = (ClickSlotC2SPacket)packet2;
            switch (this.clickBypass.getValue().ordinal()) {
                case 3: {
                    if (click.actionType() == SlotActionType.PICKUP || click.actionType() == SlotActionType.PICKUP_ALL) break;
                    mc.getNetworkHandler().sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
                    break;
                }
                case 1: {
                    if (!NoSlow.mc.player.isOnGround() || NoSlow.mc.world.getBlockCollisions((Entity)NoSlow.mc.player, NoSlow.mc.player.getBoundingBox().offset(0.0, 0.0656, 0.0)).iterator().hasNext()) break;
                    if (NoSlow.mc.player.isSprinting()) {
                        mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)NoSlow.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    }
                    mc.getNetworkHandler().sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(NoSlow.mc.player.getX(), NoSlow.mc.player.getY() + 0.0656, NoSlow.mc.player.getZ(), false, false));
                    break;
                }
                case 2: {
                    if (!NoSlow.mc.player.isOnGround() || NoSlow.mc.world.getBlockCollisions((Entity)NoSlow.mc.player, NoSlow.mc.player.getBoundingBox().offset(0.0, 2.71875E-7, 0.0)).iterator().hasNext()) break;
                    if (NoSlow.mc.player.isSprinting()) {
                        mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)NoSlow.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    }
                    mc.getNetworkHandler().sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(NoSlow.mc.player.getX(), NoSlow.mc.player.getY() + 2.71875E-7, NoSlow.mc.player.getZ(), false, false));
                    break;
                }
                case 4: {
                    this.storedClicks.add(click);
                    e.cancel();
                }
            }
        }
        if (e.getPacket() instanceof CloseHandledScreenC2SPacket && this.clickBypass.is(Bypass.Delay)) {
            this.pause.set(true);
            while (!this.storedClicks.isEmpty()) {
                mc.getNetworkHandler().sendPacket((Packet)this.storedClicks.poll());
            }
            this.pause.set(false);
        }
    }

    @EventListener
    public void onPacketSendPost(PacketEvent.Sent e) {
        if (e.getPacket() instanceof ClickSlotC2SPacket && NoSlow.mc.player.isSprinting() && this.clickBypass.is(Bypass.NCP)) {
            mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)NoSlow.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    public boolean noSlow() {
        return this.isOn() && this.mode.getValue() != Mode.None && (this.mode.getValue() != Mode.Drop && this.mode.getValue() != Mode.GrimPacket || this.using);
    }

    public boolean soulSand() {
        return this.isOn() && this.soulSand.getValue();
    }

    public boolean climb() {
        return this.isOn() && this.climb.getValue();
    }

    public static enum Mode {
        Vanilla,
        NCP,
        Grim,
        GrimPacket,
        Drop,
        None;

    }

    private static enum Bypass {
        None,
        NCP,
        NCP2,
        Grim,
        Delay;

    }
}


package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoPearl extends Module {
    public static AutoPearl INSTANCE;
    public static boolean throwing = false;
    public final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true));
    private final EnumSetting<Timing> timing =
            add(new EnumSetting<>("Timing", Timing.All));
    public final EnumSetting<SwingSide> interactSwing = add(new EnumSetting<>("Swing", SwingSide.All));
    private final BooleanSetting rotation = add(new BooleanSetting("Rotation", true));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false).setParent());
    private final BooleanSetting whenElytra =
            add(new BooleanSetting("FallFlying", true, yawStep::isOpen));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, yawStep::isOpen));
    private final SliderSetting fov = add(new SliderSetting("Fov", 20, 0, 360, 0.1, yawStep::isOpen));
    private final SliderSetting priority = add(new SliderSetting("Priority", 100, 0, 100, yawStep::isOpen));
    private final BooleanSetting sync = add(new BooleanSetting("Sync", true, yawStep::isOpen));

    public AutoPearl() {
        super("AutoPearl", Category.Player);
        setChinese("扔珍珠");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        if (AutoPearl.INSTANCE.inventory.getValue()) {
            if (InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL) == -1) {
                disable();
                return;
            }
        } else {
            if (InventoryUtil.findItem(Items.ENDER_PEARL) == -1) {
                disable();
                return;
            }
        }
        if (shouldYawStep()) {
            return;
        }
        if (inventory.getValue() && !EntityUtil.inInventory()) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        throwPearl(mc.player.getYaw(), mc.player.getPitch());
        disable();
    }

    @EventListener
    public void onUpdate(TickEvent event) {
        if (nullCheck()) return;
        if (timing.is(Timing.Pre) && event.isPost() || timing.is(Timing.Post) && event.isPre()) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (!shouldYawStep()) {
            throwPearl(mc.player.getYaw(), mc.player.getPitch());
            disable();
        } else if (Alien.ROTATION.inFov(mc.player.getYaw(), mc.player.getPitch(), fov.getValueFloat())) {
            if (sync.getValue()) {
                throwPearl(mc.player.getYaw(), mc.player.getPitch());
            } else {
                throwing = true;
                int pearl;

                if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                    sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                    EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
                } else if (inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
                    InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                    sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                    EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
                    InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
                    EntityUtil.syncInventory();
                } else if ((pearl = InventoryUtil.findItem(Items.ENDER_PEARL)) != -1) {
                    int old = mc.player.getInventory().selectedSlot;
                    InventoryUtil.switchToSlot(pearl);
                    sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                    EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
                    InventoryUtil.switchToSlot(old);
                }
                throwing = false;
            }
            disable();
        }
    }

    @EventListener()
    public void onRotate(RotationEvent event) {
        if (shouldYawStep()) {
            event.setRotation(mc.player.getYaw(), mc.player.getPitch(), steps.getValueFloat(), priority.getValueFloat());
        }
    }

    private boolean shouldYawStep() {
        if (!whenElytra.getValue() && (mc.player.isGliding() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isGliding()))
            return false;
        return yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    public void throwPearl(float yaw, float pitch) {
        throwing = true;
        int pearl;
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            if (rotation.getValue())
                Alien.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
            if (rotation.getValue())
                Alien.ROTATION.snapBack();
        } else if (inventory.getValue() && (pearl = InventoryUtil.findItemInventorySlotFromZero(Items.ENDER_PEARL)) != -1) {
            InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            if (rotation.getValue())
                Alien.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
            InventoryUtil.inventorySwap(pearl, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            if (rotation.getValue())
                Alien.ROTATION.snapBack();
        } else if ((pearl = InventoryUtil.findItem(Items.ENDER_PEARL)) != -1) {
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(pearl);
            if (rotation.getValue())
                Alien.ROTATION.snapAt(yaw, pitch);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
            EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
            InventoryUtil.switchToSlot(old);
            if (rotation.getValue())
                Alien.ROTATION.snapBack();
        }
        throwing = false;
    }
}
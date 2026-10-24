package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class AutoMend extends Module {

    public static AutoMend INSTANCE;
    public final BooleanSetting rotation =
            add(new BooleanSetting("Rotation", true).setParent());
    private final BooleanSetting instant =
            add(new BooleanSetting("Instant", false, rotation::isOpen));
    public final BooleanSetting onlyBroken =
            add(new BooleanSetting("OnlyBroken", true));
    private final SliderSetting delay =
            add(new SliderSetting("Delay", 3, 0, 5));
    private final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true));
    public final EnumSetting<SwingSide> interactSwing = add(new EnumSetting<>("InteractSwing", SwingSide.All));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround =
            add(new BooleanSetting("OnlyGround", true));
    public final BooleanSetting autoDisable =
            add(new BooleanSetting("AutoDisable", true));
    private final Timer delayTimer = new Timer();
    boolean lookDown = false;
    int exp = 0;
    private boolean throwing = false;

    public AutoMend() {
        super("AutoMend", Category.Combat);
        setChinese("自动经验瓶");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        throwing = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        throwing = checkThrow();
        if (inventory.getValue() && !EntityUtil.inInventory()) return;
        if (lookDown && isThrow() && delayTimer.passed(delay.getValueInt() * 20L) && (!onlyGround.getValue() || mc.player.isOnGround())) {
            exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE) - 1;
            if (rotation.getValue() && instant.getValue()) {
                Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 88);
            }
            throwExp();
            if (rotation.getValue() && instant.getValue()) {
                Alien.ROTATION.snapBack();
            }
        }
        if (autoDisable.getValue()) {
            if (!isThrow()) {
                disable();
            }
        }
    }

    @Override
    public void onEnable() {
        lookDown = !rotation.getValue() || instant.getValue();
        if (nullCheck()) {
            disable();
            return;
        }
        exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public String getInfo() {
        return String.valueOf(exp);
    }

    public void throwExp() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (inventory.getValue() && (newSlot = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            delayTimer.reset();
        } else if ((newSlot = InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE)) != -1) {
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            EntityUtil.swingHand(Hand.MAIN_HAND, interactSwing.getValue());
            InventoryUtil.switchToSlot(oldSlot);
            delayTimer.reset();
        }
    }

    @EventListener(priority = EventPriority.LOWEST)
    public void RotateEvent(UpdateRotateEvent event) {
        if (!rotation.getValue() || instant.getValue()) return;
        if (isThrow()) {
            event.setPitch(88);
            lookDown = true;
        }
    }

    public boolean isThrow() {
        return throwing;
    }

    public boolean checkThrow() {
        if (this.isOff()) {
            return false;
        }
        if (AutoMend.mc.currentScreen != null) {
            return false;
        }
        if (this.usingPause.getValue() && AutoMend.mc.player.isUsingItem()) {
            return false;
        }
        if (!(InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE) != -1 || this.inventory.getValue() && InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1)) {
            return false;
        }
        if (this.onlyBroken.getValue()) {
            for (net.minecraft.entity.EquipmentSlot slot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.FEET, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.HEAD}) {
                ItemStack armor = AutoMend.mc.player.getEquippedStack(slot);
                if (armor.isEmpty() || EntityUtil.getDamagePercent(armor) >= 100) continue;
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(armor);
                var mending = AutoMend.mc.world.getRegistryManager().getOrThrow(Enchantments.MENDING.getRegistryRef()).getOrThrow(Enchantments.MENDING);
                return enchants.getEnchantments().contains(mending);
            }
        } else {
            return true;
        }
        return false;
    }
}

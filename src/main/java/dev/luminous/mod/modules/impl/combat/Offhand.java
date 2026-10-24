package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Offhand extends Module {
    private final EnumSetting<OffhandItem> item = add(new EnumSetting<>("Item", OffhandItem.Totem));
    private final BooleanSetting safe = add(new BooleanSetting("Safe", true).setParent());
    private final SliderSetting safeHealth = add(new SliderSetting("Health", 16.0f, 0.0f, 36.0f, 0.1, safe::isOpen));
    private final BooleanSetting lethalCrystal = add(new BooleanSetting("LethalCrystal", true, safe::isOpen));
    private final BooleanSetting gapSwitch = add(new BooleanSetting("GapSwitch", true).setParent());
    private final BooleanSetting always = add(new BooleanSetting("Always", false, gapSwitch::isOpen));
    private final BooleanSetting gapOnTotem = add(new BooleanSetting("Gap-Totem", false, gapSwitch::isOpen));
    private final BooleanSetting gapOnSword = add(new BooleanSetting("Gap-Sword", true, gapSwitch::isOpen));
    private final BooleanSetting gapOnPick = add(new BooleanSetting("Gap-Pickaxe", false, gapSwitch::isOpen));
    private final BooleanSetting mainHandTotem = add(new BooleanSetting("MainHandTotem", false).setParent());
    private final SliderSetting slot = add(new SliderSetting("Slot", 1, 1, 9, 1, mainHandTotem::isOpen));
    private final BooleanSetting forceUpdate = add(new BooleanSetting("ForceUpdate", false, mainHandTotem::isOpen));
    private final BooleanSetting withOffhand = add(new BooleanSetting("WithOffhand", false, mainHandTotem::isOpen));
    private final EnumSetting<SwapMode> swapMode = add(new EnumSetting<>("SwapMode", SwapMode.OffhandSwap));
    private final SliderSetting delay = add(new SliderSetting("Delay", 50.0, 0.0, 500.0, 1));
    private final EnumSetting<Timing> timing =
            add(new EnumSetting<>("Timing", Timing.All));
    private final Timer timer = new Timer();

    public Offhand() {
        super("Offhand", Category.Combat);
        setChinese("副手物品");
    }

    @EventListener
    public void totem(TotemEvent event) {
        if (event.getPlayer() == Offhand.mc.player) {
            if (Offhand.mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                Offhand.mc.player.getInventory().removeStack(0);
            } else if (Offhand.mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                Offhand.mc.player.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    private boolean lethalCrystal() {
        if (!lethalCrystal.getValue()) {
            return false;
        }
        for (final Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= 12.0f && AutoCrystal.INSTANCE.calculateDamage(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), mc.player, mc.player) >= EntityUtil.getHealth(mc.player)) {
                return true;
            }
        }
        return false;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (timing.is(Timing.Pre) && event.isPost() || timing.is(Timing.Post) && event.isPre()) return;

        if (!timer.passed(delay.getValueInt())) {
            return;
        }
        if (!EntityUtil.inInventory()) {
            return;
        }
        boolean switchMainHandTotem = mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || withOffhand.getValue();
        boolean unsafe = EntityUtil.getHealth(mc.player) < safeHealth.getValue() || lethalCrystal();
        if (mainHandTotem.getValue()) {
            int totemSlot = InventoryUtil.findItemInventorySlot(Items.TOTEM_OF_UNDYING);
            if (totemSlot != -1 && mc.player.getInventory().getStack(slot.getValueInt() - 1).getItem() != Items.TOTEM_OF_UNDYING) {
                switch (swapMode.getValue()) {
                    case ClickSlot -> {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.getValueInt() - 1 + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
                        EntityUtil.syncInventory();
                    }
                    case OffhandSwap -> {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, slot.getValueInt() - 1, SlotActionType.SWAP, mc.player);
                        EntityUtil.syncInventory();
                    }
                    case Pick -> {
                        int old = mc.player.getInventory().selectedSlot;
                        InventoryUtil.switchToSlot(slot.getValueInt() - 1);
                        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(totemSlot));
                        InventoryUtil.switchToSlot(old);
                    }
                }
                if (switchMainHandTotem) {
                    if (!safe.getValue() || unsafe) {
                        if (slot.getValueInt() - 1 != mc.player.getInventory().selectedSlot || forceUpdate.getValue()) {
                            InventoryUtil.switchToSlot(slot.getValueInt() - 1);
                        }
                    }
                }
                timer.reset();
                return;
            }
        }
        if (safe.getValue()) {
            if (unsafe) {
                if (mainHandTotem.getValue() && switchMainHandTotem) {
                    int hotBarSlot = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING);
                    if (hotBarSlot != -1) {
                        if (hotBarSlot != mc.player.getInventory().selectedSlot || forceUpdate.getValue()) {
                            InventoryUtil.switchToSlot(hotBarSlot);
                        }
                    }
                } else {
                    swap(Items.TOTEM_OF_UNDYING);
                    timer.reset();
                    return;
                }
            }
        } else if (mainHandTotem.getValue() && (switchMainHandTotem)) {
            int hotBarSlot = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING);
            if (hotBarSlot != -1) {
                if (hotBarSlot != mc.player.getInventory().selectedSlot || forceUpdate.getValue()) {
                    InventoryUtil.switchToSlot(hotBarSlot);
                }
            }
        }
        if ((this.gapOnSword.getValue() && Offhand.mc.player.getMainHandStack().isIn(ItemTags.SWORDS) || this.always.getValue() && Offhand.mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE && Offhand.mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE || this.gapOnPick.getValue() && Offhand.mc.player.getMainHandStack().isIn(ItemTags.PICKAXES) || this.gapOnTotem.getValue() && Offhand.mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) && Offhand.mc.options.useKey.isPressed() && this.gapSwitch.getValue()) {
            this.swap(Items.GOLDEN_APPLE);
            this.timer.reset();
            return;
        }
        EnumSetting<OffhandItem> item = this.item;
        OffhandItem i = item.getValue();
        if (i == OffhandItem.Shield) {
            swap(Items.SHIELD);
            timer.reset();
            return;
        }
        if (i == OffhandItem.Chorus) {
            swap(Items.CHORUS_FRUIT);
            timer.reset();
            return;
        }
        if (i == OffhandItem.Crystal) {
            swap(Items.END_CRYSTAL);
            timer.reset();
            return;
        }
        if (i == OffhandItem.Totem) {
            swap(Items.TOTEM_OF_UNDYING);
            timer.reset();
            return;
        }
        if (i == OffhandItem.Gapple) {
            swap(Items.GOLDEN_APPLE);
            timer.reset();
        }
    }

    private void swap(Item item) {
        int itemSlot = item == Items.GOLDEN_APPLE ? getGAppleSlot() : findItemInventorySlot(item);
        if (itemSlot == -1) return;

        switch (swapMode.getValue()) {
            case OffhandSwap -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 40, SlotActionType.SWAP, mc.player);
                EntityUtil.syncInventory();
            }
            case Pick -> {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(itemSlot));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN, 0));
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(itemSlot));
                break;
            }
            case ClickSlot -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                EntityUtil.syncInventory();
            }
        }
    }

    private int getGAppleSlot() {
        return findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) != -1 ? findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) : findItemInventorySlot(Items.GOLDEN_APPLE);
    }

    @Override
    public String getInfo() {
        return item.getValue().name();
    }

    public int findItemInventorySlot(Item item) {
        if (mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE && item == Items.GOLDEN_APPLE) {
            return -1;
        }
        if (mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE && (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE)) {
            return -1;
        }
        if (item == mc.player.getOffHandStack().getItem()) {
            return -1;
        }
        switch (swapMode.getValue()) {
            case Pick -> {
                for (int i = 9; i < mc.player.getInventory().size() + 1; i++) {
                    ItemStack s = mc.player.getInventory().getStack(i);
                    if (s.getItem() != item) continue;
                    return i;
                }

                for (int i = 0; i < 9; i++) {
                    ItemStack s = mc.player.getInventory().getStack(i);
                    if (s.getItem() != item) continue;
                    return i;
                }
            }
            case OffhandSwap, ClickSlot -> {
                for (int i = 44; i >= 0; --i) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == item) return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    public enum OffhandItem {
        None,
        Totem,
        Crystal,
        Gapple,
        Shield,
        Chorus
    }

    public enum SwapMode {
        ClickSlot,
        OffhandSwap,
        Pick,
    }
}

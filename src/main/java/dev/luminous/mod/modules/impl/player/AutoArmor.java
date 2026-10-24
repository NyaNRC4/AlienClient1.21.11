/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.component.type.ItemEnchantmentsComponent
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.enchantment.Enchantments
 *  net.minecraft.entity.EquipmentSlot
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.ArmorItem
 *  net.minecraft.item.ElytraItem
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
 *  net.minecraft.registry.entry.RegistryEntry
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.util.Hand
 */
package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoArmor
extends Module {
    public static AutoArmor INSTANCE;
    private final EnumSetting<EnchantPriority> head = this.add(new EnumSetting<EnchantPriority>("Head", EnchantPriority.Protection));
    private final EnumSetting<EnchantPriority> body = this.add(new EnumSetting<EnchantPriority>("Body", EnchantPriority.Protection));
    private final EnumSetting<EnchantPriority> tights = this.add(new EnumSetting<EnchantPriority>("Tights", EnchantPriority.Protection));
    private final EnumSetting<EnchantPriority> feet = this.add(new EnumSetting<EnchantPriority>("Feet", EnchantPriority.Protection));
    private final BooleanSetting ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
    private final BooleanSetting noMove = this.add(new BooleanSetting("NoMove", false));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 3.0, 0.0, 10.0, 1.0));
    private final BooleanSetting autoElytra = this.add(new BooleanSetting("AutoElytra", true));
    private final EnumSetting<HotbarSwapMode> hotbarSwap = this.add(new EnumSetting<HotbarSwapMode>("HotbarSwap", HotbarSwapMode.Swap));
    private final EnumSetting<InventorySwapMode> inventorySwap = this.add(new EnumSetting<InventorySwapMode>("InventorySwap", InventorySwapMode.ClickSlot));
    private int tickDelay = 0;

    public AutoArmor() {
        super("AutoArmor", Category.Player);
        setChinese("自动穿甲");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!EntityUtil.inInventory()) {
            return;
        }
        if (AutoArmor.mc.player.playerScreenHandler != AutoArmor.mc.player.currentScreenHandler) {
            return;
        }
        if (MovementUtil.isMoving() && this.noMove.getValue()) {
            return;
        }
        if (this.tickDelay > 0) {
            --this.tickDelay;
            return;
        }
        this.tickDelay = this.delay.getValueInt();
        HashMap<EquipmentSlot, int[]> armorMap = new HashMap<EquipmentSlot, int[]>(4);
        armorMap.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(AutoArmor.mc.player.getInventory().getStack(36)), -1, -1});
        armorMap.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(AutoArmor.mc.player.getInventory().getStack(37)), -1, -1});
        armorMap.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(AutoArmor.mc.player.getInventory().getStack(38)), -1, -1});
        armorMap.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(AutoArmor.mc.player.getInventory().getStack(39)), -1, -1});
        for (int s = 0; s < 36; ++s) {
            ItemStack stack = AutoArmor.mc.player.getInventory().getStack(s);
            if ((stack.get(DataComponentTypes.EQUIPPABLE) == null && !stack.isOf(Items.ELYTRA)) || stack.isOf(Items.ELYTRA) && (ElytraFly.INSTANCE.isOff() && this.autoElytra.getValue() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.packet.getValue())) continue;
            int protection = this.getProtection(stack);
            EquipmentSlot slot = stack.isOf(Items.ELYTRA) ? EquipmentSlot.CHEST : stack.get(DataComponentTypes.EQUIPPABLE).slot();
            for (Map.Entry e : armorMap.entrySet()) {
                if (this.autoElytra.getValue() && ElytraFly.INSTANCE.isOn() && e.getKey() == EquipmentSlot.CHEST) {
                    if (!AutoArmor.mc.player.getInventory().getStack(38).isEmpty() && AutoArmor.mc.player.getInventory().getStack(38).isOf(Items.ELYTRA) && this.isElytraUsable(AutoArmor.mc.player.getInventory().getStack(38)) || ((int[])e.getValue())[2] != -1 && !AutoArmor.mc.player.getInventory().getStack(((int[])e.getValue())[2]).isEmpty() && AutoArmor.mc.player.getInventory().getStack(((int[])e.getValue())[2]).isOf(Items.ELYTRA) && this.isElytraUsable(AutoArmor.mc.player.getInventory().getStack(((int[])e.getValue())[2])) || stack.isEmpty() || !stack.isOf(Items.ELYTRA) || !this.isElytraUsable(stack)) continue;
                    ((int[])e.getValue())[2] = s;
                    continue;
                }
                if (protection <= 0 || e.getKey() != slot || protection <= ((int[])e.getValue())[1] || protection <= ((int[])e.getValue())[3]) continue;
                ((int[])e.getValue())[2] = s;
                ((int[])e.getValue())[3] = protection;
            }
        }
        for (Map.Entry equipmentSlotEntry : armorMap.entrySet()) {
            if (((int[])equipmentSlotEntry.getValue())[2] == -1) continue;
            if (((int[])equipmentSlotEntry.getValue())[2] < 9) {
                switch (this.hotbarSwap.getValue().ordinal()) {
                    case 0: {
                        int armorSlot = 44 - ((int[])equipmentSlotEntry.getValue())[0];
                        AutoArmor.mc.interactionManager.clickSlot(AutoArmor.mc.player.currentScreenHandler.syncId, armorSlot, ((int[])equipmentSlotEntry.getValue())[2], SlotActionType.SWAP, (PlayerEntity)AutoArmor.mc.player);
                        EntityUtil.syncInventory();
                        break;
                    }
                    case 1: {
                        int old = AutoArmor.mc.player.getInventory().selectedSlot;
                        InventoryUtil.switchToSlot(((int[])equipmentSlotEntry.getValue())[2]);
                        AutoArmor.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                        InventoryUtil.switchToSlot(old);
                    }
                }
            } else if (AutoArmor.mc.player.playerScreenHandler == AutoArmor.mc.player.currentScreenHandler) {
                int armorSlot = 44 - ((int[])equipmentSlotEntry.getValue())[0];
                int newArmorSlot = ((int[])equipmentSlotEntry.getValue())[2];
                switch (this.inventorySwap.getValue().ordinal()) {
                    case 1: {
                        AutoArmor.mc.interactionManager.clickSlot(AutoArmor.mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoArmor.mc.player);
                        AutoArmor.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                        AutoArmor.mc.interactionManager.clickSlot(AutoArmor.mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoArmor.mc.player);
                        break;
                    }
                    case 0: {
                        AutoArmor.mc.interactionManager.clickSlot(AutoArmor.mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoArmor.mc.player);
                        AutoArmor.mc.interactionManager.clickSlot(AutoArmor.mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoArmor.mc.player);
                        if (((int[])equipmentSlotEntry.getValue())[1] == -1) break;
                        AutoArmor.mc.interactionManager.clickSlot(AutoArmor.mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, (PlayerEntity)AutoArmor.mc.player);
                    }
                }
                EntityUtil.syncInventory();
            }
            return;
        }
    }

    private int getProtection(ItemStack is) {
        EquippableComponent eq = is.get(DataComponentTypes.EQUIPPABLE);
        if (eq == null && !is.isOf(Items.ELYTRA)) {
            return is.isEmpty() ? -1 : 0;
        }
        EquipmentSlot slot = is.isOf(Items.ELYTRA) ? EquipmentSlot.BODY : eq.slot();
        int prot = 0;
        if (is.isOf(Items.ELYTRA)) {
            if (!this.isElytraUsable(is)) {
                return 0;
            }
            prot = 1;
        } else {
            AttributeModifiersComponent mods = is.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (mods != null) {
                for (AttributeModifiersComponent.Entry entry : mods.modifiers()) {
                    if (entry.attribute().value() == EntityAttributes.ARMOR.value() && entry.modifier().operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                        prot += (int)entry.modifier().value();
                    }
                }
            }
        }
        int blastMultiplier = 1;
        int protectionMultiplier = 1;
        switch (slot) {
            case HEAD: {
                if (this.head.is(EnchantPriority.Protection)) {
                    protectionMultiplier *= 2;
                    break;
                }
                blastMultiplier *= 2;
                break;
            }
            case BODY: {
                if (this.body.is(EnchantPriority.Protection)) {
                    protectionMultiplier *= 2;
                    break;
                }
                blastMultiplier *= 2;
                break;
            }
            case LEGS: {
                if (this.tights.is(EnchantPriority.Protection)) {
                    protectionMultiplier *= 2;
                    break;
                }
                blastMultiplier *= 2;
                break;
            }
            case FEET: {
                if (this.feet.is(EnchantPriority.Protection)) {
                    protectionMultiplier *= 2;
                    break;
                }
                blastMultiplier *= 2;
            }
        }
        if (is.hasEnchantments()) {
            Registry<Enchantment> registry = AutoArmor.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            int protLevel = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.PROTECTION), is);
            if (protLevel > 0) {
                prot += protLevel * protectionMultiplier;
            }
            int blastLevel = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.BLAST_PROTECTION), is);
            if (blastLevel > 0) {
                prot += blastLevel * blastMultiplier;
            }
            if (this.ignoreCurse.getValue() && EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.BINDING_CURSE), is) > 0) {
                prot = -999;
            }
        }
        return prot;
    }

    private boolean isElytraUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    private static enum EnchantPriority {
        Blast,
        Protection;

    }

    public static enum HotbarSwapMode {
        Swap,
        Switch;

    }

    public static enum InventorySwapMode {
        ClickSlot,
        Pick;

    }
}


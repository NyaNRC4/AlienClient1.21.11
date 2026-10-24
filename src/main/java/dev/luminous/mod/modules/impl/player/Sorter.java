package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.Manager;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Sorter extends Module {
    public static Sorter INSTANCE;
    private enum Mode {
        Whitelist,
        Blacklist
    }

    final int[] stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final SliderSetting tasksPerTicks = add(new SliderSetting("TasksPerTick", 2, 1, 20));
    private final SliderSetting delay = add(new SliderSetting("Delay", 0.1, 0, 5, 0.01).setSuffix("s"));
    private final BooleanSetting stack = add(new BooleanSetting("Stack", true));
    private final EnumSetting<Mode> trashMode = add(new EnumSetting<>("TrashMode", Mode.Whitelist));
    private final BooleanSetting edit = add(new BooleanSetting("EditTrash", false).injectTask(this::openGui));
    private final BooleanSetting sort = add(new BooleanSetting("Sort", true));
    private final BooleanSetting kit = add(new BooleanSetting("Kit", false).injectTask(this::onEnable));
    private final StringSetting kitName = add(new StringSetting("KitName", "kit1"));
    private final BooleanSetting drop = add(new BooleanSetting("Drop", true).setParent());
    private final BooleanSetting trash = add(new BooleanSetting("Trash", true, drop::isOpen));
    private final BooleanSetting rename = add(new BooleanSetting("Rename", true, drop::isOpen));
    private final BooleanSetting kitExceed = add(new BooleanSetting("KitExceed", true, drop::isOpen));
    private final BooleanSetting exceed = add(new BooleanSetting("Exceed", true, drop::isOpen));
    private final SliderSetting crystal = add(new SliderSetting("Crystal", 4, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting exp = add(new SliderSetting("Exp", 4, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting totem = add(new SliderSetting("Totem", 6, 0, 36, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting eGapple = add(new SliderSetting("EGapple", 2, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting gapple = add(new SliderSetting("Gapple", 2, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting obsidian = add(new SliderSetting("Obsidian", 1, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting web = add(new SliderSetting("Web", 1, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting glowstone = add(new SliderSetting("Glowstone", 1, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting anchor = add(new SliderSetting("Anchor", 1, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting pearl = add(new SliderSetting("Pearl", 1, 0, 8, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting piston = add(new SliderSetting("Piston", 1, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting redstone = add(new SliderSetting("RedStone", 1, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting ladder = add(new SliderSetting("Ladder", 2, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting bed = add(new SliderSetting("Bed", 4, 0, 12, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting speed = add(new SliderSetting("Speed", 1, 0, 8, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting turtle = add(new SliderSetting("Resistance", 1, 0, 8, () -> drop.isOpen() && exceed.isOpen()));
    private final SliderSetting strength = add(new SliderSetting("Strength", 1, 0, 8, () -> drop.isOpen() && exceed.isOpen()));
    private final Timer timer = new Timer();
    private final Map<Integer, String> kitMap = new ConcurrentHashMap<>();

    public Sorter() {
        super("Sorter", Category.Player);
        setChinese("背包整理");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        Alien.THREAD.execute(() -> {
            kitMap.clear();
            try {
                File file = Manager.getFile(kitName.getValue() + ".kit");
                if (!file.exists())
                    return;
                List<String> list = IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8);

                for (String s : list) {
                    String[] split = s.split(":");
                    if (split.length != 2) return;
                    kitMap.put(Integer.valueOf(split[0]), split[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static int getItemCount(Item item) {
        int count = 0;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (entry.getValue().getItem() != item) continue;
            count++;
        }
        if (mc.player.getOffHandStack().getItem() == item) {
            count++;
        }
        return count;
    }

    public static int getItemCount(Class<?> clazz) {
        int count = 0;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (entry.getValue().getItem() instanceof BlockItem && clazz.isInstance(((BlockItem) entry.getValue().getItem()).getBlock())) {
                count++;
            }
        }
        return count;
    }

    private void openGui() {
        edit.setValueWithoutTask(false);
        if (!nullCheck()) {
            mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.CLEANER)));
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!timer.passedS(delay.getValue())) return;
        if (!EntityUtil.inInventory()) {
            return;
        }
        if (exceed.getValue()) updateItem();
        for (int i = 0; i < tasksPerTicks.getValue(); i++) {
            tweak();
        }
    }

    private void tweak() {
        if (drop.getValue()) {
            for (int slot1 = 35; slot1 >= 0; --slot1) {
                ItemStack stack = mc.player.getInventory().getStack(slot1);
                if (stack.isEmpty()) continue;
                if (shouldDrop(stack)) {
                    timer.reset();
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (slot1 < 9 ? slot1 + 36 : slot1), 1, SlotActionType.THROW, mc.player);
                    return;
                }
            }
        }
        if (stack.getValue()) {
            for (int slot1 = 35; slot1 >= 9; --slot1) {
                ItemStack stack = mc.player.getInventory().getStack(slot1);
                if (stack.isEmpty()) continue;
                if (!stack.isStackable()) continue;
                if (stack.getCount() == stack.getMaxCount()) continue;
                for (int slot2 = 0; slot2 < 36; ++slot2) {
                    if (slot1 == slot2) continue;
                    ItemStack stack2 = mc.player.getInventory().getStack(slot2);
                    if (stack2.getCount() == stack2.getMaxCount()) continue;
                    if (canMerge(stack, stack2)) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2 < 9 ? slot2 + 36 : slot2, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                        timer.reset();
                        return;
                    }
                }
            }
        }
        if (drop.getValue()) {
            for (int slot1 = 35; slot1 >= 0; --slot1) {
                ItemStack stack = mc.player.getInventory().getStack(slot1);
                if (stack.isEmpty()) continue;
                if (exceed.getValue() && exceed(stack, false)) {
                    timer.reset();
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (slot1 < 9 ? slot1 + 36 : slot1), 1, SlotActionType.THROW, mc.player);
                    return;
                }
            }
        }
        if (sort.getValue()) {
            if (kit.getValue()) {
                for (int slot1 = 0; slot1 < 36; ++slot1) {
                    if (!kitMap.containsKey(slot1)) continue;
                    String target = kitMap.get(slot1);
                    String name = mc.player.getInventory().getStack(slot1).getItem().getTranslationKey();
                    if (!name.equals(target)) {
                        for (int slot2 = 0; slot2 < 36; ++slot2) {
                            String slot2Target = kitMap.get(slot2);
                            ItemStack stack = mc.player.getInventory().getStack(slot2);
                            if (stack.isEmpty()) continue;
                            String itemID = stack.getItem().getTranslationKey();
                            if (!itemID.equals(slot2Target) && itemID.equals(target)) {
                                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 0, SlotActionType.PICKUP, mc.player);
                                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2 < 9 ? slot2 + 36 : slot2, 0, SlotActionType.PICKUP, mc.player);
                                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 0, SlotActionType.PICKUP, mc.player);
                                timer.reset();
                                return;
                            }
                        }
                    }
                }
            } else {
                for (int slot1 = 9; slot1 < 36; ++slot1) {
                    int id = Item.getRawId(mc.player.getInventory().getStack(slot1).getItem());
                    if (mc.player.getInventory().getStack(slot1).isEmpty()) {
                        id = 114514;
                    }
                    int minId = getMinId(slot1, id);

                    if (minId < id) {
                        for (int slot2 = 35; slot2 > slot1; --slot2) {
                            ItemStack stack = mc.player.getInventory().getStack(slot2);
                            if (stack.isEmpty()) continue;
                            int itemID = Item.getRawId(stack.getItem());
//                        System.out.println("searchSlot:" + slot2 + " id:" + itemID);
                            if (itemID == minId) {
//                            System.out.println("targetSlot:" + slot2);
                                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP, mc.player);
                                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
                                timer.reset();
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (drop.getValue() && kitExceed.getValue()) {
            for (int slot1 = 35; slot1 >= 0; --slot1) {
                if (!kitMap.containsKey(slot1)) continue;
                ItemStack stack = mc.player.getInventory().getStack(slot1);
                if (exceed.getValue() && !exceed(stack, true)) continue;
                if (stack.isEmpty()) continue;
                if (stack.get(DataComponentTypes.EQUIPPABLE) != null) continue;
                if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
                    continue;
                if (!stack.getItem().getTranslationKey().equals(kitMap.get(slot1))) {
                    timer.reset();
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (slot1 < 9 ? slot1 + 36 : slot1), 1, SlotActionType.THROW, mc.player);
                    return;
                }
            }
        }
    }

    private boolean shouldDrop(ItemStack stack) {
        Item item = stack.getItem();
        if (trash.getValue()) {
            if (!(item instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
                boolean inList = Alien.CLEANER.inList(item.getTranslationKey());
                if (!inList && trashMode.is(Mode.Whitelist) || inList && trashMode.is(Mode.Blacklist)) {
                    return true;
                }
            }
        }
        if (rename.getValue()) {
            return stack.isStackable() && !stack.getName().getString().equals(item.getName().getString());
        }
        return false;
    }

    private boolean exceed(final ItemStack i, boolean dropOther) {
        if (i.getItem().equals(Items.END_CRYSTAL)) {
            if (this.stealCountList[0] > crystal.getValue()) {
                stealCountList[0] = stealCountList[0] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Items.EXPERIENCE_BOTTLE)) {
            if (this.stealCountList[1] > exp.getValue()) {
                stealCountList[1] = stealCountList[1] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Items.TOTEM_OF_UNDYING)) {
            if (this.stealCountList[2] > totem.getValue()) {
                stealCountList[2] = stealCountList[2] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE)) {
            if (this.stealCountList[3] > eGapple.getValue()) {
                stealCountList[3] = stealCountList[3] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Blocks.OBSIDIAN.asItem())) {
            if (this.stealCountList[4] > obsidian.getValue()) {
                stealCountList[4] = stealCountList[4] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Blocks.COBWEB.asItem())) {
            if (this.stealCountList[5] > web.getValue()) {
                stealCountList[5] = stealCountList[5] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Blocks.GLOWSTONE.asItem())) {
            if (this.stealCountList[6] > glowstone.getValue()) {
                stealCountList[6] = stealCountList[6] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem())) {
            if (this.stealCountList[7] > anchor.getValue()) {
                stealCountList[7] = stealCountList[7] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Items.ENDER_PEARL)) {
            if (this.stealCountList[8] > pearl.getValue()) {
                stealCountList[8] = stealCountList[8] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem) i.getItem()).getBlock() instanceof PistonBlock) {
            if (this.stealCountList[9] > piston.getValue()) {
                stealCountList[9] = stealCountList[9] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem())) {
            if (this.stealCountList[10] > redstone.getValue()) {
                stealCountList[10] = stealCountList[10] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem) i.getItem()).getBlock() instanceof BedBlock) {
            if (this.stealCountList[11] > bed.getValue()) {
                stealCountList[11] = stealCountList[11] - 1;
                return true;
            } else {
                return false;
            }
        }
        if (Item.getRawId(i.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = i.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                    if (this.stealCountList[12] > speed.getValue()) {
                        stealCountList[12] = stealCountList[12] - 1;
                        return true;
                    } else {
                        return false;
                    }
                } else if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                    if (this.stealCountList[13] > turtle.getValue()) {
                        stealCountList[13] = stealCountList[13] - 1;
                        return true;
                    } else {
                        return false;
                    }
                } else if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                    if (this.stealCountList[16] > strength.getValue()) {
                        stealCountList[16] = stealCountList[16] - 1;
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        if (i.getItem().equals(Items.GOLDEN_APPLE) && this.stealCountList[14] > gapple.getValue()) {
            stealCountList[14] = stealCountList[14] - 1;
            return true;
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem) i.getItem()).getBlock() instanceof LadderBlock) {
            if (this.stealCountList[15] > ladder.getValue()) {
                stealCountList[15] = stealCountList[15] - 1;
                return true;
            }
        }
        return dropOther;
    }

    private void updateItem() {
        this.stealCountList[0] = getItemCount(Items.END_CRYSTAL);
        this.stealCountList[1] = getItemCount(Items.EXPERIENCE_BOTTLE);
        this.stealCountList[2] = getItemCount(Items.TOTEM_OF_UNDYING);
        this.stealCountList[3] = getItemCount(Items.ENCHANTED_GOLDEN_APPLE);
        this.stealCountList[4] = getItemCount(Items.OBSIDIAN);
        this.stealCountList[5] = getItemCount(Items.COBWEB);
        this.stealCountList[6] = getItemCount(Items.GLOWSTONE);
        this.stealCountList[7] = getItemCount(Items.RESPAWN_ANCHOR);
        this.stealCountList[8] = getItemCount(Items.ENDER_PEARL);
        this.stealCountList[9] = getItemCount(Items.PISTON) - getItemCount(Items.STICKY_PISTON);
        this.stealCountList[10] = getItemCount(Items.REDSTONE_BLOCK);
        this.stealCountList[11] = getItemCount(BedBlock.class);
        this.stealCountList[12] = InventoryUtil.getPotionCount(StatusEffects.SPEED.value());
        this.stealCountList[13] = InventoryUtil.getPotionCount(StatusEffects.RESISTANCE.value());
        this.stealCountList[14] = getItemCount(Items.GOLDEN_APPLE);
        this.stealCountList[15] = getItemCount(LadderBlock.class);
        this.stealCountList[16] = InventoryUtil.getPotionCount(StatusEffects.STRENGTH.value());
    }

    private int getMinId(int slot, int currentId) {
        int id = currentId;
        for (int slot1 = slot + 1; slot1 < 36; ++slot1) {
            ItemStack stack = mc.player.getInventory().getStack(slot1);
            if (stack.isEmpty()) continue;
            int itemID = Item.getRawId(stack.getItem());
            if (itemID < id) {
                id = itemID;
            }
        }
//        System.out.println("inputSlot:" + slot + " currentId:" + currentId + " minId:" + id);
        return id;
    }

    public static boolean canMerge(ItemStack source, ItemStack stack) {
        return ItemStack.areItemsAndComponentsEqual(source, stack);
        //return source.getItem() == stack.getItem() && source.getName().equals(stack.getName());
    }

}

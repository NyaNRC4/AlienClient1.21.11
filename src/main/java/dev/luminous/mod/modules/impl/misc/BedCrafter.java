package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

public class BedCrafter extends Module {
    public static BedCrafter INSTANCE;
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", false));
    private final SliderSetting range = add(new SliderSetting("Range", 5, 0, 8));
    private final BooleanSetting disable = add(new BooleanSetting("Disable", true));

    public BedCrafter() {
        super("BedCrafter", Category.Misc);
        setChinese("自动制作床");
        INSTANCE = this;
    }

    public static int getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        int wool = 0, plank = 0;
        for(int i=0;i<36;i++){
            ItemStack stack = mc.player.getInventory().getStack(i);
            if(stack.getItem() == Items.WHITE_WOOL || stack.getItem() == Items.LIGHT_GRAY_WOOL || stack.getItem() == Items.GRAY_WOOL || stack.getItem() == Items.BLACK_WOOL || stack.getItem() == Items.BROWN_WOOL || stack.getItem() == Items.RED_WOOL || stack.getItem() == Items.ORANGE_WOOL || stack.getItem() == Items.YELLOW_WOOL || stack.getItem() == Items.LIME_WOOL || stack.getItem() == Items.GREEN_WOOL || stack.getItem() == Items.CYAN_WOOL || stack.getItem() == Items.LIGHT_BLUE_WOOL || stack.getItem() == Items.BLUE_WOOL || stack.getItem() == Items.PURPLE_WOOL || stack.getItem() == Items.MAGENTA_WOOL || stack.getItem() == Items.PINK_WOOL){
                wool += stack.getCount();
            }
            if(stack.getItem() == Items.OAK_PLANKS || stack.getItem() == Items.SPRUCE_PLANKS || stack.getItem() == Items.BIRCH_PLANKS || stack.getItem() == Items.JUNGLE_PLANKS || stack.getItem() == Items.ACACIA_PLANKS || stack.getItem() == Items.DARK_OAK_PLANKS || stack.getItem() == Items.MANGROVE_PLANKS || stack.getItem() == Items.CHERRY_PLANKS || stack.getItem() == Items.BAMBOO_PLANKS){
                plank += stack.getCount();
            }
        }
        if(wool <3 || plank <3) return;

        if (getEmptySlots() == 0) {
            if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                mc.player.closeHandledScreen();
            }
            if (disable.getValue()) disable();
            return;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler craftingHandler) {
            int maxPerTick = 2;
            int crafted = 0;
            for (RecipeResultCollection recipeResult : mc.player.getRecipeBook().getOrderedResults()) {
                for (RecipeDisplayEntry recipeDisplayEntry : recipeResult.getAllRecipes()) {
                    RecipeDisplay display = recipeDisplayEntry.display();
                    SlotDisplay slotDisplay = display.result();

                    if (slotDisplay instanceof SlotDisplay.StackSlotDisplay) {
                        SlotDisplay.StackSlotDisplay stackSlotDisplay = (SlotDisplay.StackSlotDisplay) slotDisplay;
                        ItemStack previewStack = stackSlotDisplay.stack();
                        if (previewStack.getItem() instanceof BedItem) {
                            while(crafted < maxPerTick && getEmptySlots() > 0) {
                                mc.interactionManager.clickRecipe(craftingHandler.syncId, recipeDisplayEntry.id(), false);
                                mc.interactionManager.clickSlot(craftingHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, mc.player);
                                crafted++;
                            }
                            if(crafted >= maxPerTick) {
                                return;
                            }
                        }
                    }
                }
            }
            mc.player.closeHandledScreen();
            if (disable.getValue()) disable();
        }
        else {
            doPlace();
        }
    }

    private void doPlace() {
        BlockPos bestPos = null;
        double distance = 100;
        boolean foundTable = false;

        for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat())) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
                bestPos = pos;
                foundTable = true;
                break;
            }
            if (BlockUtil.canPlace(pos)) {
                double dist = mc.player.squaredDistanceTo(pos.toCenterPos());
                if (bestPos == null || dist < distance) {
                    bestPos = pos;
                    distance = dist;
                }
            }
        }

        if (bestPos == null) return;

        if (foundTable) {
            BlockUtil.clickBlock(bestPos, BlockUtil.getClickSide(bestPos), rotate.getValue());
        } else {
            if (InventoryUtil.findItem(Items.CRAFTING_TABLE) == -1) return;
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.CRAFTING_TABLE));
            BlockUtil.placeBlock(bestPos, rotate.getValue());
            InventoryUtil.switchToSlot(old);
        }
    }
}

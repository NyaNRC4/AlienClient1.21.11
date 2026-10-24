package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.combat.Aura;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Bot extends Module {
    public static Bot INSTANCE;
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.AutoTrade));
    private final BooleanSetting autoEat = add(new BooleanSetting("AutoEat", true).setParent());
    private final SliderSetting hunger =
            add(new SliderSetting("Hunger", 10, 0, 20, 1, autoEat::isOpen));
    private final SliderSetting health =
            add(new SliderSetting("Health", 20, 0, 36, .1, autoEat::isOpen));
    private final BooleanSetting anyFood = add(new BooleanSetting("AnyFood", false, autoEat::isOpen));
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));

    private final SliderSetting delay = add(new SliderSetting("Delay", 5, 0, 100, () -> mode.is(Mode.TridentDupe)));
    private final BooleanSetting dropTridents = add(new BooleanSetting("DropTridents", true, () -> mode.is(Mode.TridentDupe)));

    private final BooleanSetting durabilityManagement = add(new BooleanSetting("DurabilityManagement", true, () -> mode.is(Mode.TridentDupe)));

    public final BooleanSetting edit = add(new BooleanSetting("Edit", false, () -> mode.is(Mode.AutoTrade)).injectTask(this::openAutoTradeEdit));
    public final SliderSetting repeatSetting = add(new SliderSetting("Repeat", 2, 1, 15, 1, () -> mode.is(Mode.AutoTrade)));
    public final BooleanSetting autoCloseSetting = add(new BooleanSetting("AutoClose", true, () -> mode.is(Mode.AutoTrade)));
    public final BooleanSetting timeoutCloseSetting = add(new BooleanSetting("TimeoutClose", true, () -> mode.is(Mode.AutoTrade)));
    public final SliderSetting timeOutSetting = add(new SliderSetting("Timeout", 1, 0, 15, 0.1, () -> mode.is(Mode.AutoTrade)));
    public final BooleanSetting autoOpenSetting = add(new BooleanSetting("AutoOpen", true, () -> mode.is(Mode.AutoTrade)));

    private final SliderSetting range =
            add(new SliderSetting("Range", 4, 0, 8, .1, () -> mode.is(Mode.SlabPlacer) || mode.is(Mode.AutoTrade)));

    private final BooleanSetting inventory = add(new BooleanSetting("Inventory", true, () -> mode.is(Mode.NPlusOneDupe)));
    private final BooleanSetting ai = add(new BooleanSetting("AI", true, () -> mode.is(Mode.SandMiner)));
    private final BooleanSetting nuker = add(new BooleanSetting("Nuker", true, () -> mode.is(Mode.SandMiner)));
    private final BooleanSetting redSand = add(new BooleanSetting("RedSand", false, () -> mode.is(Mode.SandMiner)));

    private final SliderSetting breaks =
            add(new SliderSetting("Breaks", 10, 0, 20, () -> mode.is(Mode.SandMiner)));
    private final SliderSetting maxTime =
            add(new SliderSetting("MaxTime", 60, 0, 100, () -> mode.is(Mode.Ominous)));

    public final SliderSetting placeDelay =
            add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> mode.is(Mode.SlabPlacer)));
    private final SliderSetting blocksPer =
            add(new SliderSetting("BlocksPer", 1, 1, 8, () -> mode.is(Mode.SlabPlacer)));

    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true, () -> mode.is(Mode.SlabPlacer)));

    private final StringSetting name = add(new StringSetting("Name", "KizuatoResu", () -> mode.is(Mode.ScoreFarmer)));
    private final BooleanSetting getScore = add(new BooleanSetting("GetScore", false, () -> mode.is(Mode.ScoreFarmer)));
    private final Timer commandTimer = new Timer();
    private final Timer duelItemTimer = new Timer();

    final List<BlockPos> emptyBox = new ArrayList<>();
    final List<TurtleEntity> inLove = new ArrayList<>();
    final Timer timeOut = new Timer();
    final Timer closeScreen = new Timer();
    final Timer openTimeOut = new Timer();
    final Timer putTimer = new Timer();
    final Timer ominousTimer = new Timer();
    Stage stage = Stage.Open;
    BlockPos boxPos;
    boolean closeToBox;
    boolean putIn;
    LlamaEntity llama;
    boolean storageSand = false;
    boolean endEat = false;
    final Timer craftTimer = new Timer();
    final Timer screenTimeout = new Timer();
    int lastSlot = -1;
    int tick = 0;
    private final List<VillagerEntity> tradedVillager = new ArrayList<>();
    private final Timer timeoutTimer = new Timer();

    public Bot() {
        super("Bot", Category.Misc);
        setChinese("机器人");
        INSTANCE = this;
    }

    private void openAutoTradeEdit() {
        edit.setValueWithoutTask(false);
        if (!nullCheck()) {
            mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.TRADE)));
        }
    }

    private void placeBlock(BlockPos pos) {
        int block;
        if ((block = getBlock()) == -1) {
            return;
        }
        int oldSlot = mc.player.getInventory().selectedSlot;
        if (BlockUtil.canPlace(pos)) {

            if (BlockUtil.allowAirPlace()) {
                doSwap(block);
                BlockUtil.placedPos.add(pos);
                BlockUtil.airPlace(pos, rotate.getValue());
                if (inventory.getValue()) {
                    doSwap(block);
                    EntityUtil.syncInventory();
                } else {
                    doSwap(oldSlot);
                }
                return;
            }
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) return;
            doSwap(block);
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate.getValue());
            if (inventory.getValue()) {
                doSwap(block);
                EntityUtil.syncInventory();
            } else {
                doSwap(oldSlot);
            }
        }
    }

    int placeProgress = 0;
    private final Timer slabPlacerDelay = new Timer();

    private void tryPlaceBlock(BlockPos pos) {
        if (pos == null) return;
        if (!(placeProgress < blocksPer.getValue())) return;
        int block;
        if (inventory.getValue()) {
            block = InventoryUtil.findClassInventorySlot(SlabBlock.class);
        } else {
            block = InventoryUtil.findClass(SlabBlock.class);
        }
        if (block == -1) return;
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) return;
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (!BlockUtil.canPlace(pos, 6, true)) return;
        if (rotate.getValue()) {
            Alien.ROTATION.lookAt(directionVec);
        }
        if (BlockUtil.hasEntity(pos, false)) return;
        int old = mc.player.getInventory().selectedSlot;
        doSwap(block);
        BlockUtil.placedPos.add(pos);
        if (BlockUtil.allowAirPlace()) {
            BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, true);
        } else {
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND);
        }
        if (inventory.getValue()) {
            doSwap(block);
            EntityUtil.syncInventory();
        } else {
            doSwap(old);
        }
        if (rotate.getValue()) {
            Alien.ROTATION.snapBack();
        }
        placeProgress++;
        slabPlacerDelay.reset();
    }

    private void doSwap(int slot) {
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getBlock() {
        if (inventory.getValue()) {
            return InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
        } else {
            return InventoryUtil.findClass(ShulkerBoxBlock.class);
        }
    }

    @Override
    public String getInfo() {
        if (mode.is(Mode.XinDupe))
            return "Stage:" + stage.name() + ", Riding:" + mc.player.hasVehicle();
        return mode.getValue().name();
    }

    @Override
    public void onEnable() {
        emptyBox.clear();
        storageSand = false;
        stage = Stage.Summon;
        boxPos = null;
        closeToBox = false;
        llama = null;
        putIn = false;
        tick = 0;
        scheduledTasks.clear();
        scheduledTasks2.clear();
        if (mode.is(Mode.TridentDupe)) {
            tridentDupe();
        }
    }

    @EventListener(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (mode.is(Mode.TridentDupe)) {
            if (!cancel)
                return;
            if (event.getPacket() instanceof PlayerMoveC2SPacket
                    || event.getPacket() instanceof CloseHandledScreenC2SPacket)
                return;

            if (!(event.getPacket() instanceof ClickSlotC2SPacket)
                    && !(event.getPacket() instanceof PlayerActionC2SPacket)) {
                return;
            }

            event.cancel();
        }
    }

    private boolean cancel = true;

    private final List<Pair<Long, Runnable>> scheduledTasks = new ArrayList<>();
    private final List<Pair<Long, Runnable>> scheduledTasks2 = new ArrayList<>();

    private void tridentDupe() {
        int delayInt = delay.getValueInt() * 100;

        int lowestHotbarSlot = 0;
        int lowestHotbarDamage = 1000;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack((i)).getItem() == Items.TRIDENT) {
                int currentHotbarDamage = mc.player.getInventory().getStack((i)).getDamage();
                if (lowestHotbarDamage > currentHotbarDamage) {
                    lowestHotbarSlot = i;
                    lowestHotbarDamage = currentHotbarDamage;
                }
            }
        }
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        cancel = true;

        int finalLowestHotbarSlot = lowestHotbarSlot;
        scheduleTask(() -> {
            cancel = false;

            if (durabilityManagement.getValue()) {
                if (finalLowestHotbarSlot != 0) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (44), 0, SlotActionType.SWAP, mc.player);
                    if (dropTridents.getValue())
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (36 + finalLowestHotbarSlot), 0, SlotActionType.SWAP, mc.player);
                }
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 3, 0, SlotActionType.SWAP, mc.player);

            PlayerActionC2SPacket packet2 = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0);
            mc.getNetworkHandler().sendPacket(packet2);

            if (dropTridents.getValue())
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);

            cancel = true;
            scheduleTask2(this::tridentDupe, delayInt);
        }, delayInt);
    }

    public void scheduleTask(Runnable task, long delayMillis) {
        long executeTime = System.currentTimeMillis() + delayMillis;
        scheduledTasks.add(new Pair<>(executeTime, task));
    }

    public void scheduleTask2(Runnable task, long delayMillis) {
        long executeTime = System.currentTimeMillis() + delayMillis;
        scheduledTasks2.add(new Pair<>(executeTime, task));
    }

    @Override
    public void onDisable() {
        BaritoneUtil.cancelEverything();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) {
            emptyBox.clear();
            stage = Stage.Summon;
            boxPos = null;
            closeToBox = false;
            llama = null;
            putIn = false;
            return;
        }
        if (autoEat.getValue() && (mc.player.getHealth() + mc.player.getAbsorptionAmount() < health.getValueFloat() || mc.player.getHungerManager().getFoodLevel() < hunger.getValueInt())) {
            int food = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (food == -1) {
                food = InventoryUtil.findItem(Items.GOLDEN_APPLE);
            }
            if (food == -1) {
                food = InventoryUtil.findItem(Items.GOLDEN_CARROT);
            }
            if (food == -1 && anyFood.getValue()) {
                food = InventoryUtil.getFood();
            }
            if (food != -1) {
                if (mc.currentScreen != null && mode.is(Mode.XinDupe)) mc.currentScreen.close();

                if (mc.player.getInventory().selectedSlot != food) {
                    InventoryUtil.switchToSlot(food);
                }
                mc.options.useKey.setPressed(true);
                endEat = true;
                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            }
            BaritoneUtil.cancelEverything();
            return;
        }
        if (endEat) {
            endEat = false;
            mc.options.useKey.setPressed(false);
        }
        switch (mode.getValue()) {
            case ScoreFarmer -> {
                if (getScore.getValue()) {
                    if (commandTimer.passedS(4)) {
                        mc.player.networkHandler.sendChatCommand("duel " + name.getValue());
                        commandTimer.reset();
                    }
                }
                if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                    if (duelItemTimer.passedS(1)) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
                        duelItemTimer.reset();
                    }
                }
            }
            case TridentDupe -> {
                long currentTime = System.currentTimeMillis();
                {
                    Iterator<Pair<Long, Runnable>> iterator = scheduledTasks.iterator();

                    while (iterator.hasNext()) {
                        Pair<Long, Runnable> entry = iterator.next();
                        if (entry.getLeft() <= currentTime) {
                            entry.getRight().run();
                            iterator.remove(); // Remove executed task from the list
                        }
                    }
                }
                {
                    Iterator<Pair<Long, Runnable>> iterator = scheduledTasks2.iterator();

                    while (iterator.hasNext()) {
                        Pair<Long, Runnable> entry = iterator.next();
                        if (entry.getLeft() <= currentTime) {
                            entry.getRight().run();
                            iterator.remove(); // Remove executed task from the list
                        }
                    }
                }
            }
            case AutoTrade -> {
                if (mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
                    if (timeoutCloseSetting.getValue()) {
                        if (timeoutTimer.passedS(timeOutSetting.getValue())) {
                            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                            mc.currentScreen.close();
                            return;
                        }
                    }

                    int i = 0;

                    TradeOfferList list = handler.getRecipes();
                    for (int size = 0; size < list.size(); ++size) {
                        if (i >= repeatSetting.getValue()) return;
                        TradeOffer tradeOffer = list.get(size);
                        if (!tradeOffer.isDisabled()) {
                            if (Alien.TRADE.inWhitelist(tradeOffer.getSellItem().getItem().getTranslationKey())) {
                                while (i < repeatSetting.getValue()) {
                                    if (!tradeOffer.getDisplayedFirstBuyItem().isEmpty()) {
                                        int count = InventoryUtil.getItemCount(tradeOffer.getDisplayedFirstBuyItem().getItem());
                                        if (handler.getSlot(0).getStack().getItem() == tradeOffer.getDisplayedFirstBuyItem().getItem()) {
                                            count += handler.getSlot(0).getStack().getCount();
                                        }
                                        if (count < tradeOffer.getDisplayedFirstBuyItem().getCount()) {
                                            break;
                                        }
                                    }
                                    if (!tradeOffer.getDisplayedSecondBuyItem().isEmpty()) {
                                        int count = InventoryUtil.getItemCount(tradeOffer.getDisplayedSecondBuyItem().getItem());
                                        if (handler.getSlot(1).getStack().getItem() == tradeOffer.getDisplayedSecondBuyItem().getItem()) {
                                            count += handler.getSlot(1).getStack().getCount();
                                        }
                                        if (count < tradeOffer.getDisplayedSecondBuyItem().getCount()) {
                                            break;
                                        }
                                    }
                                    mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(size));
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 2, 1, SlotActionType.QUICK_MOVE, mc.player);
                                    i++;
                                }
                            }
                        }
                    }
                    if (autoCloseSetting.getValue()) {
                        if (i < repeatSetting.getValue()) {
                            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                            mc.currentScreen.close();
                        }
                    }
                } else {
                    timeoutTimer.reset();
                    if (autoOpenSetting.getValue()) {
                        for (Entity entity : Alien.THREAD.getEntities()) {
                            if (entity instanceof VillagerEntity villager) {
                                if (mc.player.distanceTo(villager) <= range.getValue()) {
                                    if (tradedVillager.contains(villager)) {
                                        continue;
                                    }
                                    tradedVillager.add(villager);
                                    if (rotate.getValue()) Alien.ROTATION.snapAt(villager.getEyePos());
                                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(villager, mc.player.isSneaking(), Hand.MAIN_HAND));
                                    if (rotate.getValue()) Alien.ROTATION.snapBack();
                                    return;
                                } else {
                                    tradedVillager.remove(villager);
                                }
                            }
                        }
                    }
                }
            }
            case SlabPlacer -> {
                if (inventory.getValue() && !EntityUtil.inInventory()) return;
                placeProgress = 0;
                if (!slabPlacerDelay.passed((long) placeDelay.getValue())) return;
                if (mc.player.isUsingItem() && usingPause.getValue()) {
                    return;
                }
                for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat(), mc.player.getEntityPos())) {
                    if (!mc.world.getBlockState(pos).isFullCube(mc.world, pos) && mc.world.getBlockState(pos).getBlock() instanceof SlabBlock)
                        continue;
                    if (mc.world.isAir(pos) || !BlockUtil.canReplace(pos.up())) continue;
                    tryPlaceBlock(pos.up());
                }
            }
            case ItemFrameDupe -> {
                tick++;
                int shulker = InventoryUtil.findClass(ShulkerBoxBlock.class);
                if (shulker != -1) {
                    for (Entity entity : Alien.THREAD.getEntities()) {
                        if (entity instanceof ItemFrameEntity itemFrameEntity) {
                            if (entity.distanceTo(mc.player) > 3) continue;
                            InventoryUtil.switchToSlot(shulker);
                            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(itemFrameEntity, false, Hand.MAIN_HAND));
                            if (tick >= 2) {
                                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(itemFrameEntity, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                                tick = 0;
                            }
                            return;
                        }
                    }
                }
                tick = 0;
            }
            case SandMiner -> {
                Block sandBlock = Blocks.SAND;
                if (redSand.getValue()) {
                    sandBlock = Blocks.RED_SAND;
                }
                if (Aura.INSTANCE.isOn()) {
                    if (Aura.INSTANCE.getTarget(Aura.INSTANCE.range.getValue()) != null) {
                        if (mc.currentScreen != null) mc.currentScreen.close();
                        int slot = findSwordSlot();
                        if (mc.player.getInventory().selectedSlot != slot) {
                            InventoryUtil.switchToSlot(slot);
                        }
                        BaritoneUtil.cancelEverything();
                        return;
                    }
                }
                if (mc.currentScreen == null) {
                    screenTimeout.reset();
                } else if (screenTimeout.passedS(5)) {
                    mc.currentScreen.close();
                }
                if (InventoryUtil.findClassInventorySlot(ShovelItem.class) == -1) {
                    BaritoneUtil.cancelEverything();
                    if (mc.player.currentScreenHandler instanceof CraftingScreenHandler craftingHandler) {
                        if (craftTimer.passedS(1)) {
                            int maxPerTick = 2;
                            int crafted = 0;
                            for (RecipeResultCollection recipeResult : mc.player.getRecipeBook().getOrderedResults()) {
                                for (RecipeDisplayEntry recipe : recipeResult.getAllRecipes()) {
                                    RecipeDisplay display = recipe.display();
                                    SlotDisplay slotDisplay = display.result();
                                    if (slotDisplay instanceof SlotDisplay.StackSlotDisplay stackSlotDisplay) {
                                        ItemStack previewStack = stackSlotDisplay.stack();
                                        if (previewStack.getItem() instanceof ShovelItem) {
                                            while(crafted < maxPerTick && BedCrafter.getEmptySlots() > 0) {
                                                mc.interactionManager.clickRecipe(craftingHandler.syncId, recipe.id(), false);
                                                mc.interactionManager.clickSlot(craftingHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, mc.player);
                                                crafted++;
                                                craftTimer.reset();
                                            }
                                            if(crafted >= maxPerTick) {
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                            mc.player.closeHandledScreen();
                        }
                    } else {
                        BlockPos bestPos = null;
                        double distance = 100;
                        for (BlockPos pos : BlockUtil.getSphere(3)) {
                            if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
                                BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), rotate.getValue());
                                return;
                            }
                            if (BlockUtil.canPlace(pos)) {
                                double dist = mc.player.squaredDistanceTo(pos.toCenterPos());
                                if (bestPos == null || Math.sqrt(dist) < distance) {
                                    bestPos = pos;
                                    distance = Math.sqrt(dist);
                                }
                            }
                        }
                        if (bestPos != null) {
                            int craftTable = InventoryUtil.findItemInventorySlot(Items.CRAFTING_TABLE);
                            if (craftTable == -1) return;
                            InventoryUtil.inventorySwap(craftTable, mc.player.getInventory().selectedSlot);
                            BlockUtil.placeBlock(bestPos, rotate.getValue());
                            InventoryUtil.inventorySwap(craftTable, mc.player.getInventory().selectedSlot);
                        }
                    }
                    return;
                }
                Entity hasShulkerItemEntity = null;
                for (Entity entity : Alien.THREAD.getEntities()) {
                    if (entity instanceof ItemEntity itemEntity) {
                        if (itemEntity.getStack().getItem() instanceof BlockItem item && (item.getBlock() instanceof ShulkerBoxBlock || item.getBlock() == Blocks.ENDER_CHEST)) {
                            hasShulkerItemEntity = itemEntity;
                            break;
                        }
                    }
                }
                if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                    if (mc.currentScreen != null) mc.currentScreen.close();
                }
                int sands = InventoryUtil.getItemCount(Items.SAND);
                if (sands >= 27 * 64) {
                    //CommandManager.sendMessage("Sand Full");
                    for (int slot1 = 9; slot1 < 36; ++slot1) {
                        ItemStack stack = mc.player.getInventory().getStack(slot1);
                        if (stack.isEmpty()) continue;
                        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == sandBlock && stack.getCount() < stack.getMaxCount()) {
                            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 1, SlotActionType.THROW, mc.player);
                        }
                    }
                    if (mc.currentScreen instanceof InventoryScreen) {
                        mc.currentScreen.close();
                        return;
                    }
                    int shulkerSlot = InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
                    BlockPos shulker = BlockUtil.getBlock(ShulkerBoxBlock.class, 3f);
                    if (mc.currentScreen instanceof HandledScreen<?> && mc.player.currentScreenHandler instanceof ScreenHandler screenHandler) {
                        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
                            //CommandManager.sendMessage("Take Shulker");
                            BaritoneUtil.cancelEverything();
                            if (shulkerSlot != -1) {
                                //CommandManager.sendMessage("Have shulker, Close EnderChest");
                                mc.currentScreen.close();
                            } else {
                                for (Slot slot : screenHandler.slots) {
                                    if (slot.id < 27 && slot.getStack().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock && !ShulkerViewer.hasItems(slot.getStack())) {
                                        mc.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                        break;
                                    }
                                }
                            }
                        } else {
                            //CommandManager.sendMessage("Fill Sand");
                            BaritoneUtil.cancelEverything();
                            storageSand = true;
                            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerBoxScreenHandler) {
                                for (Slot slot : shulkerBoxScreenHandler.slots) {
                                    if (slot.id < 27 && slot.getStack().isEmpty()) {
                                        for (Slot slot2 : shulkerBoxScreenHandler.slots) {
                                            if (slot2.id >= 27 && slot2.getStack().getItem() == Items.SAND && slot2.getStack().getCount() == slot2.getStack().getMaxCount()) {
                                                mc.interactionManager.clickSlot(shulkerBoxScreenHandler.syncId, slot2.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (shulker == null) {
                        //CommandManager.sendMessage("shulker == null");
                        double distance = 100;
                        BlockPos bestPos = null;
                        for (BlockPos pos : BlockUtil.getSphere(3, mc.player.getEyePos())) {
                            if (!mc.world.isAir(pos.up())) continue;
                            if (!BlockUtil.clientCanPlace(pos, false)
                                    || !BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)
                                    || !BlockUtil.canClick(pos.offset(Direction.DOWN))
                            ) continue;
                            if (bestPos == null || MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < distance) {
                                distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));
                                bestPos = pos;
                            }
                        }
                        if (bestPos != null) {
                            //CommandManager.sendMessage("bestPos != null");
                            BaritoneUtil.cancelEverything();
                            if (shulkerSlot != -1) {
                                //CommandManager.sendMessage("shulkerSlot != -1");
                                InventoryUtil.inventorySwap(shulkerSlot, mc.player.getInventory().selectedSlot);
                                BlockUtil.clickBlock(bestPos.offset(Direction.DOWN), Direction.UP, rotate.getValue());
                                InventoryUtil.inventorySwap(shulkerSlot, mc.player.getInventory().selectedSlot);
                            } else {
                                BlockPos ec = BlockUtil.getBlock(Blocks.ENDER_CHEST, 3f);
                                if (ec != null) {
                                    //CommandManager.sendMessage("enderchest BlockPos != null");
                                    BlockUtil.clickBlock(ec, BlockUtil.getClickSide(ec), rotate.getValue());
                                } else {
                                    int enderChest = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                                    if (enderChest != -1) {
                                        //CommandManager.sendMessage("enderChest != -1");
                                        InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                                        BlockUtil.placeBlock(bestPos, true);
                                        InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                                    }
                                }
                            }
                        }
                    } else {
                        //CommandManager.sendMessage("shulker != null, try click shulker");
                        BlockUtil.clickBlock(shulker, BlockUtil.getClickSide(shulker), rotate.getValue());
                    }
                } else if (storageSand) {
                    //CommandManager.sendMessage("Fill Sand");
                    if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerBoxScreenHandler) {
                        for (Slot slot : shulkerBoxScreenHandler.slots) {
                            if (slot.id < 27 && slot.getStack().isEmpty()) {
                                for (Slot slot2 : shulkerBoxScreenHandler.slots) {
                                    if (slot2.id >= 27 && slot2.getStack().getItem() == Items.SAND && slot2.getStack().getCount() == slot2.getStack().getMaxCount()) {
                                        mc.interactionManager.clickSlot(shulkerBoxScreenHandler.syncId, slot2.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                        return;
                                    }
                                }
                            }
                        }
                        mc.currentScreen.close();
                        storageSand = false;
                    }
                } else {
                    BlockPos shulker = BlockUtil.getBlock(ShulkerBoxBlock.class, 3f);
                    if (shulker != null) {
                        //CommandManager.sendMessage("Recovery Shulker");
                        BaritoneUtil.mine(mc.world.getBlockState(shulker).getBlock());
                    } else {
                        int fillShulker = InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
                        if (fillShulker != -1) {
                            BaritoneUtil.cancelEverything();
                            if (mc.player.currentScreenHandler instanceof ScreenHandler screenHandler) {
                                for (Slot slot : screenHandler.slots) {
                                    if (slot.id >= 27 && slot.getStack().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                                        mc.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                        return;
                                    }
                                }
                            }
                            double distance = 100;
                            BlockPos bestPos = null;
                            for (BlockPos pos : BlockUtil.getSphere(3)) {
                                if (!mc.world.isAir(pos.up())) continue;
                                if (!BlockUtil.clientCanPlace(pos, false)
                                        || !BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)
                                        || !BlockUtil.canClick(pos.offset(Direction.DOWN))
                                ) continue;
                                if (bestPos == null || MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < distance) {
                                    distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));
                                    bestPos = pos;
                                }
                            }
                            if (bestPos != null) {
                                BlockPos ec = BlockUtil.getBlock(Blocks.ENDER_CHEST, 3f);
                                if (ec != null) {
                                    BlockUtil.clickBlock(ec, BlockUtil.getClickSide(ec), rotate.getValue());
                                } else {
                                    int enderChest = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                                    if (enderChest != -1) {
                                        InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                                        BlockUtil.placeBlock(bestPos, true);
                                        InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                                    }
                                }
                            }
                            return;
                        }
                        if (hasShulkerItemEntity != null) {
                            BaritoneUtil.gotoPos(hasShulkerItemEntity.getBlockPos());
                        }
                        BlockPos pos = BlockUtil.getBlock(Blocks.ENDER_CHEST, 5);
                        if (pos != null) {
                            BaritoneUtil.mine(Blocks.ENDER_CHEST);
                            if (mc.currentScreen != null) {
                                mc.currentScreen.close();
                            }
                            return;
                        }
                        if (ai.getValue()) {
                            BaritoneUtil.mine(sandBlock);
                        }
                        if (nuker.getValue()) {
                            if (!mc.player.isOnGround()) return;
                            int b = 0;
                            for (BlockPos sand : BlockUtil.getSphere(3f, mc.player.getEyePos())) {
                                if (sandBlock != mc.world.getBlockState(sand).getBlock()) continue;
                                Direction side = BlockUtil.getClickSideStrict(sand);
                                if (side == null) continue;
                                PlaceRender.INSTANCE.create(sand);
                                Alien.ROTATION.snapAt(sand.toCenterPos());
                                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, sand, side, id));
                                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, sand, side, id));
                                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, sand, side, id));
                                Alien.ROTATION.snapBack();
                                b++;
                                if (b >= breaks.getValue()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            case Ominous -> {
                int ominousSlot = InventoryUtil.findItem(Items.OMINOUS_BOTTLE);
                if (ominousSlot != -1) {
                    if (!mc.player.hasStatusEffect(StatusEffects.BAD_OMEN) && !mc.player.hasStatusEffect(StatusEffects.RAID_OMEN)) {
                        if (ominousTimer.passedS(maxTime.getValue())) {
                            if (mc.player.getMainHandStack().getItem() != Items.OMINOUS_BOTTLE) {
                                lastSlot = mc.player.getInventory().selectedSlot;
                                InventoryUtil.switchToSlot(ominousSlot);
                            } else {
                                mc.options.useKey.setPressed(true);
                                ominousTimer.reset();
                            }
                        }
                    } else if (mc.player.getMainHandStack().getItem() == Items.OMINOUS_BOTTLE) {
                        mc.options.useKey.setPressed(false);
                        if (lastSlot != -1) {
                            InventoryUtil.switchToSlot(lastSlot);
                            lastSlot = -1;
                        }
                    }
                }
            }

            case TurtlePath -> {
                int seagrass = InventoryUtil.findItem(Items.SEAGRASS);
                if (seagrass == -1) {
                    if (BaritoneUtil.isActive()) {
                        BaritoneUtil.cancelEverything();
                    }
                    return;
                }
                if (timeOut.passedS(300)) {
                    inLove.clear();
                    timeOut.reset();
                }
                double distance = 0;
                TurtleEntity target = null;
                for (Entity entity : Alien.THREAD.getEntities()) {
                    if (entity instanceof TurtleEntity turtle) {
                        if (turtle.isBaby()) continue;
                        if (inLove.contains(turtle)) continue;
                        if (Math.abs(mc.player.getY() - turtle.getY()) > 3) continue;
                        double dis = mc.player.distanceTo(turtle);
                        if (target == null || dis < distance) {
                            distance = dis;
                            target = turtle;
                        }
                    }
                }
                if (target == null) {
                    if (timeOut.passedS(20)) {
                        inLove.clear();
                        timeOut.reset();
                    }
                    if (BaritoneUtil.isActive()) {
                        BaritoneUtil.cancelEverything();
                    }
                    return;
                }
                if (mc.player.distanceTo(target) < 3) {
                    BaritoneUtil.cancelEverything();
                    InventoryUtil.switchToSlot(seagrass);
                    Alien.ROTATION.snapAt(target.getEntityPos());
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(target, mc.player.isSneaking(), Hand.MAIN_HAND));
                    EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.interactSwing.getValue());
                    //InventoryUtil.switchToSlot(selectedSlot);
                    Alien.ROTATION.snapBack();
                    inLove.add(target);
                } else {
                    BaritoneUtil.gotoPos(target.getBlockPos());
                }
            }
            case Turtle -> {
                int seagrass = InventoryUtil.findItem(Items.SEAGRASS);
                if (seagrass == -1) {
                    return;
                }
                if (timeOut.passedS(300)) {
                    inLove.clear();
                    timeOut.reset();
                }
                double distance = 0;
                TurtleEntity target = null;
                for (Entity entity : Alien.THREAD.getEntities()) {
                    if (entity instanceof TurtleEntity turtle) {
                        if (turtle.isBaby()) continue;
                        if (inLove.contains(turtle)) continue;
                        if (mc.player.distanceTo(turtle) > 3) continue;
                        double dis = mc.player.distanceTo(turtle);
                        if (target == null || dis < distance) {
                            distance = dis;
                            target = turtle;
                        }
                    }
                }
                if (target == null) {
                    if (timeOut.passedS(20)) {
                        inLove.clear();
                        timeOut.reset();
                    }
                    return;
                }
                InventoryUtil.switchToSlot(seagrass);
                Alien.ROTATION.snapAt(target.getEntityPos());
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(target, mc.player.isSneaking(), Hand.MAIN_HAND));
                EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.interactSwing.getValue());
                //InventoryUtil.switchToSlot(selectedSlot);
                Alien.ROTATION.snapBack();
                inLove.add(target);
            }
            case NPlusOneDupe -> {
                BlockPos placePos = PacketMine.getBreakPos();
                if (placePos != null && BlockUtil.canPlace(placePos)) {
                    placeBlock(placePos);
                }
            }
            case XinDupe -> {
                if (llama != null) {
                    if (llama.isDead() || llama.distanceTo(mc.player) > 20) {
                        llama = null;
                    }
                }
                int chestSlot = InventoryUtil.findBlockInventorySlot(Blocks.CHEST);
                int swordSlot = findSwordSlot();
                if (chestSlot == -1) {
                    emptyBox.clear();
                    stage = Stage.Open;
                    boxPos = null;
                    closeToBox = false;
                    llama = null;
                    putIn = false;
                    return;
                }

                int shulkers = 0;
                for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                    if (entry.getValue().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                        shulkers++;
                    }
                }
                if (shulkers > 18) {
                    if (mc.currentScreen != null) mc.currentScreen.close();
                    //mc.setScreen(null);
                    for (int slot1 = 9; slot1 < 36; ++slot1) {
                        ItemStack stack = mc.player.getInventory().getStack(slot1);
                        if (stack.isEmpty()) continue;
                        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                            shulkers--;
                            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 1, SlotActionType.THROW, mc.player);
                            if (shulkers <= 18) return;
                        }
                    }
                    return;
                }

                if (closeToBox && boxPos != null) {
                    closeTo(boxPos);
                }
                if (!openTimeOut.passed(100)) return;
                switch (stage) {
                    case Open -> {
                        if (!closeScreen.passed(250)) {
                            if (mc.currentScreen != null) mc.currentScreen.close();
                            return;
                        }
                        if (mc.player.hasVehicle()) {
                            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
                                    new PlayerInput(false, false, false, false, false, false, true)
                            ));
                            return;
                        }
                        if (boxPos == null || emptyBox.contains(boxPos)) {
                            for (BlockPos pos : BlockUtil.getSphere(3)) {
                                if (!emptyBox.contains(pos) && mc.world.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity && BlockUtil.getClickSideStrict(pos) != null) {
                                    closeToBox = false;
                                    boxPos = pos;
                                    break;
                                }
                            }
                        }
                        if (boxPos != null && !emptyBox.contains(boxPos)) {
                            if (mc.player.getEyePos().distanceTo(boxPos.toCenterPos()) < 4) {
                                if (openTimeOut.passedS(1)) {
                                    closeToBox = false;
                                    openTimeOut.reset();
                                    BlockUtil.clickBlock(boxPos, BlockUtil.getClickSide(boxPos), true);
                                    stage = Stage.Take;
                                }
                            } else {
                                closeToBox = true;
                            }
                        }
                    }
                    case Take -> {
                        if (mc.player.hasVehicle()) {
                            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
                                    new PlayerInput(false, false, false, false, false, false, true)
                            ));
                            return;
                        }
                        if (boxPos == null || emptyBox.contains(boxPos)) {
                            closeScreen.reset();
                            stage = Stage.Open;
                            return;
                        }
                        if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
                            boolean egg = false;
                            boolean hay = false;
                            for (Slot slot : shulker.slots) {
                                if (slot.id < 27 && !slot.getStack().isEmpty()) {
                                    if (slot.getStack().getItem() == Items.EGG) {
                                        egg = true;
                                    }
                                    if (slot.getStack().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                        hay = true;
                                    }
                                }
                            }
                            if (egg && hay) {
                                int eggs = 0;
                                int hays = 0;
                                for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                                    if (entry.getValue().getItem() == Items.EGG) {
                                        eggs++;
                                    }
                                    if (entry.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                        hays++;
                                    }
                                }
                                for (Slot slot : shulker.slots) {
                                    if (!slot.getStack().isEmpty()) {
                                        if (slot.id < 27) {
                                            if (slot.getStack().getItem() == Items.EGG && eggs < 2) {
                                                eggs++;
                                                mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                            }
                                            if (slot.getStack().getItem() == Blocks.HAY_BLOCK.asItem() && hays < 2) {
                                                hays++;
                                                mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                            }
                                        } else {
                                            if (slot.getStack().getItem() == Items.LEATHER) {
                                                mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                            }
                                        }
                                    }
                                }
                                if (hays < 1 || eggs < 1) {
                                    emptyBox.add(boxPos);
                                }
                                if (mc.currentScreen != null) mc.currentScreen.close();
                                //mc.setScreen(null);
                                stage = Stage.Summon;
                            } else {
                                closeScreen.reset();
                                emptyBox.add(boxPos);
                                stage = Stage.Open;
                            }
                        } else if (openTimeOut.passedS(1)) {
                            closeScreen.reset();
                            stage = Stage.Open;
                        }
                    }
                    case Summon -> {
                        int eggs = 0;
                        int hays = 0;
                        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                            if (entry.getValue().getItem() == Items.EGG) {
                                eggs++;
                            }
                            if (entry.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                hays++;
                            }
                        }
                        if (eggs <= 1 || hays <= 1) {
                            closeScreen.reset();
                            stage = Stage.Open;
                            return;
                        }
                        for (Entity entity : Alien.THREAD.getEntities()) {
                            if (entity instanceof LlamaEntity llamaEntity && mc.player.getEyePos().distanceTo(entity.getEntityPos()) < 10 && entity.isAlive()) {
                                if (mc.player.getEyePos().distanceTo(entity.getEntityPos()) < 5) {
                                    llama = llamaEntity;
                                    stage = Stage.Tame;
                                } else {
                                    closeTo(entity.getBlockPos());
                                }
                                return;
                            }
                        }
                        if (mc.currentScreen != null) mc.currentScreen.close();
                        //mc.setScreen(null);

                        int slot = InventoryUtil.findItemInventorySlot(Items.EGG);
                        InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
                        Alien.ROTATION.snapAt(Alien.ROTATION.getLastYaw(), 89);
                        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                        InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
                    }
                    case Tame -> {
                        if (llama == null || llama.isDead()) {
                            stage = Stage.Summon;
                            return;
                        }
                        int eggs = 0;
                        int hays = 0;
                        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                            if (entry.getValue().getItem() == Items.EGG) {
                                eggs++;
                            }
                            if (entry.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                hays++;
                            }
                        }
                        if (eggs <= 1 || hays <= 1) {
                            closeScreen.reset();
                            stage = Stage.Open;
                            return;
                        }

                        if (mc.player.hasVehicle()) {
                            if (llama.isTame()) {
                                if (llama.hasChest()) {
                                    int moves = 0;
                                    if (mc.player.currentScreenHandler instanceof HorseScreenHandler shulker) {
                                        if (putTimer.passed(250)) {
                                            if (!putIn) {
                                                for (Slot slot : shulker.slots) {
                                                    if (slot.getStack().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                                                        mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                                        moves++;
                                                        putTimer.reset();
                                                        if (moves >= 15) break;
                                                    }
                                                }
                                                putIn = true;
                                            } else {
                                                stage = Stage.Kill;
                                                mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
                                                        new PlayerInput(false, false, false, false, false, false, true)
                                                ));
                                            }
                                        }
                                    } else {
                                        putIn = false;
                                        putTimer.reset();
                                        mc.player.openRidingInventory();
                                    }
                                } else {
                                    mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
                                            new PlayerInput(false, false, false, false, false, false, true)
                                    ));
                                }
                            }
                        } else {
                            if (mc.player.getEyePos().distanceTo(llama.getEntityPos()) > 5) {
                                closeTo(llama.getBlockPos());
                                return;
                            }
                            if (llama.isBaby()) {
                                if (mc.currentScreen != null) mc.currentScreen.close();
                                //mc.setScreen(null);

                                int slot = InventoryUtil.findBlockInventorySlot(Blocks.HAY_BLOCK);
                                InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
                                Alien.ROTATION.lookAt(llama.getEntityPos());
                                mc.interactionManager.interactEntity(mc.player, llama, Hand.MAIN_HAND);
                                InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
                            } else {
                                if (llama.isTame()) {
                                    if (llama.hasChest()) {
                                        for (int i = 0; i < 9; ++i) {
                                            if (mc.player.getInventory().getStack(i).isEmpty()) {
                                                InventoryUtil.switchToSlot(i);
                                                mc.interactionManager.interactEntity(mc.player, llama, Hand.MAIN_HAND);
                                                return;
                                            }
                                        }
                                        for (int i = 0; i < 9; ++i) {
                                            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock || mc.player.getInventory().getStack(i).getItem() == Items.LEATHER) {
                                                InventoryUtil.switchToSlot(i);
                                                mc.player.dropSelectedItem(true);
                                                mc.interactionManager.interactEntity(mc.player, llama, Hand.MAIN_HAND);
                                                return;
                                            }
                                        }
                                    } else {
                                        putTimer.reset();
                                        putIn = false;
                                        if (mc.currentScreen != null) mc.currentScreen.close();
                                        //mc.setScreen(null);

                                        InventoryUtil.inventorySwap(chestSlot, mc.player.getInventory().selectedSlot);
                                        Alien.ROTATION.lookAt(llama.getEntityPos());
                                        mc.interactionManager.interactEntity(mc.player, llama, Hand.MAIN_HAND);
                                        InventoryUtil.inventorySwap(chestSlot, mc.player.getInventory().selectedSlot);
                                    }
                                } else {
                                    if (mc.currentScreen != null) mc.currentScreen.close();
                                    //mc.setScreen(null);

                                    for (int i = 0; i < 9; ++i) {
                                        if (mc.player.getInventory().getStack(i).isEmpty()) {
                                            InventoryUtil.switchToSlot(i);
                                            mc.interactionManager.interactEntity(mc.player, llama, Hand.MAIN_HAND);
                                            return;
                                        }
                                    }
                                    for (int i = 0; i < 9; ++i) {
                                        if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem blockItem) {
                                            if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
                                                InventoryUtil.switchToSlot(i);
                                                mc.player.dropSelectedItem(true);
                                                mc.interactionManager.interactEntity(mc.player, llama, Hand.MAIN_HAND);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case Kill -> {
                        if (llama == null || llama.isDead()) {
                            llama = null;
                            stage = Stage.Summon;
                            return;
                        }
                        if (mc.currentScreen != null) mc.currentScreen.close();
                        if (mc.player.hasVehicle()) {
                            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
                                    new PlayerInput(false, false, false, false, false, false, true)
                            ));
                            return;
                        }
                        if (mc.player.getEntityPos().distanceTo(llama.getEntityPos()) > 1) {
                            closeTo(llama.getBlockPos());
                        }
                        if (mc.player.getEntityPos().distanceTo(llama.getEntityPos()) > 2) {
                            return;
                        }
                        InventoryUtil.switchToSlot(swordSlot);
                        if (check()) {
                            Alien.ROTATION.lookAt(llama.getEyePos());
                            mc.interactionManager.attackEntity(mc.player, llama);
                            EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
                        }

                    }
                }
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @EventListener
    private void PacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) {
            return;
        }
        if (mode.is(Mode.ScoreFarmer) && event.getPacket() instanceof GameMessageS2CPacket packet) {
            if (packet.content() != null) {
                String received = packet.content().getString().replaceAll("§[a-zA-Z0-9]", "");
                if (!getScore.getValue() && received.contains("你收到一个决斗申请")) {
                    mc.player.networkHandler.sendChatCommand("duel accept " + name.getValue());
                } else if (received.contains("<")) {
                } else if (getScore.getValue() && received.contains("Starting in 3 seconds")) {
                    mc.player.networkHandler.sendChatCommand("suicide");
                } else if (!getScore.getValue() && received.contains("Starting in 4 seconds")) {
                    mc.player.networkHandler.sendChatCommand("suicide");
                }
            }
        }
    }

    @EventListener
    public void onRotate(UpdateRotateEvent event) {
        if (mode.is(Mode.XinDupe)) {
            event.setPitch(88);
        }
    }

    private boolean check() {
        int at = ((ILivingEntity) mc.player).getLastAttackedTicks();
        return Math.max(at / Aura.getAttackCooldownProgressPerTick(), 0.0F) >= 1.3;
    }

    private void closeTo(BlockPos pos) {
        double speed = 0.2873 / 1.5;
        float forward = 1f;
        float side = 0;
        float yaw = RotationManager.getRotation(pos.toCenterPos())[0];
        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = forward * speed * cos + side * speed * sin;
        final double posZ = forward * speed * sin - side * speed * cos;
        MovementUtil.setMotionX(posX);
        MovementUtil.setMotionZ(posZ);
    }

    public static int findSwordSlot() {
        if (mc.player == null) return -1;
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                return i;
            }
        }
        for (int i = 9; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                return i;
            }
        }
        return -1;
    }


    public enum Mode {
        AutoTrade,
        ItemFrameDupe,
        XinDupe,
        NPlusOneDupe,
        TridentDupe,
        TurtlePath,
        Turtle,
        SandMiner,
        ScoreFarmer,
        SlabPlacer,
        Ominous,
        None,
    }

    public enum Stage {
        Open,
        Take,
        Summon,
        Tame,
        Kill
    }
}
package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class Blocker extends Module {
    public static Blocker INSTANCE;
    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));
    final List<BlockPos> placePos = new ArrayList<>();
    final List<BlockPos> blockerPos = new ArrayList<>();
    final List<BlockPos> list = new ArrayList<>();
    private final Timer timer = new Timer();
    private final SliderSetting delay =
            add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> page.getValue() == Page.General));
    private final SliderSetting blocksPer =
            add(new SliderSetting("BlocksPer", 1, 1, 8, () -> page.getValue() == Page.General));
    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.General));
    private final BooleanSetting breakCrystal =
            add(new BooleanSetting("Break", true, () -> page.getValue() == Page.General));
    private final BooleanSetting inventorySwap =
            add(new BooleanSetting("InventorySwap", true, () -> page.getValue() == Page.General));
    private final BooleanSetting bevelCev =
            add(new BooleanSetting("BevelCev", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting burrow =
            add(new BooleanSetting("Burrow", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting face =
            add(new BooleanSetting("Face", true, () -> page.getValue() == Page.Target).setParent());
    private final BooleanSetting faceUp =
            add(new BooleanSetting("FaceUp", false, () -> page.getValue() == Page.Target && face.isOpen()));
    private final BooleanSetting feet =
            add(new BooleanSetting("Feet", true, () -> page.getValue() == Page.Target).setParent());
    private final BooleanSetting extend =
            add(new BooleanSetting("Extend", false, () -> page.getValue() == Page.Target && feet.isOpen()));
    private final BooleanSetting onlySurround =
            add(new BooleanSetting("OnlySurround", true, () -> page.getValue() == Page.Target && feet.isOpen()));
    private final BooleanSetting inAirPause =
            add(new BooleanSetting("InAirPause", false, () -> page.getValue() == Page.Check));
    private final BooleanSetting detectMining =
            add(new BooleanSetting("DetectMining", true, () -> page.getValue() == Page.Check));
    private final BooleanSetting eatingPause = add(new BooleanSetting("EatingPause", true, () -> page.getValue() == Page.Check));
    private int placeProgress = 0;
    private BlockPos playerBP;

    public Blocker() {
        super("Blocker", Category.Combat);
        setChinese("水晶阻挡");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        list.clear();
        if (inventorySwap.getValue() && !EntityUtil.inInventory()) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (!timer.passedMs(delay.getValue())) return;
        if (eatingPause.getValue() && mc.player.isUsingItem()) return;
        placeProgress = 0;

        if (playerBP != null && !playerBP.equals(EntityUtil.getPlayerPos(true))) {
            placePos.clear();
            blockerPos.clear();
        }
        playerBP = EntityUtil.getPlayerPos(true);
        double[] offset = new double[]{AntiCheat.getOffset(), -AntiCheat.getOffset(), 0};
        if (bevelCev.getValue()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN) continue;
                if (isBedrock(playerBP.offset(i).up())) continue;

                BlockPos blockerPos = playerBP.offset(i).up(2);
                if (crystalHere(blockerPos) && !placePos.contains(blockerPos)) {
                    placePos.add(blockerPos);
                }
            }
        }
        if (face.getValue() && (!onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
            for (double x : offset) {
                for (double z : offset) {
                    for (Direction i : Direction.values()) {
                        for (int d = 0; d < 3; ++d) {
                            BlockPos aroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z).offset(i, 1).up();
                            BlockPos blockerPos = new BlockPosX(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z).offset(i, d).up();
                            if (crystalHere(blockerPos) && !placePos.contains(blockerPos) && !Alien.HOLE.isHard(aroundPos)) {
                                placePos.add(blockerPos);
                            }
                        }
                    }
                }
            }
            if (faceUp.getValue()) {
                for (Direction i : Direction.values()) {
                    if (i == Direction.DOWN) continue;
                    if (isBedrock(playerBP.offset(i).up())) continue;

                    BlockPos blockerPos = playerBP.offset(i).up(2);
                    if (crystalHere(blockerPos) && !placePos.contains(blockerPos)) {
                        placePos.add(blockerPos);
                    }
                }
            }
        }
        if (getObsidian() == -1) {
            return;
        }

        placePos.removeIf((pos) -> !BlockUtil.clientCanPlace(pos, true));
        if (burrow.getValue()) {
            for (double x : offset) {
                for (double z : offset) {
                    BlockPos surroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                    if (isBedrock(surroundPos)) continue;
                    if (Alien.BREAK.isMining(surroundPos)) {
                        for (Direction direction : Direction.values()) {
                            if (direction == Direction.DOWN || direction == Direction.UP) continue;
                            BlockPos defensePos = surroundPos.offset(direction);
                            if (detectMining.getValue() && Alien.BREAK.isMining(defensePos)) {
                                continue;
                            }
                            if (breakCrystal.getValue()) {
                                CombatUtil.attackCrystal(defensePos, rotate.getValue(), false);
                            }
                            if (BlockUtil.canPlace(defensePos, 6, breakCrystal.getValue())) {
                                blockerPos.add(defensePos);
                            }
                        }
                    }
                }
            }
        }
        if (feet.getValue() && (!onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
            for (double x : offset) {
                for (double z : offset) {
                    for (Direction i : Direction.values()) {
                        BlockPos surroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z).offset(i);
                        if (isBedrock(surroundPos)) continue;
                        if (Alien.BREAK.isMining(surroundPos)) {
                            for (Direction direction : Direction.values()) {
                                //if (direction == Direction.DOWN || direction == Direction.UP) continue;
                                BlockPos defensePos = surroundPos.offset(direction);
                                if (detectMining.getValue() && Alien.BREAK.isMining(defensePos)) {
                                    continue;
                                }
                                if (breakCrystal.getValue()) {
                                    CombatUtil.attackCrystal(defensePos, rotate.getValue(), false);
                                }
                                if (BlockUtil.canPlace(defensePos, 6, breakCrystal.getValue())) {
                                    blockerPos.add(defensePos);
                                } else if (BlockUtil.canReplace(defensePos) && !BlockUtil.hasEntity(defensePos, true) && getHelper(defensePos) != null) {
                                    blockerPos.add(getHelper(defensePos));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (feet.getValue() && extend.getValue() && (!onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
            for (double x : offset) {
                for (double z : offset) {
                    for (Direction i : Direction.values()) {
                        if (i == Direction.UP || i == Direction.DOWN) continue;
                        BlockPos surroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z).offset(i);
                        if (isBedrock(surroundPos)) continue;
                        for (Direction direction : Direction.values()) {
                            if (direction == Direction.UP || direction == Direction.DOWN) continue;
                            BlockPos blockPos = surroundPos.offset(direction);
                            if (AutoCrystal.INSTANCE.canPlaceCrystal(blockPos, true, true)) {
                                if (detectMining.getValue() && Alien.BREAK.isMining(blockPos)) {
                                    continue;
                                }
                                if (breakCrystal.getValue()) {
                                    CombatUtil.attackCrystal(blockPos, rotate.getValue(), false);
                                }
                                if (BlockUtil.canPlace(blockPos, 6, breakCrystal.getValue())) {
                                    blockerPos.add(blockPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        blockerPos.removeIf((pos) -> !BlockUtil.clientCanPlace(pos, true));
        if (inAirPause.getValue() && !mc.player.isOnGround()) return;

        if (blockerPos.isEmpty()) return;
        int oldSlot = mc.player.getInventory().selectedSlot;
        int block = getObsidian();
        if (block == -1) {
            return;
        }
        doSwap(block);
        for (BlockPos defensePos : blockerPos) {
            if (BlockUtil.canPlace(defensePos, 6, breakCrystal.getValue())) {
                doPlace(defensePos);
            }
        }
        if (inventorySwap.getValue()) {
            doSwap(block);
            EntityUtil.syncInventory();
        } else {
            doSwap(oldSlot);
        }
    }

    public BlockPos getHelper(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i == Direction.UP) continue;
            if (detectMining.getValue() && Alien.BREAK.isMining(pos.offset(i))) continue;
            if (!BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()))
                continue;
            if (BlockUtil.canPlace(pos.offset(i), 6.0)) return pos.offset(i);
        }
        return null;
    }

    private boolean crystalHere(BlockPos pos) {
        return BlockUtil.getEndCrystals(new Box(pos)).stream().anyMatch(entity -> entity.getBlockPos().equals(pos));
    }

    private boolean isBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    private void doPlace(BlockPos pos) {
        if (list.contains(pos)) return;
        list.add(pos);
        if (!(placeProgress < blocksPer.getValue())) return;
        BlockUtil.placeBlock(pos, rotate.getValue());
        timer.reset();
        placeProgress++;
    }

    private void tryPlaceObsidian(BlockPos pos) {
        if (list.contains(pos)) return;
        list.add(pos);
        if (!(placeProgress < blocksPer.getValue())) return;
        if (detectMining.getValue() && Alien.BREAK.isMining(pos)) {
            return;
        }
        int oldSlot = mc.player.getInventory().selectedSlot;
        int block;
        if ((block = getObsidian()) == -1) {
            return;
        }
        doSwap(block);
        BlockUtil.placeBlock(pos, rotate.getValue());
        timer.reset();
        if (inventorySwap.getValue()) {
            doSwap(block);
            EntityUtil.syncInventory();
        } else {
            doSwap(oldSlot);
        }
        placeProgress++;
    }

    private void doSwap(int slot) {
        if (inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getObsidian() {
        if (inventorySwap.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        } else {
            return InventoryUtil.findBlock(Blocks.OBSIDIAN);
        }
    }

    public enum Page {
        General,
        Target,
        Check,
    }
}
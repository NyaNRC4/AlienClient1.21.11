package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static dev.luminous.api.utils.world.BlockUtil.getBlock;

public class Breaker extends Module {
    public static Breaker INSTANCE;
    public final SliderSetting targetRange =
            add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    public final SliderSetting range =
            add(new SliderSetting("Range", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final BooleanSetting burrow = add(new BooleanSetting("Burrow", true));
    private final BooleanSetting head = add(new BooleanSetting("Head", true));
    private final BooleanSetting face = add(new BooleanSetting("Face", true));
    private final BooleanSetting down = add(new BooleanSetting("Down", false));
    private final BooleanSetting surround = add(new BooleanSetting("Surround", true));

    private final BooleanSetting cevPause = add(new BooleanSetting("CevPause", true));
    private final BooleanSetting forceDouble = add(new BooleanSetting("ForceDouble", false));

    public Breaker() {
        super("Breaker", Category.Combat);
        setChinese("自动挖掘");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (CevBreaker.INSTANCE.isOn() && cevPause.getValue()) return;
        if (AntiCrawl.INSTANCE.work) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        PlayerEntity player = CombatUtil.getClosestEnemy(targetRange.getValue());
        if (player == null) return;
        doBreak(player);
    }

    private void doBreak(PlayerEntity player) {
        BlockPos pos = EntityUtil.getEntityPos(player, true);
        {
            if (PacketMine.getBreakPos() != null && !PacketMine.getBreakPos().equals(PacketMine.secondPos) && PacketMine.secondPos != null && !mc.world.isAir(PacketMine.secondPos) && forceDouble.getValue())
                return;
            double[] yOffset = new double[]{-0.8, 0.3, 1.1};
            double[] xzOffset = new double[]{0.3, -0.3};
            for (PlayerEntity entity : CombatUtil.getEnemies(targetRange.getValue())) {
                for (double y : yOffset) {
                    for (double x : xzOffset) {
                        for (double z : xzOffset) {
                            BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                            if (canBreak(offsetPos) && offsetPos.equals(PacketMine.getBreakPos())) {
                                return;
                            }
                        }
                    }
                }
            }
            List<Float> yList = new ArrayList<>();
            if (down.getValue()) {
                yList.add(-0.8f);
            }
            if (head.getValue()) {
                yList.add(2.3f);
            }
            if (burrow.getValue()) {
                yList.add(0.3f);
            }
            if (face.getValue()) {
                yList.add(1.1f);
            }
            for (double y : yList) {
                for (double offset : xzOffset) {
                    BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
                    if (canBreak(offsetPos)) {
                        PacketMine.INSTANCE.mine(offsetPos);
                        return;
                    }
                }
            }
            for (double y : yList) {
                for (double offset : xzOffset) {
                    for (double offset2 : xzOffset) {
                        BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
                        if (canBreak(offsetPos)) {
                            PacketMine.INSTANCE.mine(offsetPos);
                            return;
                        }
                    }
                }
            }
        }
        if (surround.getValue()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range.getValue()) {
                    continue;
                }
                if ((mc.world.isAir(pos.offset(i)) || pos.offset(i).equals(PacketMine.getBreakPos())) && canPlaceCrystal(pos.offset(i), false) && !pos.offset(i).equals(PacketMine.secondPos)) {
                    return;
                }
            }
            ArrayList<BlockPos> list = new ArrayList<>();
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range.getValue()) {
                    continue;
                }
                if (canBreak(pos.offset(i)) && canPlaceCrystal(pos.offset(i), true) && !isSurroundPos(pos.offset(i))) {
                    list.add(pos.offset(i));
                }
            }
            if (!list.isEmpty()) {
                PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble((E) -> E.getSquaredDistance(mc.player.getEyePos()))).get());
            } else {
                for (Direction i : Direction.values()) {
                    if (i == Direction.UP || i == Direction.DOWN) continue;
                    if (Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range.getValue()) {
                        continue;
                    }
                    if (canBreak(pos.offset(i)) && canPlaceCrystal(pos.offset(i), false)) {
                        list.add(pos.offset(i));
                    }
                }
                if (!list.isEmpty()) {
                    PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble((E) -> E.getSquaredDistance(mc.player.getEyePos()))).get());
                }
            }

        }
//wtf
//        if (PacketMine.getBreakPos() == null) {
//            if (burrow.getValue()) {
//                double[] yOffset;
//                double[] xzOffset = new double[]{0, 0.3, -0.3};
//
//                yOffset = new double[]{0.5, 1.1};
//                for (double y : yOffset) {
//                    for (double offset : xzOffset) {
//                        BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
//                        if (isObsidian(offsetPos)) {
//                            PacketMine.INSTANCE.mine(offsetPos);
//                            return;
//                        }
//                    }
//                }
//                for (double y : yOffset) {
//                    for (double offset : xzOffset) {
//                        for (double offset2 : xzOffset) {
//                            BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
//                            if (isObsidian(offsetPos)) {
//                                PacketMine.INSTANCE.mine(offsetPos);
//                                return;
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    private boolean isSurroundPos(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN) {
                continue;
            }
            BlockPos self = EntityUtil.getPlayerPos(true);
            if (self.offset(i).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean block) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN || !block)
                && BlockUtil.noEntityBlockCrystal(boost, true, true)
                && BlockUtil.noEntityBlockCrystal(boost.up(), true, true)
                ;
    }

    public static final List<Block> hard = Arrays.asList(
            Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
    );

    private boolean isObsidian(BlockPos pos) {
        return mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= PacketMine.INSTANCE.range.getValue() && hard.contains(getBlock(pos)) && BlockUtil.getClickSideStrict(pos) != null;
    }

    private boolean canBreak(BlockPos pos) {
        return this.isObsidian(pos) && (BlockUtil.getClickSideStrict(pos) != null || pos.equals((Object)PacketMine.getBreakPos())) && (!pos.equals((Object)PacketMine.secondPos) || !(Breaker.mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)) && !PacketMine.INSTANCE.autoSwitch.getValue() && !PacketMine.INSTANCE.noGhostHand.getValue());
    }
}
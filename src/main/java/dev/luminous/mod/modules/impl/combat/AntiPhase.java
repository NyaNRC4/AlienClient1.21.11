package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AntiPhase extends Module {
    public AntiPhase() {
        super("AntiPhase", Category.Combat);
        setChinese("反穿墙");
    }

    private final SliderSetting placeRange = add(new SliderSetting("PlaceRange", 4, 0, 8));
    private final BooleanSetting ladder = add(new BooleanSetting("Ladder", true).setParent());
    private final BooleanSetting onlyHard = add(new BooleanSetting("OnlyHard", true, ladder::isOpen));
    private final BooleanSetting itemFrame = add(new BooleanSetting("ItemFrame", true).setParent());
    private final BooleanSetting fill = add(new BooleanSetting("Fill", false, itemFrame::isOpen));
    private final BooleanSetting scaffolding = add(new BooleanSetting("Scaffolding", true));
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    private final BooleanSetting eatingPause = add(new BooleanSetting("EatingPause", true));
    private final BooleanSetting collideSkip = add(new BooleanSetting("CollideSkip", true));
    private final BooleanSetting crawlingSkip = add(new BooleanSetting("CrawlingSkip", true));
    private final BooleanSetting onlyGround = add(new BooleanSetting("InAirSkip", false));
    private final SliderSetting targetRange = add(new SliderSetting("TargetRange", 5, 0, 7, .1));
    private final SliderSetting delay = add(new SliderSetting("Delay", 100, 0, 2000, 1));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));

    private final Timer timer = new Timer();

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (eatingPause.getValue() && mc.player.isUsingItem()) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        for (Entity target : CombatUtil.getEnemies(targetRange.getValue())) {
            if (crawlingSkip.getValue() && target.isCrawling())
                continue;
            if (onlyGround.getValue() && !target.isOnGround())
                continue;
            if (collideSkip.getValue() && BlockUtil.canCollide(target, target.getBoundingBox()))
                continue;
            if (!timer.passed(delay.getValueInt())) continue;

            if (scaffolding.getValue()) {
                if (BlockUtil.canReplace(target.getBlockPos())) {
                    int block = getScaffolding();
                    if (block != -1) {
                        BlockPos bp = target.getBlockPos();
                        Direction downSide = null;
                        Direction placeSide = BlockUtil.getPlaceSide(bp, 6.0);
                        if (placeSide != null || (downSide = getSideIgnore(bp.down())) != null && BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock && !mc.player.isSneaking() || mc.player.isSneaking() && (downSide = getSideOnly(bp.down())) != null && BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock) {
                            Vec3d targetPos = placeSide != null ? bp.offset(placeSide).toCenterPos().add(placeSide.getOpposite().getVector().getX() * 0.5, placeSide.getOpposite().getVector().getY() * 0.5, placeSide.getOpposite().getVector().getZ() * 0.5) : bp.down().toCenterPos().add(downSide.getVector().getX() * 0.5, downSide.getVector().getY() * 0.5, downSide.getVector().getZ() * 0.5);
                            double distance = mc.player.getEyePos().distanceTo(targetPos);
                            if (distance <= placeRange.getValue()) {
                                int old = mc.player.getInventory().selectedSlot;
                                doSwap(block);
                                if (BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock && downSide != null) {
                                    BlockUtil.clickBlock(bp.down(), downSide, rotate.getValue());
                                } else {
                                    BlockUtil.placeBlock(bp, rotate.getValue());
                                }
                                timer.reset();
                                if (inventory.getValue()) {
                                    doSwap(block);
                                    EntityUtil.syncInventory();
                                } else {
                                    doSwap(old);
                                }
                            }
                        }
                    }
                }
            }

            if (itemFrame.getValue()) {
                if (mc.world.isAir(target.getBlockPos())) {
                    ItemFrameEntity itemFrameEntity = hasItemFrame(new Box(target.getBlockPos()));
                    if (itemFrameEntity == null) {
                        int block = getItemFrame();
                        if (block != -1) {
                            BlockPos bp = target.getBlockPos().down();
                            double distance = mc.player.getEyePos().distanceTo(bp.toBottomCenterPos().add(0, 1, 0));
                            if (distance <= placeRange.getValue()) {
                                if (BlockUtil.isStrictDirection(bp, Direction.UP) && !BlockUtil.canReplace(bp) && BlockUtil.canClick(bp)) {
                                    int old = mc.player.getInventory().selectedSlot;
                                    doSwap(block);
                                    BlockUtil.clickBlock(bp, Direction.UP, rotate.getValue());
                                    timer.reset();
                                    if (inventory.getValue()) {
                                        doSwap(block);
                                        EntityUtil.syncInventory();
                                    } else {
                                        doSwap(old);
                                    }
                                    //return;
                                }
                            }
                        }
                    }
                    if (fill.getValue() && itemFrameEntity != null && itemFrameEntity.getHeldItemStack().isEmpty()) {
                        Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), itemFrameEntity.getBoundingBox());
                        if (mc.player.getEyePos().distanceTo(hitVec) <= AntiCheat.INSTANCE.ieRange.getValue()) {
                            int block = getObsidian();
                            if (block != -1) {
                                int old = mc.player.getInventory().selectedSlot;
                                doSwap(block);
                                if (rotate.getValue()) {
                                    Alien.ROTATION.snapAt(hitVec);
                                }
                                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(itemFrameEntity, mc.player.isSneaking(), Hand.MAIN_HAND));
                                timer.reset();
                                if (inventory.getValue()) {
                                    doSwap(block);
                                    EntityUtil.syncInventory();
                                } else {
                                    doSwap(old);
                                }
                                if (rotate.getValue()) {
                                    Alien.ROTATION.snapBack();
                                }
                            }
                        }
                    }
                }
            }

            if (ladder.getValue()) {
                int block = getLadder();
                if (block != -1) {
                    if (BlockUtil.canReplace(target.getBlockPos())) {
                        Direction facing = targetFacing(target.getEntityPos());
                        if (facing != null) {
                            BlockPos bp = target.getBlockPos().offset(facing);
                            double distance = mc.player.getEyePos().distanceTo(bp.toCenterPos().add(facing.getOpposite().getVector().getX() * 0.5, facing.getOpposite().getVector().getY() * 0.5, facing.getOpposite().getVector().getZ() * 0.5));
                            if (distance <= placeRange.getValue()) {
                                BlockUtil.placedPos.add(target.getBlockPos());
                                int old = mc.player.getInventory().selectedSlot;
                                doSwap(block);
                                BlockUtil.clickBlock(bp, facing.getOpposite(), rotate.getValue());
                                timer.reset();
                                if (inventory.getValue()) {
                                    doSwap(block);
                                    EntityUtil.syncInventory();
                                } else {
                                    doSwap(old);
                                }
                                //return;
                            }
                        }
                    }
                }
            }
        }
    }

    private Direction getSideOnly(BlockPos pos) {
        if (BlockUtil.isStrictDirection(pos, Direction.UP))
            return Direction.UP;
        return null;
    }

    private Direction getSideIgnore(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i == Direction.UP) continue;
            if (!BlockUtil.isStrictDirection(pos, i)) continue;
            return i;
        }
        return null;
    }

    private ItemFrameEntity hasItemFrame(Box box) {
        for (Entity entity : BlockUtil.getEntities(box)) {
            if (entity instanceof ItemFrameEntity itemFrameEntity && entity.getFacing() == Direction.UP) {
                return itemFrameEntity;
            }
        }
        return null;
    }

    private static Box getBox(Direction facing, BlockPos bp) {
        Box box = null;
        double wide = 0.1875;
        double x = facing.getOffsetX() * 0.5 + bp.getX() + 0.5;
        double y = bp.getY();
        double z = facing.getOffsetZ() * 0.5 + bp.getZ() + 0.5;
        switch (facing) {
            case WEST -> box = new Box(x, y, z, x + wide, y + 1, z + 1);
            case EAST -> box = new Box(x, y, z, x - wide, y + 1, z + 1);
            case NORTH -> box = new Box(x, y, z, x + 1, y + 1, z + wide);
            case SOUTH -> box = new Box(x, y, z, x + 1, y + 1, z - wide);
        }
        return box;
    }

    private void doSwap(int slot) {
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getFlintAndSteel() {
        if (inventory.getValue()) {
            return InventoryUtil.findItemInventorySlot(Items.FLINT_AND_STEEL);
        } else {
            return InventoryUtil.findItem(Items.FLINT_AND_STEEL);
        }
    }

    private int getObsidian() {
        if (inventory.getValue()) {
            return InventoryUtil.findItemInventorySlot(Items.OBSIDIAN);
        } else {
            return InventoryUtil.findItem(Items.OBSIDIAN);
        }
    }

    private int getItemFrame() {
        if (inventory.getValue()) {
            return InventoryUtil.findItemInventorySlot(Items.ITEM_FRAME);
        } else {
            return InventoryUtil.findItem(Items.ITEM_FRAME);
        }
    }

    private int getLadder() {
        if (inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.LADDER);
        } else {
            return InventoryUtil.findBlock(Blocks.LADDER);
        }
    }

    private int getScaffolding() {
        if (inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.SCAFFOLDING);
        } else {
            return InventoryUtil.findBlock(Blocks.SCAFFOLDING);
        }
    }

    private Direction targetFacing(Vec3d vec3d) {
        BlockPos blockPos = new BlockPosX(vec3d);
        Vec3d centerPos = blockPos.toBottomCenterPos();

        float factorValue = 0.4f;

        double minDistance = Double.MAX_VALUE;
        Direction facing = null;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            BlockPos bp = blockPos.offset(direction);
            if (!BlockUtil.isStrictDirection(bp, direction.getOpposite()))
                continue;
            if (onlyHard.getValue()) {
                if (!Alien.HOLE.isHard(bp)) continue;
            } else {
                if (BlockUtil.canReplace(bp)) continue;
                if (!BlockUtil.canClick(bp)) continue;
            }
            Box box = getBox(direction, blockPos);
            if (box == null) continue;
            if (hasEntity(box)) continue;

            Vec3d tempPos = centerPos.add(direction.getOffsetX() * factorValue, 0, direction.getOffsetZ() * factorValue);

            double distance = tempPos.distanceTo(vec3d);
            if (distance < minDistance) {
                minDistance = distance;
                facing = direction;
            }
        }

        return facing;
    }

    public static boolean hasEntity(Box box) {
        for (Entity entity : BlockUtil.getEntities(box)) {
            if (!entity.isAlive() || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ExperienceBottleEntity || entity instanceof ArrowEntity || entity instanceof ItemFrameEntity)
                continue;
            return true;
        }
        return false;
    }
}

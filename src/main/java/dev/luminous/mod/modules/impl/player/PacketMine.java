package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.ClickBlockEvent;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.CevBreaker;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.*;
import net.minecraft.block.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.luminous.api.utils.world.BlockUtil.getBlock;

public class PacketMine extends Module {
    public static PacketMine INSTANCE;
    public static BlockPos secondPos;
    public static double progress = 0;
    private final FadeUtils animationTime = new FadeUtils(1000);
    private final FadeUtils secondAnim = new FadeUtils(1000);
    private final DecimalFormat df = new DecimalFormat("0.0");
    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    private final SliderSetting stopDelay = add(new SliderSetting("StopDelay", 50, 0, 500, 1, () -> page.is(Page.General)));
    private final SliderSetting startDelay = add(new SliderSetting("StartDelay", 200, 0, 500, 1, () -> page.is(Page.General)));
    private final SliderSetting damage = add(new SliderSetting("Damage", 0.7f, 0.0f, 2.0f, 0.01, () -> page.is(Page.General)));
    private final SliderSetting maxBreak = add(new SliderSetting("MaxBreak", 3, 0, 20, 1, () -> page.is(Page.General)));
    public final BooleanSetting noGhostHand = add(new BooleanSetting("1.21", false, () -> page.is(Page.General)));
    public final BooleanSetting noCollide = add(new BooleanSetting("NoCollide", true, () -> page.is(Page.General)));
    private final EnumSetting<Timing> timing = add(new EnumSetting<>("Timing", Timing.All, () -> page.getValue() == Page.General));
    private final BooleanSetting grimDisabler = add(new BooleanSetting("GrimDisabler", false, () -> page.is(Page.General)));
    private final BooleanSetting instant = add(new BooleanSetting("Instant", false, () -> page.is(Page.General)));
    private final BooleanSetting wait = add(new BooleanSetting("Wait", true, () -> !instant.getValue() && page.is(Page.General)));
    private final BooleanSetting mineAir = add(new BooleanSetting("MineAir", true, () -> wait.getValue() && !instant.getValue() && page.is(Page.General)));
    private final BooleanSetting hotBar = add(new BooleanSetting("HotbarSwap", false, () -> page.is(Page.General)));
    private final BooleanSetting doubleBreak = add(new BooleanSetting("DoubleBreak", true, () -> page.is(Page.General))).setParent();
    public final BooleanSetting autoSwitch = add(new BooleanSetting("AutoSwitch", true, () -> page.is(Page.General) && doubleBreak.isOpen()));
    private final SliderSetting start = add(new SliderSetting("Start", 0.9f, 0.0f, 2.0f, 0.01, () -> page.is(Page.General) && doubleBreak.isOpen()));
    private final SliderSetting timeOut = add(new SliderSetting("TimeOut", 1.2f, 0.0f, 2.0f, 0.01, () -> page.is(Page.General) && doubleBreak.isOpen()));
    private final BooleanSetting setAir = add(new BooleanSetting("SetAir", false, () -> page.is(Page.General)));
    private final BooleanSetting swing = add(new BooleanSetting("Swing", true, () -> page.is(Page.General)));
    private final BooleanSetting endSwing = add(new BooleanSetting("EndSwing", false, () -> page.is(Page.General)));
    public final SliderSetting range = add(new SliderSetting("Range", 6f, 3.0f, 10.0f, 0.1, () -> page.is(Page.General)));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("SwingSide", SwingSide.All, () -> page.is(Page.General)));

    private final BooleanSetting unbreakableCancel = add(new BooleanSetting("UnbreakableCancel", true, () -> page.is(Page.Check)));
    private final BooleanSetting switchReset = add(new BooleanSetting("SwitchReset", false, () -> page.is(Page.Check)));
    private final BooleanSetting preferWeb = add(new BooleanSetting("PreferWeb", true, () -> page.is(Page.Check)));
    private final BooleanSetting preferHead = add(new BooleanSetting("PreferHead", true, () -> page.is(Page.Check)));
    private final BooleanSetting farCancel = add(new BooleanSetting("FarCancel", false, () -> page.is(Page.Check)));
    private final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", true, () -> page.is(Page.Check)));
    private final BooleanSetting checkWeb = add(new BooleanSetting("CheckWeb", true, () -> page.is(Page.Check)));
    private final BooleanSetting checkGround = add(new BooleanSetting("CheckGround", true, () -> page.is(Page.Check)));
    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, () -> page.is(Page.Check) && checkGround.getValue()));
    private final BooleanSetting usingPause = add(new BooleanSetting("UsingPause", false, () -> page.is(Page.Check)).setParent());
    private final BooleanSetting allowOffhand = add(new BooleanSetting("AllowOffhand", true, () -> page.is(Page.Check) && usingPause.isOpen()));
    private final BooleanSetting bypassGround = add(new BooleanSetting("BypassGround", true, () -> page.is(Page.Check)));
    private final SliderSetting bypassTime = add(new SliderSetting("BypassTime", 400, 0, 2000, () -> bypassGround.getValue() && page.is(Page.Check)));
    private final BindSetting pause = add(new BindSetting("Pause", -1, () -> page.is(Page.Check)));

    private final BooleanSetting rotate = add(new BooleanSetting("StartRotate", true, () -> page.is(Page.Rotation)));
    private final BooleanSetting endRotate = add(new BooleanSetting("EndRotate", false, () -> page.is(Page.Rotation)));
    private final SliderSetting syncTime = add(new SliderSetting("Sync", 300, 0, 1000, () -> page.is(Page.Rotation)));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> page.is(Page.Rotation)).setParent());
    private final BooleanSetting whenElytra = add(new BooleanSetting("FallFlying", true, () -> page.is(Page.Rotation) && yawStep.isOpen()));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> page.is(Page.Rotation) && yawStep.isOpen()));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> page.is(Page.Rotation) && yawStep.isOpen()));
    private final SliderSetting fov = add(new SliderSetting("Fov", 20, 0, 360, 0.1, () -> page.is(Page.Rotation) && yawStep.isOpen()));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10, 0, 100, () -> page.is(Page.Rotation) && yawStep.isOpen()));

    public final BooleanSetting crystal = add(new BooleanSetting("Crystal", false, () -> page.is(Page.Place)).setParent());
    private final BooleanSetting onlyHeadBomber = add(new BooleanSetting("OnlyCev", true, () -> page.is(Page.Place) && crystal.isOpen()));
    private final BooleanSetting waitPlace = add(new BooleanSetting("WaitPlace", true, () -> page.is(Page.Place) && crystal.isOpen()));
    private final BooleanSetting spamPlace = add(new BooleanSetting("SpamPlace", false, () -> page.is(Page.Place) && crystal.isOpen()));
    private final BooleanSetting afterBreak = add(new BooleanSetting("AfterBreak", true, () -> page.is(Page.Place) && crystal.isOpen()));
    private final BooleanSetting checkDamage = add(new BooleanSetting("DetectProgress", true, () -> page.is(Page.Place) && crystal.isOpen()));
    private final SliderSetting crystalDamage = add(new SliderSetting("Progress", 0.9f, 0.0f, 1.0f, 0.01, () -> page.is(Page.Place) && crystal.isOpen() && checkDamage.getValue()));
    public final BindSetting obsidian = add(new BindSetting("Obsidian", -1, () -> page.is(Page.Place)));
    private final BindSetting enderChest = add(new BindSetting("EnderChest", -1, () -> page.is(Page.Place)));
    private final BooleanSetting placeRotate = add(new BooleanSetting("PlaceRotate", true, () -> page.is(Page.Place)));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true, () -> page.is(Page.Place)));
    private final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 100, 0, 1000, () -> page.is(Page.Place)));

    private final BooleanSetting checkDouble = add(new BooleanSetting("CheckDouble", false, () -> page.is(Page.Render)));
    private final EnumSetting<Animation> animation = add(new EnumSetting<>("Animation", Animation.Up, () -> page.is(Page.Render)));
    private final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> page.is(Page.Render)));
    private final EnumSetting<Easing> fadeEase = add(new EnumSetting<>("FadeEase", Easing.CubicInOut, () -> page.is(Page.Render)));
    private final SliderSetting expandLine = add(new SliderSetting("ExpandLine", 0.0, 0.0, 1, () -> page.is(Page.Render)));
    private final ColorSetting startColor = add(new ColorSetting("StartFill", new Color(255, 255, 255, 100), () -> page.is(Page.Render)));
    private final ColorSetting startOutlineColor = add(new ColorSetting("StartOutline", new Color(255, 255, 255, 100), () -> page.is(Page.Render)));
    private final ColorSetting endColor = add(new ColorSetting("EndFill", new Color(255, 255, 255, 100), () -> page.is(Page.Render)));
    private final ColorSetting endOutlineColor = add(new ColorSetting("EndOutline", new Color(255, 255, 255, 100), () -> page.is(Page.Render)));
    private final ColorSetting doubleColor = add(new ColorSetting("DoubleFill", new Color(88, 94, 255, 100), () -> doubleBreak.getValue() && page.is(Page.Render)));
    private final ColorSetting doubleOutlineColor = add(new ColorSetting("DoubleOutline", new Color(88, 94, 255, 100), () -> doubleBreak.getValue() && page.is(Page.Render)));
    private final BooleanSetting text = add(new BooleanSetting("Text", true, () -> page.is(Page.Render)));
    private final BooleanSetting box = add(new BooleanSetting("Box", true, () -> page.is(Page.Render)));
    private final BooleanSetting outline = add(new BooleanSetting("Outline", true, () -> page.is(Page.Render)));
    private final Timer mineTimer = new Timer();
    private final Timer sync = new Timer();
    private final Timer secondTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer startTime = new Timer();
    public static boolean ghost = false;
    public static boolean complete = false;
    int lastSlot = -1;
    Vec3d directionVec = null;
    Runnable switchBack;
    BlockPos breakPos;
    boolean startPacket = false;
    int breakNumber = 0;
    double breakFinalTime;
    double secondFinalTime;
    boolean sendGroundPacket = false;
    boolean swapped = false;
    int mainSlot = 0;

    public PacketMine() {
        super("PacketMine", Category.Player);
        setChinese("发包挖掘");
        INSTANCE = this;
    }

    public static BlockPos getBreakPos() {
        if (INSTANCE.isOn()) {
            return INSTANCE.breakPos;
        }
        return null;
    }

    @Override
    public String getInfo() {
        if (progress >= 1) {
            return "Done";
        }
        return df.format(progress * 100) + "%";
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (rotate.getValue() && shouldYawStep() && directionVec != null && !sync.passedMs(syncTime.getValue())) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @Override
    public void onLogin() {
        startPacket = false;
        ghost = false;
        complete = false;
        breakPos = null;
        secondPos = null;
    }

    @Override
    public void onDisable() {
        startPacket = false;
        ghost = false;
        complete = false;
        breakPos = null;
    }

    private void autoSwitch() {
        if (autoSwitch.getValue() && doubleBreak.getValue()) {
            int index = -1;
            if (secondPos != null) {
                float CurrentFastest = 1.0f;
                for (int i = 0; i < 9; ++i) {
                    float destroySpeed;
                    float digSpeed;
                    ItemStack stack = PacketMine.mc.player.getInventory().getStack(i);
                    if (stack == ItemStack.EMPTY || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().getOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOrThrow(Enchantments.EFFICIENCY)), (ItemStack)stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(secondPos))) > CurrentFastest)) continue;
                    CurrentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
            if (index == -1 || mc.options.useKey.isPressed() || mc.options.attackKey.isPressed() || mc.player.isUsingItem() || !secondTimer.passedMs(getBreakTime(secondPos, index, start.getValue()))) {
                if (swapped) {
                    InventoryUtil.switchToSlot(mainSlot);
                    swapped = false;
                }
            } else {
                if (index != mc.player.getInventory().selectedSlot) {
                    mainSlot = mc.player.getInventory().selectedSlot;
                    InventoryUtil.switchToSlot(index);
                    swapped = true;
                }
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (breakPos != null && mc.world.isAir(breakPos)) {
            complete = true;
        }
        if (secondPos != null) {
            int secondSlot = getTool(secondPos);
            if (secondSlot == -1) {
                secondSlot = mc.player.getInventory().selectedSlot;
            }
            secondFinalTime = getBreakTime(secondPos, secondSlot, 1);
            if (isAir(secondPos) || unbreakable(secondPos)) {
                secondPos = null;
            } else {
                double time = getBreakTime(secondPos, mc.player.getInventory().selectedSlot, 1);
                if (secondTimer.passedMs(time * timeOut.getValue())) {
                    secondPos = null;
                }
            }
        }
        if (switchBack != null && event.isPre()) {
            switchBack.run();
            switchBack = null;
        }
        if (timing.is(Timing.Pre) && event.isPost() || timing.is(Timing.Post) && event.isPre()) return;
        if (mc.player.isDead()) {
            secondPos = null;
        }
        autoSwitch();
        if (mc.player.isCreative()) {
            startPacket = false;
            ghost = false;
            complete = false;
            breakNumber = 0;
            breakPos = null;
            progress = 0;
            return;
        }
        if (breakPos == null) {
            breakNumber = 0;
            startPacket = false;
            ghost = false;
            complete = false;
            progress = 0;
            return;
        }
        int slot = getTool(breakPos);
        if (slot == -1) {
            slot = mc.player.getInventory().selectedSlot;
        }
        breakFinalTime = getBreakTime(breakPos, slot);
        progress = (double) mineTimer.getMs() / breakFinalTime;
        if (isAir(breakPos)) {
            breakNumber = 0;
        }
        if (breakNumber > maxBreak.getValue() - 1 && maxBreak.getValue() > 0 && !complete || !wait.getValue() && isAir(breakPos) && !instant.getValue()) {
            if (breakPos.equals(secondPos)) {
                secondPos = null;
            }
            startPacket = false;
            ghost = false;
            complete = false;
            breakNumber = 0;
            breakPos = null;
            return;
        }
        if (unbreakable(breakPos)) {
            if (unbreakableCancel.getValue()) {
                breakPos = null;
                startPacket = false;
                ghost = false;
                complete = false;
            }
            breakNumber = 0;
            return;
        }
        if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(breakPos.toCenterPos())) > range.getValue()) {
            if (farCancel.getValue()) {
                startPacket = false;
                ghost = false;
                complete = false;
                breakNumber = 0;
                breakPos = null;
            }
            return;
        }
        if (usingPause.getValue() && mc.player.isUsingItem() && (!allowOffhand.getValue() || mc.player.getActiveHand() == Hand.MAIN_HAND)) {
            return;
        }
        if (pause.isPressed()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (breakPos.equals(AutoAnchor.INSTANCE.currentPos) && getBlock(getBreakPos()) instanceof RespawnAnchorBlock)
            return;
        if (!hotBar.getValue() && !EntityUtil.inInventory()) {
            return;
        }

        if (isAir(breakPos)) {
            if (shouldCrystal()) {
                for (Direction facing : Direction.values()) {
                    CombatUtil.attackCrystal(breakPos.offset(facing), placeRotate.getValue(), true);
                }
            }
            if (placeTimer.passedMs(placeDelay.getValue())) {
                if (BlockUtil.canPlace(breakPos) && mc.currentScreen == null) {
                    if (enderChest.isPressed()) {
                        int eChest = findBlock(Blocks.ENDER_CHEST);
                        if (eChest != -1) {
                            int oldSlot = mc.player.getInventory().selectedSlot;
                            doSwap(eChest, eChest);
                            BlockUtil.placeBlock(breakPos, placeRotate.getValue(), true);
                            doSwap(oldSlot, eChest);
                            placeTimer.reset();
                        }
                    } else if (obsidian.isPressed()) {

                        int obsidian = findBlock(Blocks.OBSIDIAN);
                        if (obsidian != -1) {

                            boolean hasCrystal = false;
                            if (shouldCrystal()) {
                                for (Entity entity : BlockUtil.getEntities(new Box(breakPos.up()))) {
                                    if (entity instanceof EndCrystalEntity) {
                                        hasCrystal = true;
                                        break;
                                    }
                                }
                            }

                            if (!hasCrystal || spamPlace.getValue()) {
                                int oldSlot = mc.player.getInventory().selectedSlot;
                                doSwap(obsidian, obsidian);
                                BlockUtil.placeBlock(breakPos, placeRotate.getValue(), true);
                                doSwap(oldSlot, obsidian);
                                placeTimer.reset();
                            }
                        }
                    }
                }
            }
            breakNumber = 0;
        } else if (canPlaceCrystal(breakPos.up())) {
            if (shouldCrystal()) {
                if (placeTimer.passedMs(placeDelay.getValue())) {
                    if (checkDamage.getValue()) {
                        if (mineTimer.getMs() / breakFinalTime >= crystalDamage.getValue()) {
                            if (!placeCrystal()) return;
                        }
                    } else {
                        if (!placeCrystal()) return;
                    }
                } else if (startPacket) {
                    return;
                }
            }
        }
        if (waitPlace.getValue()) {
            for (Direction i : Direction.values()) {
                if (breakPos.offset(i).equals(AutoCrystal.INSTANCE.crystalPos)) {
                    if (AutoCrystal.INSTANCE.canPlaceCrystal(breakPos, false, false)) {
                        return;
                    }
                    break;
                }
            }
        }
        if (!delayTimer.passed((long) stopDelay.getValue())) return;
        if (startPacket) {
            if (isAir(breakPos)) {
                return;
            }
            if (onlyGround.getValue() && !mc.player.isOnGround()) return;
            if (mineTimer.passed((long) breakFinalTime)) {
                if (endRotate.getValue() && shouldYawStep()) {
                    if (!faceVector(breakPos.toCenterPos().offset(BlockUtil.getClickSide(breakPos), 0.5))) {
                        return;
                    }
                }
                int old = mc.player.getInventory().selectedSlot;
                boolean shouldSwitch;
                if (hotBar.getValue()) {
                    shouldSwitch = slot != old;
                } else {
                    if (slot < 9) {
                        slot = slot + 36;
                    }
                    shouldSwitch = old + 36 != slot;
                }
                if (shouldSwitch) {
                    if (hotBar.getValue()) {
                        InventoryUtil.switchToSlot(slot);
                    } else {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, old, SlotActionType.SWAP, mc.player);
                    }
                }

                int finalSlot = slot;
                switchBack = () -> {
                    if (endRotate.getValue()) {
                        if (!faceVector(breakPos.toCenterPos().offset(BlockUtil.getClickSide(breakPos), 0.5))) {
                            if (shouldSwitch) {
                                if (hotBar.getValue()) {
                                    InventoryUtil.switchToSlot(old);
                                } else {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, mc.player);
                                    EntityUtil.syncInventory();
                                }
                            }
                            return;
                        }
                    }
                    sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakPos, BlockUtil.getClickSide(breakPos), id));
                    if (endSwing.getValue()) EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
                    if (shouldSwitch) {
                        if (hotBar.getValue()) {
                            InventoryUtil.switchToSlot(old);
                        } else {
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, mc.player);
                            EntityUtil.syncInventory();
                        }
                    }
                    breakNumber++;
                    delayTimer.reset();
                    startTime.reset();
                    if (afterBreak.getValue() && shouldCrystal()) {
                        for (Direction facing : Direction.values()) {
                            CombatUtil.attackCrystal(breakPos.offset(facing), placeRotate.getValue(), true);
                        }
                    }
                    if (setAir.getValue()) {
                        mc.world.setBlockState(breakPos, Blocks.AIR.getDefaultState());
                    }
                    if (endRotate.getValue() && !shouldYawStep()) {
                        Alien.ROTATION.snapBack();
                    }
                    ghost = true;
                };
                if (!noGhostHand.getValue()) {
                    switchBack.run();
                    switchBack = null;
                }
            }
        } else {
            if (!startTime.passed(startDelay.getValueInt())) return;
            if (!mineAir.getValue() && isAir(breakPos)) {
                return;
            }
            Direction side = BlockUtil.getClickSide(breakPos);
            if (rotate.getValue()) {
                Vec3i vec3i = side.getVector();
                if (!faceVector(breakPos.toCenterPos().add(new Vec3d(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5)))) {
                    return;
                }
            }
            mineTimer.reset();
            animationTime.reset();
            if (swing.getValue()) {
                EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
            }
            if (doubleBreak.getValue()) {
                if (secondPos == null || isAir(secondPos)) {
                    double breakTime = (getBreakTime(breakPos, slot, 1));
                    secondAnim.reset();
                    secondAnim.setLength((long) breakTime);
                    secondTimer.reset();
                    secondPos = breakPos;
                }
                doDoubleBreak(side);
            }
            sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, breakPos, side, id));
            if (rotate.getValue() && !shouldYawStep()) {
                Alien.ROTATION.snapBack();
            }
            startTime.reset();
        }
    }

    private void breakBlock(BlockPos breakPos) {
        mc.world.getBlockState(breakPos).getBlock().onBreak(mc.world, breakPos, mc.world.getBlockState(breakPos), mc.player);
    }

    void doDoubleBreak(Direction side) {
        sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, breakPos, side, id));
        sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakPos, side, id));
    }

    boolean placeCrystal() {
        int crystal = findCrystal();
        if (crystal != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            doSwap(crystal, crystal);
            BlockUtil.placeCrystal(breakPos.up(), placeRotate.getValue());
            doSwap(oldSlot, crystal);
            placeTimer.reset();
            return !waitPlace.getValue();
        }
        return true;
    }

    @EventListener
    public void onAttackBlock(ClickBlockEvent event) {
        if (nullCheck()) {
            return;
        }
        if (mc.player.isCreative()) {
            return;
        }
        event.cancel();

        BlockPos pos = (event.getPos());
        if (pos.equals(breakPos)) {
            return;
        }
        if (unbreakable(pos)) {
            return;
        }
        if (breakPos != null && preferWeb.getValue() && getBlock(breakPos) == Blocks.COBWEB) {
            return;
        }
        if (breakPos != null && preferHead.getValue() && mc.player.isCrawling() && EntityUtil.getPlayerPos(true).up().equals(breakPos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > range.getValue()) {
            return;
        }
        breakPos = pos;
        breakNumber = 0;
        startPacket = false;
        ghost = false;
        complete = false;
        mineTimer.reset();
        animationTime.reset();
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        Direction side = BlockUtil.getClickSide(breakPos);
        if (rotate.getValue()) {
            Vec3i vec3i = side.getVector();
            if (!faceVector(breakPos.toCenterPos().add(new Vec3d(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5)))) {
                return;
            }
        }
        if (!startTime.passed(startDelay.getValueInt())) return;
        if (swing.getValue()) {
            EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
        }
        if (doubleBreak.getValue()) {
            if (secondPos == null || isAir(secondPos)) {
                int slot = getTool(breakPos);
                if (slot == -1) {
                    slot = mc.player.getInventory().selectedSlot;
                }
                secondFinalTime = (getBreakTime(breakPos, slot, 1));
                secondAnim.reset();
                secondAnim.setLength((long) secondFinalTime);
                secondTimer.reset();
                secondPos = breakPos;
            }
            doDoubleBreak(side);
        }
        int slot = getTool(breakPos);
        if (slot == -1) {
            slot = mc.player.getInventory().selectedSlot;
        }
        breakFinalTime = getBreakTime(breakPos, slot);
        sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, breakPos, side, id));
        if (rotate.getValue() && !shouldYawStep()) {
            Alien.ROTATION.snapBack();
        }
        startTime.reset();
    }

    public void mine(BlockPos pos) {
        if (nullCheck()) {
            return;
        }
        if (mc.player.isCreative()) {
            mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            return;
        }
        if (isOff()) {
            mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            return;
        }
        if (pos.equals(breakPos)) {
            return;
        }
        if (unbreakable(pos)) {
            return;
        }
        if (breakPos != null && preferWeb.getValue() && getBlock(breakPos) == Blocks.COBWEB) {
            return;
        }
        if (breakPos != null && preferHead.getValue() && mc.player.isCrawling() && EntityUtil.getPlayerPos(true).up().equals(breakPos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > range.getValue()) {
            return;
        }
        breakPos = pos;
        breakNumber = 0;
        startPacket = false;
        ghost = false;
        complete = false;
        mineTimer.reset();
        animationTime.reset();
    }

    boolean faceVector(Vec3d directionVec) {
        if (!shouldYawStep()) {
            Alien.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.sync.reset();
            this.directionVec = directionVec;
            if (Alien.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (breakPos != null && mc.world.isAir(breakPos)) {
            complete = true;
        }
        if (!mc.player.isCreative()) {
            if (secondPos != null) {
                if (isAir(secondPos)) {
                    secondPos = null;
                    return;
                }
                if (!checkDouble.getValue() || !secondPos.equals(breakPos)) {
                    secondAnim.setLength((long) secondFinalTime);
                    double ease = secondAnim.ease(this.ease.getValue());
                    if (box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, getFillBox(secondPos, ease), doubleColor.getValue());
                    }
                    if (outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, getOutlineBox(secondPos, ease), doubleOutlineColor.getValue());
                    }
                }
            }
            if (breakPos != null) {
                progress = (double) mineTimer.getMs() / breakFinalTime;
                animationTime.setLength((long) breakFinalTime);
                double ease = animationTime.ease(this.ease.getValue());
                if (unbreakable(breakPos)) {
                    if (box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, new Box(breakPos), startColor.getValue());
                    }
                    if (outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, new Box(breakPos), startOutlineColor.getValue());
                    }
                    return;
                }
                if (box.getValue()) {
                    Render3DUtil.drawFill(matrixStack, getFillBox(breakPos, ease), getColor(animationTime.ease(fadeEase.getValue())));
                }
                if (outline.getValue()) {
                    Render3DUtil.drawBox(matrixStack, getOutlineBox(breakPos, ease), getOutlineColor(animationTime.ease(fadeEase.getValue())));
                }
                if (text.getValue()) {
                    if (isAir(breakPos)) {
                        Render3DUtil.drawText3D("Waiting", breakPos.toCenterPos(), -1);
                    } else {
                        if ((int) mineTimer.getMs() < breakFinalTime) {
                            Render3DUtil.drawText3D(df.format(progress * 100) + "%", breakPos.toCenterPos(), -1);
                        } else {
                            Render3DUtil.drawText3D("100.0%", breakPos.toCenterPos(), -1);
                        }
                    }
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }
    }

    private Box getFillBox(BlockPos pos, double ease) {
        switch (animation.getValue()) {
            case Grow -> {
                ease = (1 - ease) / 2;
                return new Box(pos).shrink(ease, 0, ease).shrink(-ease, 0, -ease);
            }
            case Down -> {
                return new Box(pos.getX(), pos.getY() + 1 - ease, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            }
            case Up -> {
                return new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + ease, pos.getZ() + 1);
            }
            case Center -> {
                ease = (1 - ease) / 2;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
            case Oscillation -> {
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
        }
        return new Box(pos);
    }

    private Box getOutlineBox(BlockPos pos, double ease) {
        ease = Math.min(ease + expandLine.getValue(), 1);
        switch (animation.getValue()) {
            case Grow -> {
                ease = (1 - ease) / 2;
                return new Box(pos).shrink(ease, 0, ease).shrink(-ease, 0, -ease);
            }
            case Down -> {
                return new Box(pos.getX(), pos.getY() + 1 - ease, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            }
            case Up -> {
                return new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + ease, pos.getZ() + 1);
            }
            case Center -> {
                ease = (1 - ease) / 2;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
            case Oscillation -> {
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            }
        }
        return new Box(pos);
    }

    @EventListener(priority = EventPriority.LOWEST)
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck() || mc.player.isCreative()) {
            return;
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (bypassGround.getValue() && !mc.player.isGliding() && breakPos != null && !isAir(breakPos) && bypassTime.getValue() > 0 && MathHelper.sqrt((float) breakPos.toCenterPos().squaredDistanceTo(mc.player.getEyePos())) <= range.getValueFloat() + 2) {
                double breakTime = (breakFinalTime - bypassTime.getValue());
                if (breakTime <= 0 || mineTimer.passed((long) breakTime)) {
                    sendGroundPacket = true;
                    ((IPlayerMoveC2SPacket) event.getPacket()).setOnGround(true);
                }
            } else {
                sendGroundPacket = false;
            }
        } else if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
            if (packet.getSelectedSlot() != lastSlot) {
                lastSlot = packet.getSelectedSlot();
                if (switchReset.getValue()) {
                    startPacket = false;
                    ghost = false;
                    complete = false;
                    mineTimer.reset();
                    animationTime.reset();
                }
            }
        } else if (event.getPacket() instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                if (breakPos == null || !packet.getPos().equals(breakPos)) {
                    return;
                }
                if (grimDisabler.getValue())
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), packet.getDirection()));
                startPacket = true;
            } else if (packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                if (breakPos == null || !packet.getPos().equals(breakPos)) {
                    return;
                }
                if (!instant.getValue()) {
                    startPacket = false;
                    ghost = false;
                    complete = false;
                }
            }
        }
    }

    boolean canPlaceCrystal(BlockPos pos) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
                && BlockUtil.getClickSideStrict(obsPos) != null
                && noEntity(boost)
                && noEntity(boost.up())
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost.up()));
    }

    boolean noEntity(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (entity instanceof ItemEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.ignoreArmorStand.getValue())
                continue;
            return false;
        }
        return true;
    }

    void doSwap(int slot, int inv) {
        if (!inventory.getValue()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(inv, mc.player.getInventory().selectedSlot);
        }
    }

    int findCrystal() {
        if (inventory.getValue()) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        } else {
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        }
    }

    int findBlock(Block block) {
        if (inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(block);
        } else {
            return InventoryUtil.findBlock(block);
        }
    }

    boolean shouldCrystal() {
        return crystal.getValue() && (!onlyHeadBomber.getValue() || obsidian.isPressed()) || CevBreaker.INSTANCE.isOn();
    }

    public static double getBreakTime(BlockPos pos) {
        int slot = INSTANCE.getTool(pos);
        if (slot == -1) {
            slot = mc.player.getInventory().selectedSlot;
        }
        return INSTANCE.getBreakTime(pos, slot);
    }

    double getBreakTime(BlockPos pos, int slot) {
        return getBreakTime(pos, slot, damage.getValue());
    }

    double getBreakTime(BlockPos pos, int slot, double damage) {
        return (1 / getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20 * 1000 * damage);
    }

    float getBlockStrength(BlockPos position, ItemStack itemStack) {
        BlockState state = mc.world.getBlockState(position);
        float hardness = state.getHardness(mc.world, position);
        if (hardness < 0) {
            return 0;
        }
        float i = !state.isToolRequired() || itemStack.isSuitableFor(state) ? 30 : 100;
        return getDigSpeed(state, itemStack) / hardness / i;
    }

    float getDigSpeed(BlockState state, ItemStack itemStack) {
        boolean inWeb;
        int efficiencyModifier;
        float digSpeed = this.getDestroySpeed(state, itemStack);
        if (digSpeed > 1.0f && (efficiencyModifier = EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().getOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOrThrow(Enchantments.EFFICIENCY)), (ItemStack)itemStack)) > 0 && !itemStack.isEmpty()) {
            digSpeed += (float)(StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
        }
        if (PacketMine.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            digSpeed *= 1.0f + (float)(PacketMine.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2f;
        }
        if (PacketMine.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            digSpeed *= (switch (PacketMine.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            });
        }
        if (PacketMine.mc.player.isSubmergedInWater()) {
            digSpeed *= (float)1.0;
        }
        boolean bl = inWeb = this.checkWeb.getValue() && Alien.PLAYER.isInWeb((PlayerEntity)PacketMine.mc.player) && PacketMine.mc.world.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
        if ((!PacketMine.mc.player.isOnGround() || inWeb) && PacketMine.INSTANCE.checkGround.getValue() && (!this.smart.getValue() || Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || PacketMine.mc.player.isGliding() || inWeb)) {
            digSpeed /= 5.0f;
        }
        return digSpeed < 0.0f ? 0.0f : digSpeed;
    }

    float getDestroySpeed(BlockState state, ItemStack itemStack) {
        float destroySpeed = 1;
        if (itemStack != null && !itemStack.isEmpty()) {
            destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
        }
        return destroySpeed;
    }

    Color getColor(double quad) {
        int sR = startColor.getValue().getRed();
        int sG = startColor.getValue().getGreen();
        int sB = startColor.getValue().getBlue();
        int sA = startColor.getValue().getAlpha();

        int eR = endColor.getValue().getRed();
        int eG = endColor.getValue().getGreen();
        int eB = endColor.getValue().getBlue();
        int eA = endColor.getValue().getAlpha();
        return new Color((int) (sR + (eR - sR) * quad),
                (int) (sG + (eG - sG) * quad),
                (int) (sB + (eB - sB) * quad),
                (int) (sA + (eA - sA) * quad));
    }

    Color getOutlineColor(double quad) {
        int sR = startOutlineColor.getValue().getRed();
        int sG = startOutlineColor.getValue().getGreen();
        int sB = startOutlineColor.getValue().getBlue();
        int sA = startOutlineColor.getValue().getAlpha();

        int eR = endOutlineColor.getValue().getRed();
        int eG = endOutlineColor.getValue().getGreen();
        int eB = endOutlineColor.getValue().getBlue();
        int eA = endOutlineColor.getValue().getAlpha();
        return new Color((int) (sR + (eR - sR) * quad),
                (int) (sG + (eG - sG) * quad),
                (int) (sB + (eB - sB) * quad),
                (int) (sA + (eA - sA) * quad));
    }

    int getTool(BlockPos pos) {
        if (this.hotBar.getValue()) {
            int index = -1;
            float CurrentFastest = 1.0f;
            for (int i = 0; i < 9; ++i) {
                float destroySpeed;
                float digSpeed;
                ItemStack stack = PacketMine.mc.player.getInventory().getStack(i);
                if (stack == ItemStack.EMPTY || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().getOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOrThrow(Enchantments.EFFICIENCY)), (ItemStack)stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
                CurrentFastest = digSpeed + destroySpeed;
                index = i;
            }
            return index;
        }
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        float CurrentFastest = 1.0f;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            float destroySpeed;
            float digSpeed;
            if (entry.getValue().getItem() instanceof AirBlockItem || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().getOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOrThrow(Enchantments.EFFICIENCY)), (ItemStack)entry.getValue())) + (destroySpeed = entry.getValue().getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
            CurrentFastest = digSpeed + destroySpeed;
            slot.set(entry.getKey());
        }
        return slot.get();
    }
    private boolean shouldYawStep() {
        if (!whenElytra.getValue() && (mc.player.isGliding() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isGliding()))
            return false;
        return yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    boolean isAir(BlockPos breakPos) {
        return mc.world.isAir(breakPos) || getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
    }

    public static boolean unbreakable(BlockPos blockPos) {
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return !(block instanceof AirBlock) && (block.getHardness() == -1 || block.getHardness() == 100);
    }

    private enum Animation {
        Center,
        Grow,
        Up,
        Down,
        Oscillation,
        None
    }

    public enum Page {
        General,
        Check,
        Place,
        Rotation,
        Render,
    }
}
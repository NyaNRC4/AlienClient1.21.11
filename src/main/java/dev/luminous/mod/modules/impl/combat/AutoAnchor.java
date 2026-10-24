package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.PlayerEntityPredict;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.ExplosionUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.ArrayList;

import static dev.luminous.api.utils.world.BlockUtil.*;

public class AutoAnchor extends Module {
    public static AutoAnchor INSTANCE;
    static Vec3d placeVec3d;
    static Vec3d curVec3d;
    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));
    public final SliderSetting range =
            add(new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1, () -> page.getValue() == Page.General).setSuffix("m"));
    public final SliderSetting targetRange =
            add(new SliderSetting("TargetRange", 8.0, 0.10, 12, 0.1, () -> page.getValue() == Page.General).setSuffix("m"));
    public final SliderSetting minDamage =
            add(new SliderSetting("Min", 4.0, 0.0, 36.0, 0.1, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    public final SliderSetting breakMin =
            add(new SliderSetting("ExplosionMin", 4.0, 0.0, 36.0, 0.1, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    public final SliderSetting headDamage =
            add(new SliderSetting("ForceHead", 7.0, 0.0, 36.0, 0.1, () -> page.getValue() == Page.Interact).setSuffix("dmg"));

    private final SliderSetting selfPredict = add(new SliderSetting("SelfPredict", 4, 0, 10, () -> page.getValue() == Page.Predict).setSuffix("ticks"));
    private final SliderSetting predictTicks = add(new SliderSetting("Predict", 4, 0, 10, () -> page.getValue() == Page.Predict).setSuffix("ticks"));
    private final SliderSetting simulation =
            add(new SliderSetting("Simulation", 5, 0, 20, 1, () -> page.getValue() == Page.Predict));
    private final SliderSetting maxMotionY =
            add(new SliderSetting("MaxMotionY", 0.34, 0, 2, 0.01, () -> page.getValue() == Page.Predict));
    private final BooleanSetting step =
            add(new BooleanSetting("Step", false, () -> page.getValue() == Page.Predict));
    private final BooleanSetting doubleStep =
            add(new BooleanSetting("DoubleStep", false, () -> page.getValue() == Page.Predict));
    private final BooleanSetting jump =
            add(new BooleanSetting("Jump", false, () -> page.getValue() == Page.Predict));
    private final BooleanSetting inBlockPause =
            add(new BooleanSetting("InBlockPause", true, () -> page.getValue() == Page.Predict));
    final ArrayList<BlockPos> chargeList = new ArrayList<>();
    //Assist
    private final BooleanSetting assist =
            add(new BooleanSetting("Assist", true, () -> page.getValue() == Page.Assist));
    private final BooleanSetting obsidian =
            add(new BooleanSetting("Obsidian", true, () -> page.getValue() == Page.Assist));
    private final BooleanSetting checkMine =
            add(new BooleanSetting("DetectMining", false, () -> page.getValue() == Page.Assist));
    private final SliderSetting assistRange =
            add(new SliderSetting("AssistRange", 5.0, 0.0, 6.0, 0.1, () -> page.getValue() == Page.Assist).setSuffix("m"));
    private final SliderSetting assistDamage =
            add(new SliderSetting("AssistDamage", 6.0, 0.0, 36.0, 0.1, () -> page.getValue() == Page.Assist).setSuffix("h"));
    private final SliderSetting delay =
            add(new SliderSetting("AssistDelay", 0.1, 0.0, 1, 0.01, () -> page.getValue() == Page.Assist).setSuffix("s"));
    //
    private final BooleanSetting preferCrystal = add(new BooleanSetting("PreferCrystal", false, () -> page.getValue() == Page.General));
    private final BooleanSetting thread =
            add(new BooleanSetting("Thread", false, () -> page.getValue() == Page.General));
    private final BooleanSetting light =
            add(new BooleanSetting("LessCPU", true, () -> page.getValue() == Page.General));
    private final BooleanSetting inventorySwap =
            add(new BooleanSetting("InventorySwap", true, () -> page.getValue() == Page.General));
    private final BooleanSetting breakCrystal =
            add(new BooleanSetting("BreakCrystal", true, () -> page.getValue() == Page.General).setParent());
    private final BooleanSetting spam =
            add(new BooleanSetting("Spam", true, () -> page.getValue() == Page.General).setParent());
    private final BooleanSetting mineSpam =
            add(new BooleanSetting("OnlyMining", true, () -> page.getValue() == Page.General && spam.isOpen()));
    private final BooleanSetting spamPlace =
            add(new BooleanSetting("Fast", true, () -> page.getValue() == Page.General).setParent());
    private final BooleanSetting inSpam =
            add(new BooleanSetting("WhenSpamming", true, () -> page.getValue() == Page.General && spamPlace.isOpen()));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true, () -> page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("Swing", SwingSide.All, () -> page.getValue() == Page.General));
    private final EnumSetting<Timing> timing =
            add(new EnumSetting<>("Timing", Timing.All, () -> page.getValue() == Page.General));
    private final SliderSetting placeDelay =
            add(new SliderSetting("PlaceDelay", 100, 0, 500, 1, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting fillDelay =
            add(new SliderSetting("FillDelay", 100, 0, 500, 1, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting breakDelay =
            add(new SliderSetting("BreakDelay", 100, 0, 500, 1, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting spamDelay =
            add(new SliderSetting("SpamDelay", 200, 0, 1000, 1, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting updateDelay =
            add(new SliderSetting("UpdateDelay", 200, 0, 1000, 1, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotate).setParent());
    private final BooleanSetting yawStep =
            add(new BooleanSetting("YawStep", true, () -> rotate.isOpen() && page.getValue() == Page.Rotate).setParent());
    private final BooleanSetting whenElytra =
            add(new BooleanSetting("FallFlying", true, () -> rotate.isOpen() && yawStep.isOpen() && page.getValue() == Page.Rotate));
    private final SliderSetting steps =
            add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && yawStep.isOpen() && page.getValue() == Page.Rotate));
    private final BooleanSetting checkFov =
            add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen() && yawStep.isOpen() && page.getValue() == Page.Rotate));
    private final SliderSetting fov =
            add(new SliderSetting("Fov", 20, 0, 360, 0.1, () -> rotate.isOpen() && yawStep.isOpen() && checkFov.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting priority =
            add(new SliderSetting("Priority", 10, 0, 100, () -> rotate.isOpen() && yawStep.isOpen() && page.getValue() == Page.Rotate));
    private final BooleanSetting noSuicide =
            add(new BooleanSetting("NoSuicide", true, () -> page.getValue() == Page.Interact));
    private final BooleanSetting smart =
            add(new BooleanSetting("Smart", true, () -> page.getValue() == Page.Interact));
    private final BooleanSetting terrainIgnore =
            add(new BooleanSetting("TerrainIgnore", true, () -> page.getValue() == Page.Interact));
    private final SliderSetting minPrefer =
            add(new SliderSetting("Prefer", 7.0, 0.0, 36.0, 0.1, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final SliderSetting maxSelfDamage =
            add(new SliderSetting("MaxSelf", 8.0, 0.0, 36.0, 0.1, () -> page.getValue() == Page.Interact).setSuffix("dmg"));
    private final EnumSetting<Aura.TargetESP> mode = add(new EnumSetting<>("TargetESP", Aura.TargetESP.Jello, () -> page.getValue() == Page.Render));
    private final ColorSetting color = add(new ColorSetting("TargetColor", new Color(255, 255, 255, 250), () -> page.getValue() == Page.Render));
    private final ColorSetting outlineColor = add(new ColorSetting("TargetOutlineColor", new Color(255, 255, 255, 250), () -> page.getValue() == Page.Render));
    private final BooleanSetting render =
            add(new BooleanSetting("Render", true, () -> page.getValue() == Page.Render));
    private final BooleanSetting shrink =
            add(new BooleanSetting("Shrink", true, () -> page.getValue() == Page.Render && render.getValue()));
    private final ColorSetting box =
            add(new ColorSetting("Box", new Color(255, 255, 255, 255), () -> page.getValue() == Page.Render && render.getValue()).injectBoolean(true));
    private final ColorSetting fill =
            add(new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> page.getValue() == Page.Render && render.getValue()).injectBoolean(true));
    private final SliderSetting sliderSpeed = add(new SliderSetting("SliderSpeed", 0.2, 0d, 1, 0.01, () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting startFadeTime =
            add(new SliderSetting("StartFade", 0.3d, 0d, 2d, 0.01, () -> page.getValue() == Page.Render && render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed =
            add(new SliderSetting("FadeSpeed", 0.2d, 0.01d, 1d, 0.01, () -> page.getValue() == Page.Render && render.getValue()));
    private final Timer delayTimer = new Timer();
    private final Timer calcTimer = new Timer();
    private final Timer noPosTimer = new Timer();
    private final Timer assistTimer = new Timer();
    public Vec3d directionVec = null;
    public PlayerEntity displayTarget;
    public BlockPos currentPos;
    public BlockPos tempPos;
    public double lastDamage;
    double fade = 0;
    BlockPos assistPos;

    public AutoAnchor() {
        super("AutoAnchor", Category.Combat);
        setChinese("重生锚光环");
        INSTANCE = this;
        Alien.EVENT_BUS.subscribe(new AnchorRender());
    }

    public static boolean canSee(Vec3d from, Vec3d to) {
        HitResult result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    @Override
    public String getInfo() {
        if (displayTarget != null && currentPos != null) return displayTarget.getName().getString();
        return null;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        try {
            final PlayerEntity localTarget = displayTarget;
            final BlockPos localPos = currentPos;

            if (localTarget != null && localPos != null && !localTarget.isRemoved() && localTarget.getEntityWorld() != null) {
                float partialTicks = (float) mc.getRenderTickCounter().getTickProgress(true);
                Aura.doRender(matrixStack, partialTicks, localTarget, color.getValue(), outlineColor.getValue(), mode.getValue());
            }
        } catch (Throwable ignored) {
        }
    }


    @EventListener
    public void onRotate(RotationEvent event) {
        if (currentPos != null && rotate.getValue() && shouldYawStep() && directionVec != null) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @Override
    public void onDisable() {
        tempPos = null;
        currentPos = null;
    }

    public void onThread() {
        if (isOff() || nullCheck()) return;
        if (thread.getValue()) {
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
                currentPos = null;
                return;
            }
            if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && preferCrystal.getValue()) {
                currentPos = null;
                return;
            }
            int anchor = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
            int glowstone = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
            int unBlock = inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
            if (anchor == -1) {
                currentPos = null;
                return;
            }
            if (glowstone == -1) {
                currentPos = null;
                return;
            }
            if (unBlock == -1) {
                currentPos = null;
                return;
            }
            if (mc.player.isSneaking()) {
                currentPos = null;
                return;
            }
            if (usingPause.getValue() && mc.player.isUsingItem()) {
                currentPos = null;
                return;
            }
            calc();
        }
    }

    private boolean shouldYawStep() {
        if (!whenElytra.getValue() && (mc.player.isGliding() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isGliding()))
            return false;
        return yawStep.getValue() && !Velocity.INSTANCE.noRotation();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (timing.is(Timing.Pre) && event.isPost() || timing.is(Timing.Post) && event.isPre()) return;

        int anchor = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int unBlock = inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
        int old = mc.player.getInventory().selectedSlot;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            currentPos = null;
            return;
        }
        if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null) {
            currentPos = null;
            return;
        }
        if (anchor == -1) {
            currentPos = null;
            return;
        }
        if (glowstone == -1) {
            currentPos = null;
            return;
        }
        if (unBlock == -1) {
            currentPos = null;
            return;
        }
        if (mc.player.isSneaking()) {
            currentPos = null;
            return;
        }
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            currentPos = null;
            return;
        }
        if (inventorySwap.getValue() && !EntityUtil.inInventory()) return;
        if (assist.getValue()) onAssist();
        if (!thread.getValue()) calc();
        BlockPos pos = currentPos;
        if (pos != null) {
            if (breakCrystal.getValue()) CombatUtil.attackCrystal(new BlockPos(pos), rotate.getValue(), false);
            boolean shouldSpam = this.spam.getValue() && (!mineSpam.getValue() || Alien.BREAK.isMining(pos));
            if (shouldSpam) {
                if (!delayTimer.passed((long) (spamDelay.getValueFloat()))) {
                    return;
                }
                delayTimer.reset();
                if (canPlace(pos, range.getValue(), breakCrystal.getValue())) {
                    placeBlock(pos, rotate.getValue(), anchor);
                }
                if (!chargeList.contains(pos)) {
                    delayTimer.reset();
                    clickBlock(pos, getClickSide(pos), rotate.getValue(), glowstone);
                    chargeList.add(pos);
                }
                chargeList.remove(pos);
                clickBlock(pos, getClickSide(pos), rotate.getValue(), unBlock);
                if (spamPlace.getValue() && inSpam.getValue()) {
                    if (shouldYawStep() && checkFov.getValue()) {
                        Direction side = getClickSide(pos);
                        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
                        if (Alien.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                            placeBlock(pos, rotate.getValue(), anchor);
                            CombatUtil.modifyPos = null;
                        }
                    } else {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        placeBlock(pos, rotate.getValue(), anchor);
                        CombatUtil.modifyPos = null;
                    }
                }
            } else {
                if (canPlace(pos, range.getValue(), breakCrystal.getValue())) {
                    if (!delayTimer.passed((long) (placeDelay.getValueFloat()))) {
                        return;
                    }
                    delayTimer.reset();
                    placeBlock(pos, rotate.getValue(), anchor);
                } else if (getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
                    if (!chargeList.contains(pos)) {
                        if (!delayTimer.passed((long) (fillDelay.getValueFloat()))) {
                            return;
                        }
                        delayTimer.reset();
                        clickBlock(pos, getClickSide(pos), rotate.getValue(), glowstone);
                        chargeList.add(pos);
                    } else {
                        if (!delayTimer.passed((long) (breakDelay.getValueFloat()))) {
                            return;
                        }
                        delayTimer.reset();
                        chargeList.remove(pos);
                        clickBlock(pos, getClickSide(pos), rotate.getValue(), unBlock);
                        if (spamPlace.getValue()) {
                            if (shouldYawStep() && checkFov.getValue()) {
                                Direction side = getClickSide(pos);
                                Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
                                if (Alien.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                                    CombatUtil.modifyPos = pos;
                                    CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                                    placeBlock(pos, rotate.getValue(), anchor);
                                    CombatUtil.modifyPos = null;
                                }
                            } else {
                                CombatUtil.modifyPos = pos;
                                CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                                placeBlock(pos, rotate.getValue(), anchor);
                                CombatUtil.modifyPos = null;
                            }
                        }
                    }
                }
            }
            if (!inventorySwap.getValue()) doSwap(old);
        }
    }

    private void calc() {
        if (nullCheck()) return;
        if (calcTimer.passed((long) (updateDelay.getValueFloat()))) {
            calcTimer.reset();
            PlayerEntityPredict selfPredict = new PlayerEntityPredict(mc.player, maxMotionY.getValue(), this.selfPredict.getValueInt(), simulation.getValueInt(), step.getValue(), doubleStep.getValue(), jump.getValue(), inBlockPause.getValue());
            tempPos = null;
            double placeDamage = minDamage.getValue();
            double breakDamage = breakMin.getValue();
            boolean anchorFound = false;
            java.util.List<PlayerEntity> enemies = CombatUtil.getEnemies(targetRange.getValue());
            ArrayList<PlayerEntityPredict> list = new ArrayList<>();
            for (PlayerEntity player : enemies) {
                list.add(new PlayerEntityPredict(player, maxMotionY.getValue(), predictTicks.getValueInt(), simulation.getValueInt(), step.getValue(), doubleStep.getValue(), jump.getValue(), inBlockPause.getValue()));
            }
            for (PlayerEntityPredict pap : list) {
                BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
                if (canPlace(pos, range.getValue(), breakCrystal.getValue()) || getBlock(pos) == Blocks.RESPAWN_ANCHOR && BlockUtil.getClickSideStrict(pos) != null) {
                    double selfDamage;
                    if ((selfDamage = getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > maxSelfDamage.getValue() || noSuicide.getValue() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                        continue;
                    }
                    double damage;
                    if ((damage = getAnchorDamage(pos, pap.player, pap.predict)) > headDamage.getValueFloat()) {
                        if (smart.getValue()) {
                            if (selfDamage > damage) continue;
                        }
                        lastDamage = damage;
                        displayTarget = pap.player;
                        tempPos = pos;
                        break;
                    }
                }
            }
            if (tempPos == null) {
                for (BlockPos pos : getSphere(range.getValueFloat() + 1, mc.player.getEyePos())) {
                    for (PlayerEntityPredict pap : list) {
                        if (light.getValue()) {
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                            boolean skip = !canSee(pos.toCenterPos(), pap.predict.getEntityPos());
                            CombatUtil.modifyPos = null;
                            if (skip) continue;
                        }

                        if (getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                            if (anchorFound) continue;
                            if (!canPlace(pos, range.getValue(), breakCrystal.getValue())) continue;

                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                            boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                            CombatUtil.modifyPos = null;
                            if (skip) continue;

                            double damage = getAnchorDamage(pos, pap.player, pap.predict);
                            if (damage >= placeDamage) {
                                if (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || AutoCrystal.INSTANCE.lastDamage < damage) {
                                    double selfDamage;
                                    if ((selfDamage = getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > maxSelfDamage.getValue() || noSuicide.getValue() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                                        continue;
                                    }
                                    if (smart.getValue()) {
                                        if (selfDamage > damage) continue;
                                    }
                                    lastDamage = damage;
                                    displayTarget = pap.player;
                                    placeDamage = damage;
                                    tempPos = pos;
                                }
                            }
                        } else {
                            double damage = getAnchorDamage(pos, pap.player, pap.predict);
                            if (getClickSideStrict(pos) == null) continue;
                            if (damage >= breakDamage) {
                                if (damage >= minPrefer.getValue()) anchorFound = true;
                                if (!anchorFound && damage < placeDamage) {
                                    continue;
                                }
                                if (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || AutoCrystal.INSTANCE.lastDamage < damage) {
                                    double selfDamage;
                                    if ((selfDamage = getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > maxSelfDamage.getValue() || noSuicide.getValue() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                                        continue;
                                    }
                                    if (smart.getValue()) {
                                        if (selfDamage > damage) continue;
                                    }
                                    lastDamage = damage;
                                    displayTarget = pap.player;
                                    breakDamage = damage;
                                    tempPos = pos;
                                }
                            }
                        }
                    }
                }
            }
        }
        currentPos = tempPos;
    }

    public double getAnchorDamage(BlockPos anchorPos, PlayerEntity target, PlayerEntity predict) {
        if (terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }
        double damage = ExplosionUtil.anchorDamage(anchorPos, target, predict);
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    public void placeBlock(BlockPos pos, boolean rotate, int slot) {
        if (BlockUtil.allowAirPlace()) {
            //BlockUtil.placedPos.add(pos);
            airPlace(pos, rotate, slot);
            return;
        }
        Direction side = getPlaceSide(pos);
        if (side == null) return;
        //BlockUtil.placedPos.add(pos);
        clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
    }

    public void clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
        if (pos == null) return;
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (rotate) {
            if (!faceVector(directionVec)) return;
        }
        doSwap(slot);
        EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        if (inventorySwap.getValue()) {
            doSwap(slot);
        }
        if (rotate && !shouldYawStep()) {
            Alien.ROTATION.snapBack();
        }
    }

    public void airPlace(BlockPos pos, boolean rotate, int slot) {
        if (pos == null) return;
        Direction side = BlockUtil.getClickSide(pos);
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (rotate) {
            if (!faceVector(directionVec)) return;
        }
        doSwap(slot);
        boolean bypass = AirPlace.INSTANCE.grimBypass.getValue();
        if (bypass) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
        }
        EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(bypass ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
        if (bypass) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
        }
        if (inventorySwap.getValue()) {
            doSwap(slot);
        }
        if (rotate && !shouldYawStep()) {
            Alien.ROTATION.snapBack();
        }
    }

    private void doSwap(int slot) {
        if (inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    public boolean faceVector(Vec3d directionVec) {
        if (!shouldYawStep()) {
            Alien.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            if (Alien.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }

    public void onAssist() {
        assistPos = null;
        int anchor = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int old = mc.player.getInventory().selectedSlot;
        if (anchor == -1) {
            return;
        }
        if (obsidian.getValue()) {
            anchor = inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (anchor == -1) {
                return;
            }
        }
        if (glowstone == -1) {
            return;
        }
        if (mc.player.isSneaking()) {
            return;
        }
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (!assistTimer.passed((long) (delay.getValueFloat() * 1000))) {
            return;
        }
        assistTimer.reset();
        double bestDamage;
        ArrayList<PlayerEntityPredict> list = new ArrayList<>();
        for (PlayerEntity player : CombatUtil.getEnemies(assistRange.getValue())) {
            list.add(new PlayerEntityPredict(player, maxMotionY.getValue(), predictTicks.getValueInt(), simulation.getValueInt(), step.getValue(), doubleStep.getValue(), jump.getValue(), inBlockPause.getValue()));
        }

        bestDamage = assistDamage.getValue();
        for (PlayerEntityPredict pap : list) {
            BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
            if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                return;
            }
            if (BlockUtil.clientCanPlace(pos, false)) {
                double damage = getAnchorDamage(pos, pap.player, pap.predict);
                if (damage >= bestDamage) {
                    bestDamage = damage;
                    assistPos = pos;
                }
            }
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (BlockUtil.clientCanPlace(pos.offset(i), false)) {
                    double damage = getAnchorDamage(pos.offset(i), pap.player, pap.predict);
                    if (damage >= bestDamage) {
                        bestDamage = damage;
                        assistPos = pos.offset(i);
                    }
                }
            }
        }
        if (assistPos != null && BlockUtil.getPlaceSide(assistPos, range.getValue()) == null) {
            BlockPos placePos;
            if ((placePos = getHelper(assistPos)) != null) {
                doSwap(anchor);
                BlockUtil.placeBlock(placePos, rotate.getValue());
                if (inventorySwap.getValue()) {
                    doSwap(anchor);
                } else {
                    doSwap(old);
                }
            }
        }
    }

    public BlockPos getHelper(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (checkMine.getValue() && Alien.BREAK.isMining(pos.offset(i))) continue;
            if (!BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())) continue;
            if (BlockUtil.canPlace(pos.offset(i))) return pos.offset(i);
        }
        return null;
    }

    public enum Page {
        General,
        Interact,
        Predict,
        Rotate,
        Assist,
        Render,
    }

    public class AnchorRender {
        @EventListener
        public void onRender3D(Render3DEvent event) {
            BlockPos currentPos = AutoAnchor.INSTANCE.currentPos;
            if (currentPos != null) {
                noPosTimer.reset();
                placeVec3d = currentPos.toCenterPos();
            }
            if (placeVec3d == null) {
                return;
            }
            if (fadeSpeed.getValue() >= 1) {
                fade = noPosTimer.passed((long) (startFadeTime.getValue() * 1000)) ? 0 : 0.5;
            } else {
                fade = AnimateUtil.animate(fade, noPosTimer.passed((long) (startFadeTime.getValue() * 1000)) ? 0 : 0.5, fadeSpeed.getValue() / 10);
            }
            if (fade == 0) {
                curVec3d = null;
                return;
            }
            if (curVec3d == null || sliderSpeed.getValue() >= 1) {
                curVec3d = placeVec3d;
            } else {
                curVec3d = new Vec3d(AnimateUtil.animate(curVec3d.x, placeVec3d.x, sliderSpeed.getValue() / 10),
                        AnimateUtil.animate(curVec3d.y, placeVec3d.y, sliderSpeed.getValue() / 10),
                        AnimateUtil.animate(curVec3d.z, placeVec3d.z, sliderSpeed.getValue() / 10));
            }

            if (render.getValue()) {
                Box cbox = new Box(curVec3d, curVec3d);
                if (shrink.getValue()) {
                    cbox = cbox.expand(fade);
                } else {
                    cbox = cbox.expand(0.5);
                }
                if (fill.booleanValue) {
                    event.drawFill(cbox, ColorUtil.injectAlpha(fill.getValue(), (int) (fill.getValue().getAlpha() * fade * 2D)));
                }
                if (box.booleanValue) {
                    event.drawBox(cbox, ColorUtil.injectAlpha(box.getValue(), (int) (box.getValue().getAlpha() * fade * 2D)));
                }
            }
        }
    }
}
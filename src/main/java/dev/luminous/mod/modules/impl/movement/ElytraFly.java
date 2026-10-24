package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.*;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.asm.accessors.IFireworkRocketEntity;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static dev.luminous.api.utils.player.MovementUtil.*;

public class ElytraFly extends Module {
    public static ElytraFly INSTANCE;
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Control));
    public final BooleanSetting infiniteDura = add(new BooleanSetting("InfiniteDura", false));
    public final BooleanSetting packet = add(new BooleanSetting("Packet", false).setParent());
    private final SliderSetting packetDelay =
            add(new SliderSetting("PacketDelay", 0, 0, 20, 1, packet::isOpen));
    private final BooleanSetting setFlag = add(new BooleanSetting("SetFlag", false, () -> !mode.is(Mode.Bounce)));
    private final BooleanSetting firework = add(new BooleanSetting("Firework", false).setParent());
    public final BindSetting fireWork = add(new BindSetting("FireWorkBind", -1, firework::isOpen));
    public final BooleanSetting packetInteract = add(new BooleanSetting("PacketInteract", true, firework::isOpen));
    public final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true, firework::isOpen));
    public final BooleanSetting onlyOne = add(new BooleanSetting("OnlyOne", true, firework::isOpen));
    private final BooleanSetting usingPause = add(new BooleanSetting("UsingPause", true, firework::isOpen));
    public final BooleanSetting autoJump = add(new BooleanSetting("AutoJump", true, () -> mode.is(Mode.Bounce)));
    public final SliderSetting upPitch = add(new SliderSetting("UpPitch", 0.0f, 0.0f, 90.0f, () -> mode.getValue() == Mode.Control));
    public final SliderSetting upFactor = add(new SliderSetting("UpFactor", 1.0f, 0.0f, 10.0f, () -> mode.getValue() == Mode.Control));
    public final SliderSetting downFactor = add(new SliderSetting("FallSpeed", 1.0f, 0.0f, 10.0f, () -> mode.getValue() == Mode.Control));
    public final SliderSetting speed = add(new SliderSetting("Speed", 1.0f, 0.1f, 10.0f, () -> mode.getValue() == Mode.Control));
    public final BooleanSetting speedLimit = add(new BooleanSetting("SpeedLimit", true, () -> mode.getValue() == Mode.Control));
    public final SliderSetting maxSpeed = add(new SliderSetting("MaxSpeed", 2.5f, 0.1f, 10.0f, () -> speedLimit.getValue() && mode.getValue() == Mode.Control));
    public final BooleanSetting noDrag = add(new BooleanSetting("NoDrag", false, () -> mode.getValue() == Mode.Control));
    public final Timer fireworkTimer = new Timer();
    private final BooleanSetting autoStop = add(new BooleanSetting("AutoStop", true));
    private final BooleanSetting sprint = add(new BooleanSetting("Sprint", true, () -> mode.is(Mode.Bounce)));
    private final SliderSetting pitch =
            add(new SliderSetting("Pitch", 88, -90, 90, 0.1, () -> mode.is(Mode.Bounce)));
    private final BooleanSetting instantFly = add(new BooleanSetting("AutoStart", true, () -> !mode.is(Mode.Bounce)));
    private final BooleanSetting checkSpeed = add(new BooleanSetting("CheckSpeed", false, () -> !mode.is(Mode.Bounce)));
    public final SliderSetting minSpeed = add(new SliderSetting("MinSpeed", 70.0, 0.1, 200.0, () -> !mode.is(Mode.Bounce)));
    private final SliderSetting delay =
            add(new SliderSetting("Delay", 1000, 0, 20000, 50, () -> !mode.is(Mode.Bounce)));
    private final SliderSetting timeout = add(new SliderSetting("Timeout", 0, 0.1, 1, 0.1, () -> !mode.is(Mode.Bounce)));
    private final SliderSetting sneakDownSpeed = add(new SliderSetting("DownSpeed", 1.0, 0.1, 10.0, () -> mode.getValue() == Mode.Control));
    private final SliderSetting boost = add(new SliderSetting("Boost", 1, 0.1, 4, () -> mode.getValue() == Mode.Boost));
    private final BooleanSetting freeze = add(new BooleanSetting("Freeze", false, () -> mode.is(Mode.Rotation)));
    private final BooleanSetting motionStop = add(new BooleanSetting("MotionStop", false, () -> mode.is(Mode.Rotation)));
    private final SliderSetting infiniteMaxSpeed = add(new SliderSetting("InfiniteMaxSpeed", 150f, 50f, 170f, () -> mode.getValue() == Mode.Pitch));
    private final SliderSetting infiniteMinSpeed = add(new SliderSetting("InfiniteMinSpeed", 25f, 10f, 70f, () -> mode.getValue() == Mode.Pitch));
    private final SliderSetting infiniteMaxHeight = add(new SliderSetting("InfiniteMaxHeight", 200, -50, 360, () -> mode.getValue() == Mode.Pitch));
    public final BooleanSetting releaseSneak = add(new BooleanSetting("ReleaseSneak", false));
    private final Timer instantFlyTimer = new Timer();

    boolean prev;
    float prePitch;
    private boolean hasElytra = false;

    public ElytraFly() {
        super("ElytraFly", Category.Movement);
        setChinese("鞘翅飞行");
        INSTANCE = this;
        Alien.EVENT_BUS.subscribe(new FireWorkTweak());
    }

    public void off() {
        if (inventory.getValue() && !EntityUtil.inInventory()) return;
        if (onlyOne.getValue()) {
            for (Entity entity : Alien.THREAD.getEntities()) {
                if (entity instanceof FireworkRocketEntity fireworkRocketEntity) {
                    if (((IFireworkRocketEntity) fireworkRocketEntity).getShooter() == mc.player) {
                        return;
                    }
                }
            }
        }
        ElytraFly.INSTANCE.fireworkTimer.reset();
        int firework;
        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            if (packetInteract.getValue()) {
                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            } else {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        } else if (inventory.getValue() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            if (packetInteract.getValue()) {
                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            } else {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(firework);
            if (packetInteract.getValue()) {
                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            } else {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
            InventoryUtil.switchToSlot(old);
        }
    }

    public static boolean recastElytra(ClientPlayerEntity player) {
        if (checkConditions(player) && ignoreGround(player)) {
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if (INSTANCE.setFlag.getValue()) mc.player.startGliding();
            return true;
        } else return false;
    }

    public static boolean checkConditions(ClientPlayerEntity player) {
        ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
        return !player.getAbilities().flying && !player.hasVehicle() && !player.isClimbing() && itemStack.isOf(Items.ELYTRA) && itemStack.getDamage() < itemStack.getMaxDamage() - 1;
    }

    private static boolean ignoreGround(ClientPlayerEntity player) {
        if (!player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
            ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
            if (itemStack.isOf(Items.ELYTRA) && itemStack.getDamage() < itemStack.getMaxDamage() - 1) {
                player.startGliding();
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public String getInfo() {
        return mode.getValue().name();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        hasElytra = false;
        yaw = mc.player.getYaw();
        rotationPitch = mc.player.getPitch();
    }

    @Override
    public void onDisable() {
        if (ElytraFly.nullCheck()) {
            return;
        }
        if (this.releaseSneak.getValue()) {
            mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)ElytraFly.mc.player, ClientCommandC2SPacket.Mode.STOP_SLEEPING));
        }
    }

    private void boost() {
        if (hasElytra) {
            if (!isGliding()) {
                return;
            }
            float yaw = (float) Math.toRadians(mc.player.getYaw());
            if (mc.options.forwardKey.isPressed()) {
                mc.player.addVelocity(-MathHelper.sin(yaw) * boost.getValueFloat() / 10, 0, MathHelper.cos(yaw) * boost.getValueFloat() / 10);
            }
        }
    }

    float yaw = 0;
    float rotationPitch = 0;

    @EventListener(priority = -9999)
    public void onRotation(UpdateRotateEvent event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Rotation)) {
            if (isGliding()) {
                if (MovementUtil.isMoving()) {
                    if (mc.options.jumpKey.isPressed()) {
                        rotationPitch = (-45);
                    } else if (mc.options.sneakKey.isPressed()) {
                        rotationPitch = (45);
                    } else {
                        rotationPitch = (-1.9f);
                        if (motionStop.getValue()) {
                            setY(0);
                        }
                    }
                } else {
                    if (mc.options.jumpKey.isPressed()) {
                        rotationPitch = (-89);
                    } else if (mc.options.sneakKey.isPressed()) {
                        rotationPitch = (89);
                    } else {
                        if (motionStop.getValue()) {
                            setY(0);
                        }
                    }
                }
                if (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) {
                    yaw = Sprint.getSprintYaw(mc.player.getYaw());
                } else if (motionStop.getValue()) {
                    setX(0);
                    setZ(0);
                }
                event.setYaw(yaw);
                event.setPitch(rotationPitch);
            }
        } else if (mode.is(Mode.Pitch)) {
            if (isGliding()) {
                event.setPitch(infinitePitch);
            }
        } else if (mode.is(Mode.Bounce)) {
            if (isGliding()) {
                event.setPitch(pitch.getValueFloat());
            }
        }
    }

    boolean flying = false;
    int packetDelayInt = 0;

    @EventListener
    public void onUpdate(UpdateEvent event) {
        getInfinitePitch();
        flying = false;
        if (packet.getValue()) {
            hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
        } else {
            hasElytra = false;
            for (net.minecraft.entity.EquipmentSlot slot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET}) {
                ItemStack is = ElytraFly.mc.player.getEquippedStack(slot);
                if (!(is.isOf(Items.ELYTRA))) continue;
                this.hasElytra = true;
                break;
            }
            if (infiniteDura.getValue()) {
                if (!mc.player.isOnGround() && hasElytra) {
                    flying = true;
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    if (setFlag.getValue()) mc.player.startGliding();
                }
            }
            if (mode.is(Mode.Bounce)) {
                ((ILivingEntity) mc.player).setLastJumpCooldown(0);
                return;
            }
        }
        double x = mc.player.getX() - mc.player.lastX;
        double y = mc.player.getY() - mc.player.lastY;
        double z = mc.player.getZ() - mc.player.lastZ;
        double dist = Math.sqrt(x * x + z * z + y * y) / 1000.0;
        double div = 0.05 / 3600.0;
        float timer = Alien.TIMER.get();
        final double speed = dist / div * timer;
        if (mode.getValue() == Mode.Boost) {
            boost();
        }
        if (packet.getValue()) {
            if (mc.player.isOnGround()) return;
            packetDelayInt++;
            if (packetDelayInt <= packetDelay.getValue()) return;
            int elytra = InventoryUtil.findItem(Items.ELYTRA);
            if (elytra != -1) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                mc.player.startGliding();
                if ((!checkSpeed.getValue() || speed <= minSpeed.getValue()) && firework.getValue() && fireworkTimer.passed(delay.getValueInt()) && (MovementUtil.isMoving() || mode.is(Mode.Rotation) && (mc.options.jumpKey.isPressed())) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isGliding()) {
                    off();
                    fireworkTimer.reset();
                }
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, mc.player);
                packetDelayInt = 0;
            } else {
                elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
                if (elytra != -1) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    mc.player.startGliding();
                    if ((!checkSpeed.getValue() || speed <= minSpeed.getValue()) && firework.getValue() && fireworkTimer.passed(delay.getValueInt()) && (MovementUtil.isMoving() || mode.is(Mode.Rotation) && (mc.options.jumpKey.isPressed())) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isGliding()) {
                        off();
                        fireworkTimer.reset();
                    }
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                    packetDelayInt = 0;
                }
            }
            return;
        }
        if ((!checkSpeed.getValue() || speed <= minSpeed.getValue()) && firework.getValue() && fireworkTimer.passed(delay.getValueInt()) && (MovementUtil.isMoving() || mode.is(Mode.Rotation) && (mc.options.jumpKey.isPressed())) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isGliding()) {
            off();
            fireworkTimer.reset();
        }
        if (!isGliding() && hasElytra) {
            fireworkTimer.setMs(99999999);
            if (!mc.player.isOnGround() && instantFly.getValue() && mc.player.getVelocity().getY() < 0D && !infiniteDura.getValue()) {
                if (!instantFlyTimer.passed((long) (1000 * timeout.getValue()))) return;
                instantFlyTimer.reset();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                if (setFlag.getValue()) mc.player.startGliding();
            }
        }
    }

    protected final Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public Vec3d getRotationVec(float tickDelta) {
        return this.getRotationVector(-upPitch.getValueFloat(), mc.player.getYaw(tickDelta));
    }

    @EventListener
    private void onPlayerMove(MoveEvent event) {
        if (autoStop.getValue()) {
            if (isGliding()) {
                int chunkX = (int) ((mc.player.getX()) / 16);
                int chunkZ = (int) ((mc.player.getZ()) / 16);
                if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    event.cancel();
                }
            }
        }
    }

    @EventListener
    private void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Bounce) && hasElytra) {
            if (autoJump.getValue()) mc.options.jumpKey.setPressed(true);
            if (event.isPost()) {
                if (!isGliding())
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

                if (checkConditions(mc.player)) {
                    if (!sprint.getValue()) {
                        // Sprinting all the time (when not on ground) makes it rubberband on certain anticheats.
                        if (isGliding()) mc.player.setSprinting(mc.player.isOnGround());
                        else mc.player.setSprinting(true);
                    }
                }
            } else {
                if (checkConditions(mc.player) && sprint.getValue()) mc.player.setSprinting(true);
            }
        }
    }

    @EventListener
    private void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Bounce) && hasElytra && event.getPacket() instanceof ClientCommandC2SPacket && ((ClientCommandC2SPacket) event.getPacket()).getMode().equals(ClientCommandC2SPacket.Mode.START_FALL_FLYING) && !sprint.getValue()) {
            mc.player.setSprinting(true);
        }
    }

    @EventListener
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Bounce) && hasElytra && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            mc.player.stopGliding();
        }
    }

    @EventListener
    public void travel(TravelEvent event) {
        if (nullCheck()) return;
        if (!AntiCheat.INSTANCE.movementSync()) {
            if (mode.is(Mode.Bounce) && hasElytra) {
                if (event.isPre()) {
                    prev = true;
                    prePitch = mc.player.getPitch();
                    mc.player.setPitch(pitch.getValueFloat());
                } else {
                    if (prev) {
                        prev = false;
                        mc.player.setPitch(prePitch);
                    }
                }
            } else if (mode.is(Mode.Pitch) && isGliding()) {
                if (event.isPre()) {
                    prev = true;
                    prePitch = mc.player.getPitch();
                    mc.player.setPitch(lastInfinitePitch);
                } else {
                    if (prev) {
                        prev = false;
                        mc.player.setPitch(prePitch);
                    }
                }
            }
        }
    }

    @EventListener
    public void onMove(TravelEvent event) {
        if (nullCheck() || !hasElytra || !isGliding() || event.isPost()) return;
        if (mode.is(Mode.Freeze) || mode.is(Mode.Rotation) && freeze.getValue()) {
            if (!MovementUtil.isMoving() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                event.cancel();
                return;
            }
        }
        if (mode.getValue() == Mode.Control) {
            if (firework.getValue()) {
                if (!(mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed())) {
                    if (mc.options.sneakKey.isPressed()) {
                        setY(-sneakDownSpeed.getValue());
                    } else if (mc.options.jumpKey.isPressed()) {
                        setY(upFactor.getValue());
                    } else {
                        setY(-0.00000000003D * downFactor.getValue());
                    }
                } else {
                    setY(0);
                }
                double[] dir = directionSpeed(speed.getValue());
                setX(dir[0]);
                setZ(dir[1]);
            } else {
                Vec3d lookVec = getRotationVec(mc.getRenderTickCounter().getDynamicDeltaTicks());
                double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                double motionDist = Math.sqrt(getX() * getX() + getZ() * getZ());
                if (mc.options.sneakKey.isPressed()) {
                    setY(-sneakDownSpeed.getValue());
                } else if (!mc.options.jumpKey.isPressed()) {
                    setY(-0.00000000003D * downFactor.getValue());
                }
                if (mc.options.jumpKey.isPressed()) {
                    if (motionDist > upFactor.getValue() / upFactor.getMax()) {
                        double rawUpSpeed = motionDist * 0.01325D;
                        setY(getY() + rawUpSpeed * 3.2D);
                        setX(getX() - lookVec.x * rawUpSpeed / lookDist);
                        setZ(getZ() - lookVec.z * rawUpSpeed / lookDist);
                    } else {
                        double[] dir = directionSpeed(speed.getValue());
                        setX(dir[0]);
                        setZ(dir[1]);
                    }
                }
                if (lookDist > 0.0D) {
                    setX(getX() + (lookVec.x / lookDist * motionDist - getX()) * 0.1D);
                    setZ(getZ() + (lookVec.z / lookDist * motionDist - getZ()) * 0.1D);
                }
                if (!mc.options.jumpKey.isPressed()) {
                    double[] dir = directionSpeed(speed.getValue());
                    setX(dir[0]);
                    setZ(dir[1]);
                }
                if (!noDrag.getValue()) {
                    setY(getY() * 0.9900000095367432D);
                    setX(getX() * 0.9800000190734863D);
                    setZ(getZ() * 0.9900000095367432D);
                }
                double finalDist = Math.sqrt(getX() * getX() + getZ() * getZ());
                if (speedLimit.getValue() && finalDist > maxSpeed.getValue()) {
                    setX(getX() * maxSpeed.getValue() / finalDist);
                    setZ(getZ() * maxSpeed.getValue() / finalDist);
                }
                event.cancel();
                mc.player.move(MovementType.SELF, mc.player.getVelocity());
            }
        }
    }

    private double getX() {
        return getMotionX();
    }

    private void setX(double f) {
        setMotionX(f);
    }

    private double getY() {
        return getMotionY();
    }

    private void setY(double f) {
        setMotionY(f);
    }

    private double getZ() {
        return getMotionZ();
    }

    private void setZ(double f) {
        setMotionZ(f);
    }

    public enum Mode {
        Control, Boost, Bounce, Freeze, None, Rotation, Pitch
    }

    public class FireWorkTweak {

        boolean press;

        @EventListener
        public void onTick(ClientTickEvent event) {
            if (nullCheck() || event.isPost()) return;
            if (inventory.getValue() && !EntityUtil.inInventory()) return;
            if (mc.currentScreen == null) {
                if (fireWork.isPressed()) {
                    if (!press) {
                        if (fireworkTimer.passed(delay.getValueInt()) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isGliding()) {
                            off();
                            fireworkTimer.reset();
                        }
                    }
                    press = true;
                } else {
                    press = false;
                }
            } else {
                press = false;
            }
        }
    }

    private boolean down;
    private float lastInfinitePitch;
    private float infinitePitch;

    private void getInfinitePitch() {
        lastInfinitePitch = infinitePitch;
        double currentPlayerSpeed = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ);
        if (mc.player.getY() < infiniteMaxHeight.getValue()) {
            if (currentPlayerSpeed * 72f < infiniteMinSpeed.getValue() && !down)
                down = true;
            if (currentPlayerSpeed * 72f > infiniteMaxSpeed.getValue() && down)
                down = false;
        } else down = true;

        if (down) infinitePitch += 3;
        else infinitePitch -= 3;

        infinitePitch = MathUtil.clamp(infinitePitch, -40, 40);
    }

    public boolean isGliding() {
        return mc.player.getPose() == EntityPose.GLIDING || packet.getValue() && hasElytra && !mc.player.isOnGround() || flying;
    }
}
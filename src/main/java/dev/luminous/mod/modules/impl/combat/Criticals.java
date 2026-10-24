package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IPlayerInteractEntityC2SPacket;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.impl.exploit.Phase;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;

import static dev.luminous.api.utils.player.EntityUtil.getPlayerPos;

public class Criticals extends Module {
    public static Criticals INSTANCE;
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.OldNCP));
    public final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", true, () -> !mode.is(Mode.Ground)));
    private final BooleanSetting setOnGround = add(new BooleanSetting("SetNoGround", false, () -> mode.is(Mode.Ground)));
    private final BooleanSetting blockCheck = add(new BooleanSetting("BlockCheck", true, () -> mode.is(Mode.Ground)));
    private final BooleanSetting autoJump = add(new BooleanSetting("AutoJump", true, () -> mode.is(Mode.Ground)).setParent());
    private final BooleanSetting mini = add(new BooleanSetting("Mini", true, () -> mode.is(Mode.Ground) && autoJump.isOpen()));
    private final SliderSetting y = add(new SliderSetting("MotionY", 0.05, 0, 1, 0.0000000001, () -> mode.is(Mode.Ground) && autoJump.isOpen()));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true, () -> mode.is(Mode.Ground)));
    private final BooleanSetting crawlingDisable = add(new BooleanSetting("CrawlingDisable", true, () -> mode.is(Mode.Ground)));
    private final BooleanSetting flight = add(new BooleanSetting("Flight", false, () -> mode.is(Mode.Ground)));

    public Criticals() {
        super("Criticals", Category.Combat);
        setChinese("刀刀暴击");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return mode.getValue().name();
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (mode.is(Mode.Ground)) {
            if (BowBomb.send) return;
            if (AutoPearl.throwing || Phase.INSTANCE.isOn()) return;
            if (!setOnGround.getValue()) {
                return;
            }
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                ((IPlayerMoveC2SPacket) event.getPacket()).setOnGround(false);
            }
            return;
        }
        Entity entity;
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && getInteractType(packet) == PlayerInteractEntityC2SPacket.InteractType.ATTACK && !((entity = getEntity(packet)) instanceof EndCrystalEntity)) {
            if ((!onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying) && !mc.player.isInLava() && !mc.player.isTouchingWater() && entity != null) {
                doCrit(entity);
            }
        }
    }

    boolean requireJump = false;

    @Override
    public void onLogout() {
        if (mode.is(Mode.Ground)) {
            if (autoDisable.getValue())
                disable();
        }
    }

    @Override
    public void onEnable() {
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        requireJump = true;
        if (mode.is(Mode.Ground)) {
            if (nullCheck()) {
                if (autoDisable.getValue()) {
                    disable();
                }
            } else if (MovementUtil.isMoving() && autoDisable.getValue()) {
                disable();
            } else if (crawlingDisable.getValue() && mc.player.isCrawling()) {
                disable();
            } else if (mc.player.isOnGround() && autoJump.getValue() && (!blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(getPlayerPos(true).up(2))))) {
                jump();
            }
        }
    }

    public void jump() {
        if (mini.getValue()) {
            MovementUtil.setMotionY(y.getValue());
        } else {
            mc.player.jump();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        if (mode.is(Mode.Ground)) {
            if (crawlingDisable.getValue() && mc.player.isCrawling()) {
                disable();
            } else if (MovementUtil.isMoving() && autoDisable.getValue()) {
                disable();
            } else if (flight.getValue() && mc.player.fallDistance > 0) {
                MovementUtil.setMotionY(0.0);
                MovementUtil.setMotionX(0.0);
                MovementUtil.setMotionZ(0.0);
                requireJump = false;
            } else if (blockCheck.getValue() && !BlockUtil.canCollide(mc.player, new Box(getPlayerPos(true).up(2)))) {
                requireJump = true;
            } else if (mc.player.isOnGround() && autoJump.getValue() && (flight.getValue() || requireJump)) {
                jump();
                requireJump = false;
            }
        }
    }

    public void doCrit(Entity entity) {
        switch (mode.getValue()) {
            case BBTT -> {
                if (MovementUtil.isMoving() || !MovementUtil.isStatic()) return;
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.045, mc.player.getZ(), false,false));
            }
            case Strict -> {
                mc.player.addCritParticles(entity);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false,false));
            }
            case NCP -> {
                mc.player.addCritParticles(entity);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625D, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false,false));
            }
            case OldNCP -> {
                mc.player.addCritParticles(entity);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00001058293536, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00000916580235, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.00000010371854, mc.player.getZ(), false,false));
            }
            case UpdatedNCP -> {
                mc.player.addCritParticles(entity);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000271875, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false,false));
            }
            case Hypixel2K22 -> {
                mc.player.addCritParticles(entity);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0045, mc.player.getZ(), true,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000152121, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.025, mc.player.getZ(), false,false));
            }
            case Packet -> {
                mc.player.addCritParticles(entity);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0005, mc.player.getZ(), false,false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0001, mc.player.getZ(), false,false));
            }
        }
    }

    public enum Mode {
        UpdatedNCP, Strict, NCP, OldNCP, Hypixel2K22, Packet, Ground, BBTT
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        return Criticals.mc.world == null ? null : Criticals.mc.world.getEntityById(((IPlayerInteractEntityC2SPacket)packet).getEntityId());
    }

    public static PlayerInteractEntityC2SPacket.InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        return packet.type.getType();
    }
}
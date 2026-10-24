package dev.luminous.api.utils.combat;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class CombatUtil implements Wrapper {
    public static final Timer breakTimer = new Timer();
    public static boolean terrainIgnore = false;
    public static BlockPos modifyPos;
    public static BlockState modifyBlockState = Blocks.AIR.getDefaultState();

    public static List<PlayerEntity> getEnemies(double range) {
        List<PlayerEntity> list = new ArrayList<>();
        for (AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
            if (!isValid(player, range)) continue;
            list.add(player);
        }
        return list;
    }

    public static void attackCrystal(BlockPos pos, boolean rotate, boolean eatingPause) {
        attackCrystal(new Box(pos), rotate, eatingPause);
    }

    public static void attackCrystal(Box box, boolean rotate, boolean eatingPause) {
        for (EndCrystalEntity entity : BlockUtil.getEndCrystals(box)) {
            attackWithDelay(entity, rotate, eatingPause);
        }
    }

    public static void attackWithDelay(Entity entity, boolean rotate, boolean usingPause) {
        if (!CombatUtil.breakTimer.passed((long) (AntiCheat.INSTANCE.attackDelay.getValue() * 1000))) return;
        if (usingPause && mc.player.isUsingItem())
            return;
        attack(entity, rotate);
    }

    public static void attack(Entity entity, boolean rotate) {
        if (entity != null) {
            Vec3d attackVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
            if (mc.player.getEyePos().distanceTo(attackVec) > AntiCheat.INSTANCE.ieRange.getValue()) return;
            CombatUtil.breakTimer.reset();
            if (rotate && AntiCheat.INSTANCE.attackRotate.getValue())
                Alien.ROTATION.lookAt(attackVec);
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.player.resetTicksSinceLastAttack();
            EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.attackSwing.getValue());
            if (rotate && AntiCheat.INSTANCE.attackRotate.getValue()) {
                Alien.ROTATION.snapBack();
            }
        }
    }

    public static boolean isntValid(Entity entity, double range) {
        return !isValid(entity, range);
    }

    public static boolean isValid(Entity entity, double range) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof PlayerEntity player && Alien.FRIEND.isFriend(player) || mc.player.getEntityPos().distanceTo(entity.getEntityPos()) > range;

        return !invalid;
    }

    public static boolean isValid(Entity entity) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof PlayerEntity player && Alien.FRIEND.isFriend(player);

        return !invalid;
    }

    public static PlayerEntity getClosestEnemy(double distance) {
        PlayerEntity closest = null;

        for (PlayerEntity player : getEnemies(distance)) {
            if (closest == null) {
                closest = player;
                continue;
            }

            if (!(mc.player.squaredDistanceTo(player.getEntityPos()) < mc.player.squaredDistanceTo(closest))) continue;

            closest = player;
        }
        return closest;
    }
}

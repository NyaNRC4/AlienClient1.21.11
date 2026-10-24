package dev.luminous.api.utils.math;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PredictUtil implements Wrapper {

    public static Vec3d getPos(PlayerEntity entity, int ticks) {
        if (ticks <= 0) {
            return entity.getEntityPos();
        }
        return getPos(entity, AntiCheat.INSTANCE.maxMotionY.getValue(), AntiCheat.INSTANCE.predictTicks.getValueInt(), AntiCheat.INSTANCE.simulation.getValueInt(), AntiCheat.INSTANCE.step.getValue(), AntiCheat.INSTANCE.doubleStep.getValue(), AntiCheat.INSTANCE.jump.getValue(), AntiCheat.INSTANCE.inBlockPause.getValue());
    }

    public static Vec3d getPos(PlayerEntity e, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause) {
        if (inBlockPause && BlockUtil.canCollide(e, e.getBoundingBox())) {
            return e.getEntityPos();
        }
        double velocityX;
        double velocityY;
        double velocityZ;

        if (AntiCheat.INSTANCE.motion.is(AntiCheat.Motion.Position)) {
            velocityX = e.getX() - e.lastX;
            velocityY = e.getY() - e.lastY;
            velocityZ = e.getZ() - e.lastZ;
            if (velocityY > maxMotionY) {
                velocityY = maxMotionY;
            }
        } else {
            velocityX = e.getVelocity().x;
            velocityY = e.getVelocity().y;
            velocityZ = e.getVelocity().z;
        }
        double motionX = velocityX;
        double motionY = velocityY;
        double motionZ = velocityZ;

        double x = e.getX();
        double y = e.getY();
        double z = e.getZ();
        Vec3d lastPos = new Vec3d(x, y, z);
        if (motionX == 0 && motionY == 0 && motionZ == 0) return lastPos;
        for (int i = 0; i < ticks; i++) {
            lastPos = new Vec3d(x, y, z);

            boolean move = false;
            boolean fall = false;

            start:
            for (int yTime = simulation; yTime >= 0; yTime--) {
                for (int xTime = simulation; xTime >= 0; xTime--) {
                    double xFactor = ((double) xTime / simulation);
                    double yFactor = ((double) yTime / simulation);
                    if (canMove(lastPos.add(motionX * xFactor, motionY * yFactor, motionZ * xFactor), e)) {

                        if (Math.abs(motionX * xFactor) + Math.abs(motionZ * xFactor) + Math.abs(motionY * yFactor) <= 0.05) {
                            if (step && !canMove(lastPos.add(velocityX, 0, velocityZ), e) && canMove(lastPos.add(velocityX, 1.1, velocityZ), e)) {
                                y += 1.05;
                                motionY = 0.03;
                                for (int yTime2 = simulation; yTime2 >= 0; yTime2--) {
                                    for (int xTime2 = simulation; xTime2 >= 0; xTime2--) {
                                        double xFactor2 = ((double) xTime2 / simulation);
                                        double yFactor2 = ((double) yTime2 / simulation);
                                        if (canMove(lastPos.add(motionX * xFactor2, motionY * yFactor2, motionZ * xFactor2), e)) {
                                            move = true;
                                            x += motionX * xFactor2;
                                            z += motionZ * xFactor2;
                                            if (yTime2 > 0) {
                                                y += motionY * yFactor2;
                                                fall = true;
                                            }
                                            break start;
                                        }
                                    }
                                }
                            } else if (doubleStep && !canMove(lastPos.add(velocityX, 0, velocityZ), e) && canMove(lastPos.add(velocityX, 2.1, velocityZ), e)) {
                                y += 2.05;
                                motionY = 0.03;
                                for (int yTime2 = simulation; yTime2 >= 0; yTime2--) {
                                    for (int xTime2 = simulation; xTime2 >= 0; xTime2--) {
                                        double xFactor2 = ((double) xTime2 / simulation);
                                        double yFactor2 = ((double) yTime2 / simulation);
                                        if (canMove(lastPos.add(motionX * xFactor2, motionY * yFactor2, motionZ * xFactor2), e)) {
                                            move = true;
                                            x += motionX * xFactor2;
                                            z += motionZ * xFactor2;
                                            if (yTime2 > 0) {
                                                y += motionY * yFactor2;
                                                fall = true;
                                            }
                                            break start;
                                        }
                                    }
                                }
                            }
                            return lastPos;
                        }

                        move = true;
                        x += motionX * xFactor;
                        z += motionZ * xFactor;
                        if (yTime > 0) {
                            y += motionY * yFactor;
                            fall = true;
                        }

                        break start;
                    }
                }
            }

            if (!move) {
                return lastPos;
            }

            if (!e.isGliding()) {
                motionX *= 0.99;
                motionZ *= 0.99;
                motionY *= 0.99;
                motionY -= 0.05000000074505806;
            }

            if (!fall) {
                if (e.isOnGround()) {
                    motionX = velocityX;
                    motionZ = velocityZ;
                    motionY = 0;
                } else if (jump) {
                    motionX = velocityX;
                    motionZ = velocityZ;
                    motionY = 0.333;
                } else {
                    motionY = 0;
                }
            }
        }
        return lastPos;
    }

    public static boolean canMove(Vec3d pos, PlayerEntity player) {
        return !BlockUtil.canCollide(player, ((IEntity) player).getDimensions().getBoxAt(pos)) || new Box(new BlockPosX(pos)).intersects(player.getBoundingBox());
    }
}

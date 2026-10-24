package dev.luminous.api.utils.math;

import dev.luminous.api.utils.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil implements Wrapper {
    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static float interpolateAngle(float prev, float current, float delta) {
        float diff = ((current - prev + 180.0f) % 360.0f) - 180.0f;
        return prev + diff * delta;
    }

    public static double interpolateAngle(double prev, double current, double delta) {
        double diff = ((current - prev + 180.0) % 360.0) - 180.0;
        return prev + diff * delta;
    }

    public static double round(double value, int places) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double square(double input) {
        return input * input;
    }

    public static float random(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public static double random(double min, double max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public static float rad(float angle) {
        return (float) (angle * Math.PI / 180);
    }

    public static double interpolate(double previous, double current, double delta) {
        return previous + (current - previous) * delta;
    }

    public static float interpolate(float previous, float current, float delta) {
        return previous + (current - previous) * delta;
    }

    public static Direction getFacingOrder(float yaw, float pitch) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.sin(f);
        float i = MathHelper.cos(f);
        float j = MathHelper.sin(g);
        float k = MathHelper.cos(g);
        boolean bl = j > 0.0F;
        boolean bl2 = h < 0.0F;
        boolean bl3 = k > 0.0F;
        float l = bl ? j : -j;
        float m = bl2 ? -h : h;
        float n = bl3 ? k : -k;
        float o = l * i;
        float p = n * i;
        Direction direction = bl ? Direction.EAST : Direction.WEST;
        Direction direction2 = bl2 ? Direction.UP : Direction.DOWN;
        Direction direction3 = bl3 ? Direction.SOUTH : Direction.NORTH;
        if (l > n) {
            if (m > o) {
                return direction2;
            } else {
                return direction;
            }
        } else if (m > p) {
            return direction2;
        } else {
            return direction3;
        }
    }

    public static Direction getDirectionFromEntityLiving(BlockPos pos, LivingEntity entity) {
        if (Math.abs(entity.getX() - ((double) pos.getX() + 0.5)) < 2.0 && Math.abs(entity.getZ() - ((double) pos.getZ() + 0.5)) < 2.0) {
            double d0 = entity.getY() + (double) entity.getEyeHeight(entity.getPose());
            if (d0 - (double) pos.getY() > 2.0) {
                return Direction.UP;
            }

            if ((double) pos.getY() - d0 > 0.0) {
                return Direction.DOWN;
            }
        }

        return entity.getHorizontalFacing().getOpposite();
    }

    public static Vec3d getRenderPosition(Entity entity) {
        return getRenderPosition(entity, mc.getRenderTickCounter().getDynamicDeltaTicks());
    }

    public static Vec3d getRenderPosition(Entity entity, float tickDelta) {
        return new Vec3d(entity.lastX + (entity.getX() - entity.lastX) * tickDelta,
                entity.lastY + (entity.getY() - entity.lastY) * tickDelta,
                entity.lastZ + (entity.getZ() - entity.lastZ) * tickDelta);
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

/*    public static Vec3d getPointToBoxFromBottom(Vec3d eye, Vec3d bottom, double range, double boxSize) {
        double halfWidth = boxSize / 2;

        double closestX = Math.max(bottom.x - halfWidth, Math.min(eye.x, bottom.x + halfWidth));
        double closestZ = Math.max(bottom.z - halfWidth, Math.min(eye.z, bottom.z + halfWidth));

        double dx = closestX - eye.x;
        double dz = closestZ - eye.z;

        double dxzSq = dx * dx + dz * dz;

        double rangeSq = range * range;

        if (dxzSq > rangeSq) {
            return null;
        }

        double maxYSq = rangeSq - dxzSq;

        double maxYOffset = Math.sqrt(maxYSq);
        double yGap = bottom.y - eye.y;

        if (yGap > 0) {
            double distanceSqAt0 = dxzSq + yGap * yGap;
            if (distanceSqAt0 <= rangeSq) {
                return new Vec3d(closestX, bottom.y, closestZ);
            }
            return null;
        } else {
            double lowerBound = -yGap - maxYOffset;
            double upperBound = -yGap + maxYOffset;
            double upperLimitYOffset = Math.min(boxSize, eye.y - bottom.y);

            double yMin = Math.max(0, lowerBound);
            double yMax = Math.min(upperBound, upperLimitYOffset);

            if (yMin > yMax) {
                return null;
            }

            if (yMin < 0 || yMin > boxSize) {
                return null;
            }

            double y = bottom.y + yMin;
            return new Vec3d(closestX, y, closestZ);
        }
    }*/

    public static Vec3d getPointToBoxFromBottom(Vec3d eye, Vec3d bottom, double range, double boxSize, double step) {
        Vec3d target = null;
        double halfWidth = boxSize / 2;

        double minOffset = Double.MAX_VALUE;
        boolean xPlus = bottom.x < eye.x;
        boolean zPlus = bottom.z < eye.z;

        double rangeSq = range * range;

        for (double yOffset = 0; yOffset <= boxSize; yOffset += step) {
            for (double xOffset = 0; xOffset <= halfWidth; xOffset += step) {
                for (double zOffset = 0; zOffset <= halfWidth; zOffset += step) {
                    double y = bottom.y + yOffset;
                    if (yOffset != 0 && y > eye.y) return target;
                    double x = bottom.x + (xPlus ? xOffset : -xOffset);
                    double z = bottom.z + (zPlus ? zOffset : -zOffset);

                    double dxToEye = x - eye.x;
                    double dyToEye = y - eye.y;
                    double dzToEye = z - eye.z;

                    double distSq = dxToEye * dxToEye + dyToEye * dyToEye + dzToEye * dzToEye;
                    double offsets = xOffset + yOffset + zOffset;
                    if (distSq <= rangeSq && offsets < minOffset) {
                        minOffset = offsets;
                        target = new Vec3d(x, y, z);
                    }
                }
            }
        }
        return target;
    }

    public static Vec3d getClosestPointToBox(Vec3d pos, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double closestX = Math.max(minX, Math.min(pos.x, maxX));
        double closestY = Math.max(minY, Math.min(pos.y, maxY));
        double closestZ = Math.max(minZ, Math.min(pos.z, maxZ));

        return new Vec3d(closestX, closestY, closestZ);
    }

    public static Vec3d getClosestPointToBox(Vec3d eyePos, Box boundingBox) {
        return getClosestPointToBox(eyePos, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
    }

    public static Vec3d getClosestPoint(Entity entity) {
        return getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
    }
}

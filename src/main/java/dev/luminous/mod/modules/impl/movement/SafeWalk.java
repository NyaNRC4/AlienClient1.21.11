package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.util.math.Box;

public class SafeWalk extends Module {
    public static SafeWalk INSTANCE;
    private final BooleanSetting legit = add(new BooleanSetting("Legit", true));
    private final BooleanSetting onlyInsideBlock = add(new BooleanSetting("OnlyInsideBlock", false));

    public SafeWalk() {
        super("SafeWalk", Category.Movement);
        setChinese("边缘行走");
        INSTANCE = this;
    }

    @EventListener(priority=-200)
    public void keyboard(KeyboardInputEvent event) {
        if (SafeWalk.mc.player.isOnGround() && this.legit.getValue() && this.shouldSafeWalk()) {
            if (this.isOffsetBBEmpty(0.3, -1.0, 0.0)) {
                MovementUtil.setSneaking(SafeWalk.mc.player, true);
            } else if (this.isOffsetBBEmpty(0.0, -1.0, 0.3)) {
                MovementUtil.setSneaking(SafeWalk.mc.player, true);
            } else if (this.isOffsetBBEmpty(0.3, -1.0, 0.3)) {
                MovementUtil.setSneaking(SafeWalk.mc.player, true);
            } else if (this.isOffsetBBEmpty(-0.3, -1.0, 0.0)) {
                MovementUtil.setSneaking(SafeWalk.mc.player, true);
            } else if (this.isOffsetBBEmpty(0.0, -1.0, -0.3)) {
                MovementUtil.setSneaking(SafeWalk.mc.player, true);
            } else if (this.isOffsetBBEmpty(-0.3, -1.0, -0.3)) {
                MovementUtil.setSneaking(SafeWalk.mc.player, true);
            }
        }
    }

    @EventListener(priority = EventPriority.LOW)
    public void onMove(MoveEvent event) {
        if (mc.player.isOnGround() && !legit.getValue() && shouldSafeWalk()) {
            double x = event.getX();
            double y = event.getY();
            double z = event.getZ();
            double increment = 0.05;
            while (x != 0.0 && this.isOffsetBBEmpty(x, -1.0, 0.0)) {
                if (x < increment && x >= -increment) {
                    x = 0.0;
                    continue;
                }
                if (x > 0.0) {
                    x -= increment;
                    continue;
                }
                x += increment;
            }
            while (z != 0.0 && this.isOffsetBBEmpty(0.0, -1.0, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0;
                    continue;
                }
                if (z > 0.0) {
                    z -= increment;
                    continue;
                }
                z += increment;
            }
            while (x != 0.0 && z != 0.0 && this.isOffsetBBEmpty(x, -1.0, z)) {
                x = x < increment && x >= -increment ? 0.0 : (x > 0.0 ? x - increment : x + increment);
                if (z < increment && z >= -increment) {
                    z = 0.0;
                    continue;
                }
                if (z > 0.0) {
                    z -= increment;
                    continue;
                }
                z += increment;
            }
            event.setX(x);
            event.setY(y);
            event.setZ(z);
        }
    }

    public boolean shouldSafeWalk() {
        return !onlyInsideBlock.getValue() || EntityUtil.isInsideBlock();
    }

    public boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
        Box playerBox = mc.player.getBoundingBox();
        Box box = new Box(playerBox.minX, playerBox.minY, playerBox.maxZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
        return !BlockUtil.canCollide(mc.player, box.offset(offsetX, offsetY, offsetZ));
    }
}

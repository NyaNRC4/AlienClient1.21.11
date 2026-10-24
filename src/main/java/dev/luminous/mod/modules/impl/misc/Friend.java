package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class Friend extends Module {
    public static Friend INSTANCE;

    public Friend() {
        super("Friend", Category.Misc);
        setChinese("好友");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        HitResult target = mc.crosshairTarget;
        if (target instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
            Alien.FRIEND.friend(player);
        }
        disable();
    }
}
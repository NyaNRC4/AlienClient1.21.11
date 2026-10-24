package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaceBlockEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class AntiRegear extends Module {
    public static AntiRegear INSTANCE;
    public final List<BlockPos> safe = new ArrayList<>();
    private final SliderSetting safeRange =
            add(new SliderSetting("SafeRange", 2, 0, 8, .1));
    private final SliderSetting range =
            add(new SliderSetting("Range", 5, 0, 8, .1));
    private final BooleanSetting checkSelf = add(new BooleanSetting("CheckSelf", true));

    public AntiRegear() {
        super("AntiRegear", Category.Combat);
        setChinese("反补给");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (PacketMine.getBreakPos() != null && mc.world.getBlockState(PacketMine.getBreakPos()).getBlock() instanceof ShulkerBoxBlock) {
            return;
        }
        safe.removeIf(pos -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));
        if (getBlock() != null) {
            PacketMine.INSTANCE.mine(getBlock().getPos());
        }
    }

    @EventListener
    public void onPlace(PlaceBlockEvent event) {
        if (event.block instanceof ShulkerBoxBlock) {
            safe.add(event.blockPos);
        }
    }

    private ShulkerBoxBlockEntity getBlock() {
        for (BlockEntity entity : BlockUtil.getTileEntities()) {
            if (entity instanceof ShulkerBoxBlockEntity shulker) {
                if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(shulker.getPos().toCenterPos())) <= safeRange.getValue()) {
                    continue;
                }
                if (checkSelf.getValue() && (safe.contains(shulker.getPos()) || shulker.getPos().equals(AutoRegear.INSTANCE.placePos) && !AutoRegear.INSTANCE.timeoutTimer.passed(100))) {
                    continue;
                }
                if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(shulker.getPos().toCenterPos())) <= range.getValue()) {
                    return shulker;
                }
            }
        }
        return null;
    }
}
package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Nuker extends Module {
    private final SliderSetting range =
            add(new SliderSetting("Range", 4, 0, 8, .1));
    private final BooleanSetting down =
            add(new BooleanSetting("Down", false));
    private final BooleanSetting sand =
            add(new BooleanSetting("Sand", false));
    private final SliderSetting breaks =
            add(new SliderSetting("Breaks", 10, 0, 20, sand::getValue));

    public Nuker() {
        super("Nuker", Category.Misc);
        setChinese("范围挖掘");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (PacketMine.getBreakPos() != null && !mc.world.isAir(PacketMine.getBreakPos())) {
            return;
        }

        if (sand.getValue()) {
            if (!mc.player.isOnGround()) return;
            int b = 0;
            for (BlockPos sand : BlockUtil.getSphere(range.getValueFloat(), mc.player.getEyePos())) {
                if (Blocks.SAND != mc.world.getBlockState(sand).getBlock() && Blocks.RED_SAND != mc.world.getBlockState(sand).getBlock())
                    continue;
                Direction side = BlockUtil.getClickSideStrict(sand);
                if (side == null) continue;
                PlaceRender.INSTANCE.create(sand);
                Alien.ROTATION.snapAt(sand.toCenterPos());
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, sand, side, id));
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, sand, side, id));
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, sand, side, id));
                Alien.ROTATION.snapBack();
                b++;
                if (b >= breaks.getValue()) {
                    return;
                }
            }
        } else {
            BlockPos pos = getBlock();
            if (pos != null) {
                PacketMine.INSTANCE.mine(pos);
            }
        }
    }

    private BlockPos getBlock() {
        BlockPos down = null;
        for (BlockPos pos : BlockUtil.getSphere(range.getValueFloat(), mc.player.getEyePos())) {
            if (mc.world.isAir(pos)) continue;
            if (PacketMine.unbreakable(pos)) continue;
            if (BlockUtil.getClickSideStrict(pos) == null) continue;
            if (pos.getY() < mc.player.getY()) {
                if (down == null && this.down.getValue()) {
                    down = pos;
                }
                continue;
            }
            return pos;
        }
        return down;
    }
}
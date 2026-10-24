package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AntiCrawl extends Module {
    public static AntiCrawl INSTANCE;
    final double[] xzOffset = new double[]{0, 0.3, -0.3};

    private final EnumSetting<While> whileSetting = add(new EnumSetting<>("While", While.Crawling));
    private final BooleanSetting web = add(new BooleanSetting("Web", true));

    public boolean work = false;

    public AntiCrawl() {
        super("AntiCrawl", Category.Combat);
        setChinese("反趴下");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        work = false;
        if (mc.player.isGliding()) return;
        if (whileSetting.is(While.Always) && BlockUtil.getBlock(mc.player.getBlockPos()) != Blocks.BEDROCK || mc.player.isCrawling() || whileSetting.is(While.Mining) && Alien.BREAK.isMining(mc.player.getBlockPos())) {
            for (double offset : xzOffset) {
                for (double offset2 : xzOffset) {
                    BlockPos pos = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.2, mc.player.getZ() + offset2);
                    if (canBreak(pos)) {
                        PacketMine.INSTANCE.mine(pos);
                        work = true;
                        return;
                    }
                    if (web.getValue()) {
                        BlockPos web = new BlockPosX(mc.player.getX() + offset, mc.player.getY(), mc.player.getZ() + offset2);
                        if (mc.world.getBlockState(web).getBlock() == Blocks.COBWEB && canBreak(web)) {
                            PacketMine.INSTANCE.mine(web);
                            work = true;
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean canBreak(BlockPos pos) {
        return (BlockUtil.getClickSideStrict(pos) != null || pos.equals(PacketMine.getBreakPos())) && !PacketMine.unbreakable(pos) && !mc.world.isAir(pos);
    }

    private enum While {
        Crawling,
        Mining,
        Always
    }
}
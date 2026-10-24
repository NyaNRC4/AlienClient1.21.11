package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.AmbientOcclusionEvent;
import dev.luminous.api.events.impl.ChunkOcclusionEvent;
import dev.luminous.api.events.impl.RenderBlockEntityEvent;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class Xray extends Module {
    public static Xray INSTANCE;

    public Xray() {
        super("Xray", Category.Render);
        setChinese("矿物透视");
        INSTANCE = this;
    }

    private void openGui() {
        edit.setValueWithoutTask(false);
        if (!nullCheck()) {
            mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.XRAY)));
        }
    }

    public final BooleanSetting edit = add(new BooleanSetting("Edit", false).injectTask(this::openGui));

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        mc.worldRenderer.reload();
    }

    public boolean isBlocked(Block block) {
        return !Alien.XRAY.inWhitelist(block.getTranslationKey());
    }

    @EventListener
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        if (this.isBlocked(event.blockState.getBlock())) {
            event.cancel();
        }
    }

    @EventListener
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventListener
    private void onAmbientOcclusion(AmbientOcclusionEvent event) {
        event.lightLevel = 1;
    }

    public static boolean shouldBlock(BlockState state) {
        return INSTANCE.isOn() && INSTANCE.isBlocked(state.getBlock());
    }

    public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean returns) {
        if (!returns && !this.isBlocked(state.getBlock())) {
            return shouldReveal(state, view, pos.offset(facing), facing);
        }
        return returns;
    }

    public boolean modifyDrawSideAtNeighbor(BlockState state, BlockView view, BlockPos neighborPos, Direction facing, boolean returns) {
        if (!returns && !this.isBlocked(state.getBlock())) {
            return shouldReveal(state, view, neighborPos, facing);
        }
        return returns;
    }

    private boolean shouldReveal(BlockState state, BlockView view, BlockPos neighborPos, Direction facing) {
        BlockState neighborState = view.getBlockState(neighborPos);
        return neighborState.getCullingFace(facing.getOpposite()) != VoxelShapes.fullCube()
                || neighborState.getBlock() != state.getBlock()
                || Xray.isExposed(view, neighborPos);
    }

    public static boolean isExposed(BlockPos blockPos) {
        return isExposed(mc.world, blockPos);
    }

    public static boolean isExposed(BlockView view, BlockPos blockPos) {
        if (view == null) return true;

        for (Direction direction : Direction.values()) {
            if (!view.getBlockState(EXPOSED_POS.get().set(blockPos, direction)).isOpaque()) return true;
        }

        return false;
    }

    private static final ThreadLocal<BlockPos.Mutable> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);
}

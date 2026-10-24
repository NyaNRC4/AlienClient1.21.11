package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.SelfTrap;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.luminous.api.utils.Wrapper.mc;

@Mixin(World.class)
public abstract class MixinWorld {

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (mc.world != null && mc.world.isInBuildLimit(pos)) {
            if (SelfTrap.airList.contains(pos)) {
                cir.setReturnValue(Blocks.AIR.getDefaultState());
                return;
            }
            if (!ClientSetting.INSTANCE.mioCompatible.getValue()) {
                boolean terrainIgnore = CombatUtil.terrainIgnore;
                BlockPos modifyPos = CombatUtil.modifyPos;
                BlockState modifyBlockState = CombatUtil.modifyBlockState;
                if (terrainIgnore || modifyPos != null) {
                    WorldChunk worldChunk = mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

                    BlockState tempState = worldChunk.getBlockState(pos);

                    if (modifyPos != null && modifyBlockState != null) {
                        if (pos.equals(modifyPos)) {
                            cir.setReturnValue(modifyBlockState);
                            return;
                        }
                    }

                    if (terrainIgnore) {
                        if (Alien.HOLE.isHard(tempState.getBlock())) return;
                        cir.setReturnValue(Blocks.AIR.getDefaultState());
                    }
                } else if (InteractTweaks.INSTANCE.isActive) {
                    WorldChunk worldChunk = mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

                    BlockState tempState = worldChunk.getBlockState(pos);
                    if (tempState.getBlock() == Blocks.BEDROCK
                    ) {
                        cir.setReturnValue(Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }
}
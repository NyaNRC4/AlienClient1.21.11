package dev.luminous.api.utils.entity;

import net.minecraft.entity.player.PlayerEntity;

public class PlayerEntityPredict {
    public final PlayerEntity player;
    public final PlayerEntity predict;

    public PlayerEntityPredict(PlayerEntity player, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause) {
        this.player = player;
        if (ticks > 0) {
            predict = new CopyPlayerEntity(player, true, maxMotionY, ticks, simulation, step, doubleStep, jump, inBlockPause);
        } else {
            predict = player;
        }
    }
}
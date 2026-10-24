/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.entity.BlockEntity
 */
package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.block.BlockState;

public class RenderBlockEntityEvent
extends Event {
    private static final RenderBlockEntityEvent INSTANCE = new RenderBlockEntityEvent();
    public BlockState blockState;

    public static RenderBlockEntityEvent get(BlockState blockState) {
        INSTANCE.setCancelled(false);
        RenderBlockEntityEvent.INSTANCE.blockState = blockState;
        return INSTANCE;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class ChunkOcclusionEvent
extends Event {
    private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

    private ChunkOcclusionEvent() {
    }

    public static ChunkOcclusionEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}


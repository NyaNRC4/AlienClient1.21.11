/*
 * Decompiled with CFR 0.152.
 */
package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class S2CCloseScreenEvent
extends Event {
    private static final S2CCloseScreenEvent INSTANCE = new S2CCloseScreenEvent();

    public static S2CCloseScreenEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}


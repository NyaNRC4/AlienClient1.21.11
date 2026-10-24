/*
 * Decompiled with CFR 0.152.
 */
package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class KeyboardInputEvent
extends Event {
    private static final KeyboardInputEvent INSTANCE = new KeyboardInputEvent();

    private KeyboardInputEvent() {
    }

    public static KeyboardInputEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}


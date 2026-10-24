package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class MovementInputEvent
extends Event {
    private static final MovementInputEvent INSTANCE = new MovementInputEvent();

    private MovementInputEvent() {
    }

    public static MovementInputEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}

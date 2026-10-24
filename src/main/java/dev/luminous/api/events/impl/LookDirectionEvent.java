package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.Entity;

public class LookDirectionEvent extends Event {
    private LookDirectionEvent() {
    }

    private static final LookDirectionEvent instance = new LookDirectionEvent();
    private Entity entity;
    private double cursorDeltaX, cursorDeltaY;

    public static LookDirectionEvent get(final Entity entity, double cursorDeltaX, double cursorDeltaY) {
        instance.entity = entity;
        instance.cursorDeltaX = cursorDeltaX;
        instance.cursorDeltaY = cursorDeltaY;
        instance.setCancelled(false);
        return instance;
    }

    public Entity getEntity() {
        return entity;
    }

    public double getCursorDeltaX() {
        return cursorDeltaX;
    }

    public double getCursorDeltaY() {
        return cursorDeltaY;
    }
}

package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class SprintEvent
        extends Event {
    private static final SprintEvent instance = new SprintEvent();
    private boolean sprint;
    private boolean current;

    private SprintEvent() {
    }

    public static SprintEvent get(boolean current) {
        SprintEvent.instance.current = current;
        SprintEvent.instance.sprint = current;
        instance.setCancelled(false);
        return instance;
    }

    public boolean getCurrent() {
        return this.current;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }
}

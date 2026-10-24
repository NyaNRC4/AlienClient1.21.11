package dev.luminous.api.events;

public class Event {
    public Stage stage;
    private boolean cancel;

    public Event() {
        this(Stage.Pre);
    }

    public Event(Stage stage) {
        this.cancel = false;
        this.stage = stage;
    }

    public void cancel() {
        setCancelled(true);
    }

    public boolean isCancelled() {
        return cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isPost() {
        return stage == Stage.Post;
    }

    public boolean isPre() {
        return stage == Stage.Pre;
    }

    public enum Stage {
        Pre, Post
    }
}

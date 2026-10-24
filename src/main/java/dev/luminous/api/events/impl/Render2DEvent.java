package dev.luminous.api.events.impl;

import net.minecraft.client.gui.DrawContext;

public class Render2DEvent {
    private static final Render2DEvent INSTANCE = new Render2DEvent();
    public DrawContext drawContext;
    public float tickDelta;

    public static Render2DEvent get(DrawContext drawContext, float tickDelta) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }
}

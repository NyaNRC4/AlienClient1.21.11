package dev.luminous.api.events.impl;

import dev.luminous.api.utils.render.Render3DUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

import java.awt.*;

public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();
    public MatrixStack matrixStack;
    public float tickDelta;

    public static Render3DEvent get(MatrixStack matrixStack, float tickDelta) {
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }

    public void drawBox(Box box, Color color) {
        Render3DUtil.drawBox(matrixStack, box, color);
    }

    public void drawFill(Box box, Color color) {
        Render3DUtil.drawFill(matrixStack, box, color);
    }
}


package dev.luminous.api.utils.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Collects world overlays and submits them once per world render pass. */
public final class Render3DScheduler {
    public static final Render3DScheduler INSTANCE = new Render3DScheduler();

    private final List<BoxCommand> boxes = new ArrayList<>();
    private final List<FadeCommand> fades = new ArrayList<>();
    private final List<LineCommand> lines = new ArrayList<>();

    private Render3DScheduler() {
    }

    public void addBox(Box box, Color fill, Color outline, boolean drawOutline, boolean drawFill) {
        if (drawOutline || drawFill) {
            boxes.add(new BoxCommand(box, fill, outline, drawOutline, drawFill));
        }
    }

    public void addFade(Box box, Color bottom, Color top) {
        fades.add(new FadeCommand(box, bottom, top));
    }

    public void addLine(Vec3d start, Vec3d end, Color color, float width) {
        if (start.squaredDistanceTo(end) >= 1.0E-6) {
            lines.add(new LineCommand(start, end, color, width));
        }
    }

    public void flush(MatrixStack matrices) {
        try {
            for (BoxCommand command : boxes) {
                Render3DUtil.draw3DBox(matrices, command.box, command.fill, command.outline,
                        command.outlineEnabled, command.fillEnabled);
            }
            for (FadeCommand command : fades) {
                Render3DUtil.drawFadeFill(matrices, command.box, command.bottom, command.top);
            }
            for (LineCommand command : lines) {
                Render3DUtil.drawLine(matrices, command.start, command.end, command.color, command.width);
            }
        } finally {
            clear();
        }
    }

    public void clear() {
        boxes.clear();
        fades.clear();
        lines.clear();
    }

    private record BoxCommand(Box box, Color fill, Color outline, boolean outlineEnabled, boolean fillEnabled) {}
    private record FadeCommand(Box box, Color bottom, Color top) {}
    private record LineCommand(Vec3d start, Vec3d end, Color color, float width) {}
}

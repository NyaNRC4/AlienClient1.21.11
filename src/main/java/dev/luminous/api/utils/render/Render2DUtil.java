package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.luminous.api.utils.Wrapper;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public class Render2DUtil
        implements Wrapper {
    public static void rect(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int color) {
        Render2DUtil.rectFilled(stack, x1, y1, x2, y2, color);
    }

    public static void arrow(Matrix3x2fStack matrixStack, float x, float y, Color color) {
        Render2DUtil.drawRectWithOutline(matrixStack, x - 1.0f, y - 1.0f, 2.0f, 2.0f, color, Color.BLACK);
    }

    public static void rectFilled(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int color) {
        float i;
        float f = (float)(color >> 24 & 0xFF) / 255.0f;
        float g = (float)(color >> 16 & 0xFF) / 255.0f;
        float h = (float)(color >> 8 & 0xFF) / 255.0f;
        float j = (float)(color & 0xFF) / 255.0f;
        if ((double)f <= 0.01) {
            return;
        }
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, x1, y2).color(g, h, j, f);
        bufferBuilder.vertex(stack, x2, y2).color(g, h, j, f);
        bufferBuilder.vertex(stack, x2, y1).color(g, h, j, f);
        bufferBuilder.vertex(stack, x1, y1).color(g, h, j, f);
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void horizontalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, x1, y1).color(startColor.getRGB());
        bufferBuilder.vertex(stack, x1, y2).color(startColor.getRGB());
        bufferBuilder.vertex(stack, x2, y2).color(endColor.getRGB());
        bufferBuilder.vertex(stack, x2, y1).color(endColor.getRGB());
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void horizontalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int startColor, int endColor) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, x1, y1).color(startColor);
        bufferBuilder.vertex(stack, x1, y2).color(startColor);
        bufferBuilder.vertex(stack, x2, y2).color(endColor);
        bufferBuilder.vertex(stack, x2, y1).color(endColor);
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void verticalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, x1, y1).color(startColor.getRGB());
        bufferBuilder.vertex(stack, x2, y1).color(startColor.getRGB());
        bufferBuilder.vertex(stack, x2, y2).color(endColor.getRGB());
        bufferBuilder.vertex(stack, x1, y2).color(endColor.getRGB());
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void verticalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int startColor, int endColor) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, x1, y1).color(startColor);
        bufferBuilder.vertex(stack, x2, y1).color(startColor);
        bufferBuilder.vertex(stack, x2, y2).color(endColor);
        bufferBuilder.vertex(stack, x1, y2).color(endColor);
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void drawLine(Matrix3x2fStack stack, float x, float y, float x1, float y1, int color) {
        final float thickness = 0.5f;

        float dx = x1 - x;
        float dy = y1 - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001f) return;

        float nx = -dy / len * (thickness / 2f);
        float ny = dx / len * (thickness / 2f);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(stack, x + nx, y + ny).color(color);
        bufferBuilder.vertex(stack, x - nx, y - ny).color(color);

        bufferBuilder.vertex(stack, x1 + nx, y1 + ny).color(color);
        bufferBuilder.vertex(stack, x1 - nx, y1 - ny).color(color);

        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void drawRectWithOutline(Matrix3x2fStack stack, float x, float y, float width, float height, Color c, Color c2) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(stack, x, y + height).color(c.getRGB());
        buffer.vertex(stack, x + width, y + height).color(c.getRGB());
        buffer.vertex(stack, x + width, y).color(c.getRGB());
        buffer.vertex(stack, x, y).color(c.getRGB());
        ImmediateDrawUtil.drawGui(buffer);
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(stack, x, y + height).color(c2.getRGB());
        buffer.vertex(stack, x + width, y + height).color(c2.getRGB());
        buffer.vertex(stack, x + width, y).color(c2.getRGB());
        buffer.vertex(stack, x, y).color(c2.getRGB());
        buffer.vertex(stack, x, y + height).color(c2.getRGB());
        ImmediateDrawUtil.drawGui(buffer);
    }

    public static void drawRect(Matrix3x2fStack stack, float x, float y, float width, float height, int c) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, x, y + height).color(c);
        bufferBuilder.vertex(stack, x + width, y + height).color(c);
        bufferBuilder.vertex(stack, x + width, y).color(c);
        bufferBuilder.vertex(stack, x, y).color(c);
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void drawRect(Matrix3x2fStack stack, float x, float y, float width, float height, Color c) {
        Render2DUtil.drawRect(stack, x, y, width, height, c.getRGB());
    }

    public static void drawRect(DrawContext drawContext, float x, float y, float width, float height, Color c) {
        Render2DUtil.drawRect(drawContext.getMatrices(), x, y, width, height, c);
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
    }

    public static void drawGlow(Matrix3x2fStack stack, float x, float y, float width, float height, int color) {
        int startColor = ColorUtil.injectAlpha(color, 20);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        float centerX = x + halfWidth;
        float centerY = y + halfHeight;
        float x2 = x + width;
        float y2 = y + height;
        bufferBuilder.vertex(stack, centerX, centerY).color(color);
        bufferBuilder.vertex(stack, x, centerY).color(startColor);
        bufferBuilder.vertex(stack, x, y).color(startColor);
        bufferBuilder.vertex(stack, centerX, y).color(startColor);
        bufferBuilder.vertex(stack, centerX, centerY).color(color);
        bufferBuilder.vertex(stack, centerX, y).color(startColor);
        bufferBuilder.vertex(stack, x2, y).color(startColor);
        bufferBuilder.vertex(stack, x2, centerY).color(startColor);
        bufferBuilder.vertex(stack, centerX, centerY).color(color);
        bufferBuilder.vertex(stack, x, centerY).color(startColor);
        bufferBuilder.vertex(stack, x, y2).color(startColor);
        bufferBuilder.vertex(stack, centerX, y2).color(startColor);
        bufferBuilder.vertex(stack, centerX, centerY).color(color);
        bufferBuilder.vertex(stack, x2, centerY).color(startColor);
        bufferBuilder.vertex(stack, x2, y2).color(startColor);
        bufferBuilder.vertex(stack, centerX, y2).color(startColor);
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void drawCircle(Matrix3x2fStack stack, float cx, float cy, float r, Color c, int segments) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(stack, cx, cy).color(c.getRGB());
        for (int i = 0; i <= segments; ++i) {
            double keyCodec = (double)i * (Math.PI * 2) / (double)segments;
            float x = (float)((double)cx + Math.cos(keyCodec) * (double)r);
            float y = (float)((double)cy + Math.sin(keyCodec) * (double)r);
            bufferBuilder.vertex(stack, x, y).color(c.getRGB());
        }
        ImmediateDrawUtil.drawGui(bufferBuilder);
    }

    public static void drawPill(Matrix3x2fStack stack, float x, float y, float width, float height, Color c) {
        float r = height / 2.0f;
        Render2DUtil.drawRect(stack, x + r, y, width - 2.0f * r, height, c);
        Render2DUtil.drawCircle(stack, x + r, y + r, r, c, 64);
        Render2DUtil.drawCircle(stack, x + width - r, y + r, r, c, 64);
    }

    public static void drawRoundedStroke(Matrix3x2fStack stack, float x, float y, float width, float height, float radius, Color c, int seg) {
        double keyCodec;
        int i;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float r = Math.min(radius, Math.min(width, height) / 2.0f);
        float x1 = x + r;
        float y1 = y + r;
        float x2 = x + width - r;
        float y2 = y + height - r;
        for (i = 0; i <= seg; ++i) {
            keyCodec = -1.5707963267948966 + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        for (i = 0; i <= seg; ++i) {
            keyCodec = 0.0 + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        for (i = 0; i <= seg; ++i) {
            keyCodec = 1.5707963267948966 + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        for (i = 0; i <= seg; ++i) {
            keyCodec = Math.PI + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        ImmediateDrawUtil.drawGui(buffer);
    }

    public static void drawRainbowRoundedStroke(Matrix3x2fStack stack, float x, float y, float width, float height, float radius, int seg, float speed, int alpha) {
        int argb;
        int rgb;
        int i;
        int argb2;
        int rgb2;
        float hue;
        double keyCodec;
        int i2;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float r = Math.min(radius, Math.min(width, height) / 2.0f);
        float x1 = x + r;
        float y1 = y + r;
        float x2 = x + width - r;
        float y2 = y + height - r;
        double t = (double)(System.currentTimeMillis() % (long)(1000.0f / Math.max(0.001f, speed))) / (double)(1000.0f / Math.max(0.001f, speed));
        for (i2 = 0; i2 <= seg; ++i2) {
            keyCodec = -1.5707963267948966 + (double)i2 * 1.5707963267948966 / (double)seg;
            hue = (float)((keyCodec + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb2 = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            argb2 = alpha << 24 | rgb2 & 0xFFFFFF;
            buffer.vertex(stack, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r)).color(argb2);
        }
        for (i2 = 0; i2 <= seg; ++i2) {
            keyCodec = 0.0 + (double)i2 * 1.5707963267948966 / (double)seg;
            hue = (float)((keyCodec + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb2 = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            argb2 = alpha << 24 | rgb2 & 0xFFFFFF;
            buffer.vertex(stack, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r)).color(argb2);
        }
        for (i2 = 0; i2 <= seg; ++i2) {
            keyCodec = 1.5707963267948966 + (double)i2 * 1.5707963267948966 / (double)seg;
            hue = (float)((keyCodec + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb2 = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            argb2 = alpha << 24 | rgb2 & 0xFFFFFF;
            buffer.vertex(stack, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r)).color(argb2);
        }
        for (i2 = 0; i2 <= seg; ++i2) {
            keyCodec = Math.PI + (double)i2 * 1.5707963267948966 / (double)seg;
            hue = (float)((keyCodec + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb2 = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            argb2 = alpha << 24 | rgb2 & 0xFFFFFF;
            buffer.vertex(stack, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r)).color(argb2);
        }
        ImmediateDrawUtil.drawGui(buffer);
        BufferBuilder inner = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float rIn = Math.max(0.5f, r - 1.2f);
        for (i = 0; i <= seg; ++i) {
            double a2 = -1.5707963267948966 + (double)i * 1.5707963267948966 / (double)seg;
            float hue2 = (float)((a2 + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb = Color.HSBtoRGB(hue2, 1.0f, 1.0f);
            argb = alpha << 24 | rgb & 0xFFFFFF;
            inner.vertex(stack, (float)((double)x2 + Math.cos(a2) * (double)rIn), (float)((double)y1 + Math.sin(a2) * (double)rIn)).color(argb);
        }
        for (i = 0; i <= seg; ++i) {
            double a3 = 0.0 + (double)i * 1.5707963267948966 / (double)seg;
            float hue3 = (float)((a3 + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb = Color.HSBtoRGB(hue3, 1.0f, 1.0f);
            argb = alpha << 24 | rgb & 0xFFFFFF;
            inner.vertex(stack, (float)((double)x2 + Math.cos(a3) * (double)rIn), (float)((double)y2 + Math.sin(a3) * (double)rIn)).color(argb);
        }
        for (i = 0; i <= seg; ++i) {
            double a4 = 1.5707963267948966 + (double)i * 1.5707963267948966 / (double)seg;
            float hue4 = (float)((a4 + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb = Color.HSBtoRGB(hue4, 1.0f, 1.0f);
            argb = alpha << 24 | rgb & 0xFFFFFF;
            inner.vertex(stack, (float)((double)x1 + Math.cos(a4) * (double)rIn), (float)((double)y2 + Math.sin(a4) * (double)rIn)).color(argb);
        }
        for (i = 0; i <= seg; ++i) {
            double a5 = Math.PI + (double)i * 1.5707963267948966 / (double)seg;
            float hue5 = (float)((a5 + Math.PI * 2) / (Math.PI * 2) + t) % 1.0f;
            rgb = Color.HSBtoRGB(hue5, 1.0f, 1.0f);
            argb = alpha << 24 | rgb & 0xFFFFFF;
            inner.vertex(stack, (float)((double)x1 + Math.cos(a5) * (double)rIn), (float)((double)y1 + Math.sin(a5) * (double)rIn)).color(argb);
        }
        ImmediateDrawUtil.drawGui(inner);
    }

    public static void drawRoundedRect(Matrix3x2fStack stack, float x, float y, float width, float height, float radius, Color c) {
        double keyCodec;
        int i;
        if (radius <= 0.0f) {
            Render2DUtil.drawRect(stack, x, y, width, height, c);
            return;
        }
        float r = Math.min(radius, Math.min(width, height) / 2.0f);
        Render2DUtil.drawRect(stack, x + r, y, width - 2.0f * r, height, c);
        Render2DUtil.drawRect(stack, x, y + r, r, height - 2.0f * r, c);
        Render2DUtil.drawRect(stack, x + width - r, y + r, r, height - 2.0f * r, c);
        int seg = 48;
        float cx = x + r;
        float cy = y + r;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(stack, cx, cy).color(c.getRGB());
        for (i = 0; i <= seg; ++i) {
            keyCodec = Math.PI + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        ImmediateDrawUtil.drawGui(buffer);
        cx = x + width - r;
        cy = y + r;
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(stack, cx, cy).color(c.getRGB());
        for (i = 0; i <= seg; ++i) {
            keyCodec = 4.71238898038469 + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        ImmediateDrawUtil.drawGui(buffer);
        cx = x + width - r;
        cy = y + height - r;
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(stack, cx, cy).color(c.getRGB());
        for (i = 0; i <= seg; ++i) {
            keyCodec = 0.0 + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        ImmediateDrawUtil.drawGui(buffer);
        cx = x + r;
        cy = y + height - r;
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(stack, cx, cy).color(c.getRGB());
        for (i = 0; i <= seg; ++i) {
            keyCodec = 1.5707963267948966 + (double)i * 1.5707963267948966 / (double)seg;
            buffer.vertex(stack, (float)((double)cx + Math.cos(keyCodec) * (double)r), (float)((double)cy + Math.sin(keyCodec) * (double)r)).color(c.getRGB());
        }
        ImmediateDrawUtil.drawGui(buffer);
    }

    public static void drawDropShadow(Matrix3x2fStack matrices, float x, float y, float width, float height, float radius) {
    }
}

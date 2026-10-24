package dev.luminous.api.utils.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;

public class Render3DUtil implements Wrapper {
    public static void endBuilding(BufferBuilder bb) {
        ImmediateDrawUtil.drawWorld(bb);
    }

    public static MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();
        Camera camera = Render3DUtil.mc.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x - camera.getCameraPos().x, y - camera.getCameraPos().y, z - camera.getCameraPos().z);
        return matrices;
    }

    public static void drawText3D(String text, Vec3d vec3d, Color color) {
        Render3DUtil.drawText3D(Text.of(text), vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color.getRGB());
    }

    public static void drawText3D(String text, Vec3d vec3d, int color) {
        Render3DUtil.drawText3D(Text.of(text), vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color);
    }

    public static void drawText3D(Text text, Vec3d vec3d, double offX, double offY, double scale, Color color) {
        Render3DUtil.drawText3D(text, vec3d.x, vec3d.y, vec3d.z, offX, offY, scale, color.getRGB());
    }

    public static void drawText3D(Text text, double x, double y, double z, double offX, double offY, double scale, int color) {
        MatrixStack matrices = Render3DUtil.matrixFrom(x, y, z);
        Camera camera = Render3DUtil.mc.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.translate(offX, offY, 0.0);
        matrices.scale(-0.025f * (float) scale, -0.025f * (float) scale, 1.0f);
        int halfWidth = Render3DUtil.mc.textRenderer.getWidth(text) / 2;
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(1536));
        Render3DUtil.mc.textRenderer.draw(text.getString(), (float) (-halfWidth), 0.0f, color, true, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        immediate.draw();
    }

    public static void drawFill(MatrixStack matrixStack, Box bb, Color fillColor) {
        Render3DUtil.draw3DBox(matrixStack, bb, fillColor, new Color(0, 0, 0, 0), false, true);
    }

    public static void drawBox(MatrixStack matrixStack, Box bb, Color outlineColor) {
        Render3DUtil.draw3DBox(matrixStack, bb, new Color(0, 0, 0, 0), outlineColor, true, false);
    }

    public static void drawBox(MatrixStack matrixStack, Box bb, Color outlineColor, float lineWidth) {
        Render3DUtil.draw3DBox(matrixStack, bb, new Color(0, 0, 0, 0), outlineColor, true, false, lineWidth);
    }

    public static void draw3DBox(MatrixStack matrixStack, Box box, Color fillColor, Color outlineColor) {
        Render3DUtil.draw3DBox(matrixStack, box, fillColor, outlineColor, true, true);
    }

    public static void draw3DBox(MatrixStack matrixStack, Box box, Color fillColor, Color outlineColor, boolean outline, boolean fill) {
        Render3DUtil.draw3DBox(matrixStack, box, fillColor, outlineColor, outline, fill, 1.5f);
    }

    public static void draw3DBox(MatrixStack matrixStack, Box box, Color fillColor, Color outlineColor, boolean outline, boolean fill, float lineWidth) {
        box = box.offset(mc.gameRenderer.getCamera().getCameraPos().negate());
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        if (outline) {
            float a = outlineColor.getAlpha() / 255f;
            float r = outlineColor.getRed() / 255f;
            float g = outlineColor.getGreen() / 255f;
            float b = outlineColor.getBlue() / 255f;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

            ImmediateDrawUtil.drawWorld(bufferBuilder);
        }

        if (fill) {
            float a = fillColor.getAlpha() / 255f;
            float r = fillColor.getRed() / 255f;
            float g = fillColor.getGreen() / 255f;
            float b = fillColor.getBlue() / 255f;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

            ImmediateDrawUtil.drawWorld(bufferBuilder);
        }
    }

    public static void drawFadeFill(MatrixStack stack, Box box, Color c, Color c1) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f posMatrix = stack.peek().getPositionMatrix();
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getCameraPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getCameraPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getCameraPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getCameraPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getCameraPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getCameraPos().getZ());
        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());

        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());

        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());

        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());

        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());

        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());

        ImmediateDrawUtil.drawWorld(buffer);
    }

    public static void drawLine(MatrixStack matrices, Vec3d start, Vec3d end, Color color, float width) {
        if (start.squaredDistanceTo(end) < 1.0E-6) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
        vertexLine(matrices, buffer, start.x, start.y, start.z, end.x, end.y, end.z, color, width);
        ImmediateDrawUtil.drawWorld(buffer);
    }

    public static void drawLine(Vec3d start, Vec3d end, Color color) {
        Render3DUtil.drawLine(start.x, start.getY(), start.z, end.getX(), end.getY(), end.getZ(), color, 1.0f);
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color, float width) {
        MatrixStack matrices = Render3DUtil.matrixFrom(x1, y1, z1);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
        Render3DUtil.vertexLine(matrices, buffer, 0.0, 0.0, 0.0, x2 - x1, y2 - y1, z2 - z1, color, width);
        ImmediateDrawUtil.drawWorld(buffer);
    }

    public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2, Color lineColor, float lineWidth) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normalVec = Render3DUtil.getNormal((float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2);
        buffer.vertex(model, (float) x1, (float) y1, (float) z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z()).lineWidth(lineWidth);
        buffer.vertex(model, (float) x2, (float) y2, (float) z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z()).lineWidth(lineWidth);
    }

    public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        if (normalSqrt < 1.0E-5f) {
            return new Vector3f(0.0f, 1.0f, 0.0f);
        }
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    //3D世界渲染矩形，由1.21.1 2d渲染修改而成
    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, int c) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(c);
        ImmediateDrawUtil.drawWorld(bufferBuilder);
    }

    public static void drawTargetEsp(MatrixStack stack, @NotNull Entity target, Color color) {
        int j;
        ArrayList<Vec3d> vecs = new ArrayList<Vec3d>();
        ArrayList<Vec3d> vecs1 = new ArrayList<Vec3d>();
        ArrayList<Vec3d> vecs2 = new ArrayList<Vec3d>();
        Vec3d cameraPos = Render3DUtil.mc.gameRenderer.getCamera().getCameraPos();
        double x = target.lastRenderX + (target.getX() - target.lastRenderX) * (double) Render3DUtil.getDynamicDeltaTicks() - cameraPos.getX();
        double y = target.lastRenderY + (target.getY() - target.lastRenderY) * (double) Render3DUtil.getDynamicDeltaTicks() - cameraPos.getY();
        double z = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * (double) Render3DUtil.getDynamicDeltaTicks() - cameraPos.getZ();
        double height = target.getHeight();
        for (int i = 0; i <= 361; ++i) {
            double v = Math.sin(Math.toRadians(i));
            double u = Math.cos(Math.toRadians(i));
            Vec3d vec = new Vec3d((double) ((float) (u * 0.5)), height, (double) ((float) (v * 0.5)));
            vecs.add(vec);
            double v1 = Math.sin(Math.toRadians((i + 120) % 360));
            double u1 = Math.cos(Math.toRadians(i + 120) % 360.0);
            Vec3d vec1 = new Vec3d((double) ((float) (u1 * 0.5)), height, (double) ((float) (v1 * 0.5)));
            vecs1.add(vec1);
            double v2 = Math.sin(Math.toRadians((i + 240) % 360));
            double u2 = Math.cos(Math.toRadians((i + 240) % 360));
            Vec3d vec2 = new Vec3d((double) ((float) (u2 * 0.5)), height, (double) ((float) (v2 * 0.5)));
            vecs2.add(vec2);
            height -= (double) 0.004f;
        }
        stack.push();
        stack.translate(x, y, z);
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (j = 0; j < vecs.size() - 1; ++j) {
            float alpha = 1.0f - ((float) j + (float) (System.currentTimeMillis() - Alien.initTime) / 5.0f) % 360.0f / 60.0f;
            bufferBuilder.vertex(matrix, (float) vecs.get(j).x, (float) vecs.get(j).y, (float) vecs.get(j).z).color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) ((float) j / 20.0f), 10, 1.0), (int) (alpha * 255.0f)).getRGB());
            bufferBuilder.vertex(matrix, (float) vecs.get(j + 1).x, (float) vecs.get(j + 1).y + 0.1f, (float) vecs.get(j + 1).z).color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) ((float) j / 20.0f), 10, 1.0), (int) (alpha * 255.0f)).getRGB());
        }
        Render3DUtil.endBuilding(bufferBuilder);
        bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (j = 0; j < vecs1.size() - 1; ++j) {
            float alpha = 1.0f - ((float) j + (float) (System.currentTimeMillis() - Alien.initTime) / 5.0f) % 360.0f / 60.0f;
            bufferBuilder.vertex(matrix, (float) vecs1.get(j).x, (float) vecs1.get(j).y, (float) vecs1.get(j).z).color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) ((float) j / 20.0f), 10, 1.0), (int) (alpha * 255.0f)).getRGB());
            bufferBuilder.vertex(matrix, (float) vecs1.get(j + 1).x, (float) vecs1.get(j + 1).y + 0.1f, (float) vecs1.get(j + 1).z).color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) ((float) j / 20.0f), 10, 1.0), (int) (alpha * 255.0f)).getRGB());
        }
        Render3DUtil.endBuilding(bufferBuilder);
        bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (j = 0; j < vecs2.size() - 1; ++j) {
            float alpha = 1.0f - ((float) j + (float) (System.currentTimeMillis() - Alien.initTime) / 5.0f) % 360.0f / 60.0f;
            bufferBuilder.vertex(matrix, (float) vecs2.get(j).x, (float) vecs2.get(j).y, (float) vecs2.get(j).z).color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) ((float) j / 20.0f), 10, 1.0), (int) (alpha * 255.0f)).getRGB());
            bufferBuilder.vertex(matrix, (float) vecs2.get(j + 1).x, (float) vecs2.get(j + 1).y + 0.1f, (float) vecs2.get(j + 1).z).color(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int) ((float) j / 20.0f), 10, 1.0), (int) (alpha * 255.0f)).getRGB());
        }
        Render3DUtil.endBuilding(bufferBuilder);
        stack.pop();
    }

    public static float getDynamicDeltaTicks() {
        return mc.getRenderTickCounter().getTickProgress(true);
    }
}


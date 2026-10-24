package dev.luminous.api.utils.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.awt.*;
public final class HandTintUtil {

    private HandTintUtil() {
    }

    public static VertexConsumerProvider wrap(VertexConsumerProvider provider, Color color) {
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;
        float alpha = color.getAlpha() / 255.0F;
        return new VertexConsumerProvider() {
            @Override
            public VertexConsumer getBuffer(RenderLayer layer) {
                return new TintingVertexConsumer(provider.getBuffer(layer), red, green, blue, alpha);
            }
        };
    }

    private static final class TintingVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private TintingVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.delegate.color(
                    Math.round(red * this.red),
                    Math.round(green * this.green),
                    Math.round(blue * this.blue),
                    Math.round(alpha * this.alpha)
            );
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;
            return this.color(r, g, b, a);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            this.delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            this.delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            this.delegate.lineWidth(width);
            return this;
        }
    }
}

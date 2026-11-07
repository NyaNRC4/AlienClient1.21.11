package dev.luminous.api.utils.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.RawProjectionMatrix;
import org.joml.Matrix4f;

public final class ProjectionUtil {
    private static RawProjectionMatrix projection;
    private static GpuBufferSlice projectionSlice;
    private static final Matrix4f POSITION_MATRIX = new Matrix4f();
    private static boolean valid;

    private ProjectionUtil() {
    }
    public static void capture(Matrix4f positionMatrix, Matrix4f basicProjectionMatrix) {
        if (projection == null) {
            projection = new RawProjectionMatrix("alien_world_3d");
        }
        POSITION_MATRIX.set(positionMatrix);
        projectionSlice = projection.set(basicProjectionMatrix);
        valid = true;
    }

    public static boolean isValid() {
        return valid && projectionSlice != null;
    }

    public static GpuBufferSlice projectionSlice() {
        return projectionSlice;
    }

    public static Matrix4f positionMatrix() {
        return POSITION_MATRIX;
    }
}

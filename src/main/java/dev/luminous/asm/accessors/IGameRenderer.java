package dev.luminous.asm.accessors;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={GameRenderer.class})
public interface IGameRenderer {
    @Invoker(value="renderHand")
    public void IRenderHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix);

    @Accessor("fogRenderer")
    FogRenderer getFogRenderer();
}
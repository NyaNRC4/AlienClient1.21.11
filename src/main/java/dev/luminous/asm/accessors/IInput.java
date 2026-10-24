package dev.luminous.asm.accessors;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Input.class})
public interface IInput {
    @Accessor(value="movementVector")
    public Vec2f getMovementVector();

    @Accessor(value="movementVector")
    public void setMovementVector(Vec2f var1);
}

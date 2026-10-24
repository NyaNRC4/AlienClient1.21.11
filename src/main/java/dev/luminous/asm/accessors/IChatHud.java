package dev.luminous.asm.accessors;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value={ChatHud.class})
public interface IChatHud {
    @Mutable
    @Accessor(value="visibleMessages")
    public void setVisibleMessages(List<ChatHudLine.Visible> var1);

    @Mutable
    @Accessor(value="messages")
    public void setMessages(List<ChatHudLine> var1);
}
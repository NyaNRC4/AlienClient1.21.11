package dev.luminous.asm.mixins;

import dev.luminous.api.interfaces.IChatHudLineHook;
import dev.luminous.api.utils.math.FadeUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ChatHudLine.class)
public class MixinChatHudLine implements IChatHudLineHook {
    @Unique
    private int id = 0;
    @Unique
    private boolean sync = false;
    @Unique
    private FadeUtils fade;

    @Override
    public int alienClient$getMessageId() {
        return id;
    }

    @Override
    public void alienClient$setMessageId(int id) {
        this.id = id;
    }

    @Override
    public boolean alienClient$getSync() {
        return sync;
    }

    @Override
    public void alienClient$setSync(boolean sync) {
        this.sync = sync;
    }

    @Override
    public FadeUtils alienClient$getFade() {
        return fade;
    }

    @Override
    public void alienClient$setFade(FadeUtils fade) {
        this.fade = fade;
    }
}

package dev.luminous.asm.mixins;

import dev.luminous.api.interfaces.IChatHudHook;
import dev.luminous.api.interfaces.IChatHudLineHook;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.render.ChatHudUtil;
import dev.luminous.asm.accessors.IChatHud;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 50%Aicode
 */
@Mixin(value = { ChatHud.class })
public abstract class MixinChatHud implements IChatHudHook {
    @Unique
    private int nextMessageId = 0;
    @Unique
    private boolean nextSync;
    @Final
    @Shadow
    private List<ChatHudLine.Visible> visibleMessages;
    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Inject(method = { "<init>" }, at = { @At(value = "TAIL") })
    public void onInit(MinecraftClient client, CallbackInfo ci) {
        ((IChatHud) ((Object) this)).setMessages(new CopyOnWriteArrayList<ChatHudLine>());
        ((IChatHud) ((Object) this)).setVisibleMessages(new CopyOnWriteArrayList<ChatHudLine.Visible>());
    }

    @Override
    public void alienClient$addMessage(Text message, int id) {
        this.nextMessageId = id;
        this.nextSync = true;
        this.addMessage(message);
        this.nextSync = false;
        this.nextMessageId = 0;
    }

    @Override
    public void alienClient$addMessage(Text message) {
        this.nextSync = true;
        this.addMessage(message);
        this.nextSync = false;
    }

    @Override
    public void alienClient$addMessageOutSync(Text message, int id) {
        this.nextMessageId = id;
        this.addMessage(message);
        this.nextMessageId = 0;
    }

    @Inject(method = { "addVisibleMessage" }, at = { @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER, remap = false) })
    private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
        IChatHudLineHook line = (IChatHudLineHook) (Object) this.visibleMessages.getFirst();
        if (line != null) {
            line.alienClient$setMessageId(this.nextMessageId);
            line.alienClient$setSync(this.nextSync);
            line.alienClient$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
        }
    }

    @Inject(method = { "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V" }, at = { @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER, remap = false) })
    private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
        IChatHudLineHook line = (IChatHudLineHook) (Object) this.messages.getFirst();
        if (line != null) {
            line.alienClient$setMessageId(this.nextMessageId);
            line.alienClient$setSync(this.nextSync);
            line.alienClient$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
        }
    }

    @Inject(at = { @At(value = "HEAD") }, method = { "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V" })
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (this.nextMessageId != 0) {
            this.visibleMessages.removeIf(msg -> ((IChatHudLineHook) (Object) msg).alienClient$getMessageId() == this.nextMessageId);
            this.messages.removeIf(msg -> ((IChatHudLineHook) (Object) msg).alienClient$getMessageId() == this.nextMessageId);
        }
    }

    @Redirect(method = { "addVisibleMessage" }, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2, remap = false), require = 0)
    public int chatLinesSize(List<ChatHudLine.Visible> list) {
        return ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue() ? Integer.MIN_VALUE : list.size();
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private ChatHud.Backend chatHudAnimate(ChatHud.Backend drawer) {
        if (!ClientSetting.INSTANCE.isOn()) {
            return drawer;
        }
        return new ChatHudUtil(drawer, this::alienClient$findLine);
    }

    @Unique
    private IChatHudLineHook alienClient$findLine(OrderedText text) {
        for (ChatHudLine.Visible visible : this.visibleMessages) {
            if (visible.content() == text) {
                return (IChatHudLineHook) (Object) visible;
            }
        }
        return null;
    }

    @Shadow
    public abstract void addMessage(Text var1);

    @Shadow
    public int getWidth() {
        return 0;
    }
}

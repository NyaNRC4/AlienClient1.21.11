package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.SendMessageEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class ChatAppend extends Module {
    public static ChatAppend INSTANCE;
    private final StringSetting message = add(new StringSetting("Text", Alien.NAME));

    public ChatAppend() {
        super("ChatAppend", Category.Misc);
        setChinese("消息后缀");
        INSTANCE = this;
    }

    @EventListener
    public void onSendMessage(SendMessageEvent event) {
        if (nullCheck() || event.isCancelled() || AutoReconnect.inQueueServer) return;
        String message = event.message;

        if (message.startsWith("/") || message.startsWith("!") || message.startsWith("$") || message.startsWith("#") || message.endsWith(this.message.getValue())) {
            return;
        }
        String suffix = this.message.getValue();
        message = message + " " + suffix;
        event.message = message;
    }
}
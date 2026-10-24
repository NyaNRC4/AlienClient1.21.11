package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

public class NoTerrainScreen extends Module {
    public NoTerrainScreen() {
        super("NoTerrainScreen", Category.Misc);
        setChinese("没有加载界面");
    }

    @EventListener
    public void onEvent(ClientTickEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen instanceof LevelLoadingScreen || mc.currentScreen instanceof ProgressScreen) {
            mc.currentScreen = null;
        }
    }
}

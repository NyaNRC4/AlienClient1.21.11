/*
 * Decompiled with CFR 0.152.
 */
package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.mod.commands.Command;
import java.util.List;

public class ReloadCommand
extends Command {
    public ReloadCommand() {
        super("reload", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        this.sendChatMessage("\u00a7fReloading..");
        Alien.CONFIG = new ConfigManager();
        Alien.CONFIG.load();
        Alien.CLEANER.read();
        Alien.XRAY.read();
        Alien.TRADE.read();
        Alien.FRIEND.read();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  by.radioegor146.nativeobfuscator.Native
 *  net.minecraft.text.Text
 */
package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.interfaces.IChatHudHook;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.commands.impl.AimCommand;
import dev.luminous.mod.commands.impl.BindCommand;
import dev.luminous.mod.commands.impl.BindsCommand;
import dev.luminous.mod.commands.impl.CleanerCommand;
import dev.luminous.mod.commands.impl.ClipCommand;
import dev.luminous.mod.commands.impl.EsuCommand;
import dev.luminous.mod.commands.impl.FakePlayerCommand;
import dev.luminous.mod.commands.impl.FriendCommand;
import dev.luminous.mod.commands.impl.GamemodeCommand;
import dev.luminous.mod.commands.impl.GcCommand;
import dev.luminous.mod.commands.impl.KitCommand;
import dev.luminous.mod.commands.impl.LoadCommand;
import dev.luminous.mod.commands.impl.PeekCommand;
import dev.luminous.mod.commands.impl.PingCommand;
import dev.luminous.mod.commands.impl.PrefixCommand;
import dev.luminous.mod.commands.impl.RejoinCommand;
import dev.luminous.mod.commands.impl.ReloadCommand;
import dev.luminous.mod.commands.impl.SaveCommand;
import dev.luminous.mod.commands.impl.TCommand;
import dev.luminous.mod.commands.impl.TeleportCommand;
import dev.luminous.mod.commands.impl.ToggleCommand;
import dev.luminous.mod.commands.impl.TradeCommand;
import dev.luminous.mod.commands.impl.WatermarkCommand;
import dev.luminous.mod.commands.impl.XrayCommand;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.HashMap;
import net.minecraft.text.Text;

public class CommandManager
implements Wrapper {
    private final HashMap<String, Command> commands = new HashMap();

    public CommandManager() {
        this.init();
    }

    public void init() {
        this.registerCommand(new AimCommand());
        this.registerCommand(new BindCommand());
        this.registerCommand(new BindsCommand());
        this.registerCommand(new CleanerCommand());
        this.registerCommand(new ClipCommand());
        this.registerCommand(new EsuCommand());
        this.registerCommand(new FakePlayerCommand());
        this.registerCommand(new FriendCommand());
        this.registerCommand(new XrayCommand());
        this.registerCommand(new GamemodeCommand());
        this.registerCommand(new KitCommand());
        this.registerCommand(new LoadCommand());
        this.registerCommand(new PingCommand());
        this.registerCommand(new PrefixCommand());
        this.registerCommand(new RejoinCommand());
        this.registerCommand(new ReloadCommand());
        this.registerCommand(new SaveCommand());
        this.registerCommand(new TeleportCommand());
        this.registerCommand(new TCommand());
        this.registerCommand(new ToggleCommand());
        this.registerCommand(new TradeCommand());
        this.registerCommand(new WatermarkCommand());
        this.registerCommand(new PeekCommand());
        this.registerCommand(new GcCommand());
    }

    public static void sendMessage(String message) {
        mc.execute(() -> {
            if (Module.nullCheck()) {
                return;
            }
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                CommandManager.mc.inGameHud.getChatHud().addMessage(Text.of((String)message));
                return;
            }
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                CommandManager.mc.inGameHud.getChatHud().addMessage(Text.of((String)("\u00a7f[\u00a7b" + ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f] " + message)));
                return;
            }
            Text text = Text.of((String)(ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f " + message));
            if (CommandManager.mc.inGameHud.getChatHud() instanceof IChatHudHook hook) {
                hook.alienClient$addMessage(text);
            } else {
                CommandManager.mc.inGameHud.getChatHud().addMessage(text);
            }
        });
    }

    public static void sendMessageId(String message, int id) {
        mc.execute(() -> {
            if (Module.nullCheck()) {
                return;
            }
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
                addMessageWithFallback(Text.of((String)message), id);
                return;
            }
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                addMessageWithFallback(Text.of((String)("\u00a7f[\u00a7b" + ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f] " + message)), id);
                return;
            }
            addMessageWithFallback(Text.of((String)(ClientSetting.INSTANCE.hackName.getValue() + "\u00a7f " + message)), id);
        });
    }

    public static void sendChatMessageWidthIdNoSync(String message, int id) {
        mc.execute(() -> {
            if (Module.nullCheck()) {
                return;
            }
            addMessageWithFallback(Text.of((String)("\u00a7f" + message)), id);
        });
    }

    private static void addMessageWithFallback(Text text, int id) {
        if (CommandManager.mc.inGameHud.getChatHud() instanceof IChatHudHook hook) {
            hook.alienClient$addMessage(text, id);
        } else {
            CommandManager.mc.inGameHud.getChatHud().addMessage(text);
        }
    }

    private void registerCommand(Command command) {
        this.commands.put(command.getName(), command);
    }

    public Command getCommandBySyntax(String string) {
        return this.commands.get(string);
    }

    public HashMap<String, Command> getCommands() {
        return this.commands;
    }

    public void command(String[] commandIn) {
        Command command = this.commands.get(commandIn[0].substring(Alien.getPrefix().length()).toLowerCase());
        if (command == null) {
            CommandManager.sendMessage("\u00a74Invalid Command!");
        } else {
            String[] parameterList = new String[commandIn.length - 1];
            System.arraycopy(commandIn, 1, parameterList, 0, commandIn.length - 1);
            if (parameterList.length == 1 && parameterList[0].equals("help")) {
                command.sendUsage();
                return;
            }
            command.runCommand(parameterList);
        }
    }
}


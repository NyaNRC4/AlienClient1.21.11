package dev.luminous.mod.modules;

import dev.luminous.Alien;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.modules.impl.client.*;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module extends Mod {

    public final BooleanSetting drawn;
    private final List<Setting> settings = new ArrayList<>();
    private final String description;
    private final Category category;
    private final BindSetting bindSetting;
    protected boolean state;
    private String chinese;

    public Module(String name, Category category) {
        this(name, "", category);
    }

    public Module(String name, String description, Category category) {
        super(name);
        this.category = category;
        this.description = description;
        this.bindSetting = add(new BindSetting("Key", isGuiModule() ? GLFW.GLFW_KEY_RIGHT_SHIFT : -1));
        this.drawn = add(new BooleanSetting("Drawn", !hideInModuleList()));
    }

    private boolean isGuiModule() {
        return this instanceof ClickGui;
    }

    private boolean hideInModuleList() {
        return this instanceof ColorsModule || this instanceof BaritoneModule || this instanceof AntiCheat || this instanceof ClientSetting || this instanceof HUD;
    }

    public void setChinese(String chinese) {
        this.chinese = chinese;
    }

    public String getArrayName() {
        return getDisplayName() + getArrayInfo();
    }

    public String getArrayInfo() {
        return (getInfo() == null ? "" : " §7[§f" + getInfo() + "§7]");
    }

    public String getInfo() {
        return null;
    }

    public String getDisplayName() {
        if (ClientSetting.INSTANCE.chinese.getValue() && chinese != null) {
            return chinese;
        }
        return getName();
    }

    public String getDescription() {
        return this.description;
    }

    public Module.Category getCategory() {
        return this.category;
    }

    public BindSetting getBindSetting() {
        return this.bindSetting;
    }

    public boolean isOn() {
        return this.state;
    }

    public boolean isOff() {
        return !isOn();
    }

    public void toggle() {
        if (this.isOn()) {
            disable();
        } else {
            enable();
        }
    }

    public void enable() {
        if (this.state) return;
        if (!nullCheck() && drawn.getValue()) {
            if (ClientSetting.INSTANCE.toggle.getValue()) {
                int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : hashCode();
                switch (ClientSetting.INSTANCE.messageStyle.getValue()) {
                    case Mio -> CommandManager.sendMessageId("§2[+] §f" + getDisplayName(), id);
                    case Debug ->
                            CommandManager.sendMessageId(getCategory().name().toLowerCase() + "." + getDisplayName().toLowerCase() + ".§aenable", id);
                    case Lowercase -> CommandManager.sendMessageId(getDisplayName().toLowerCase() + " §aenabled", id);
                    case Melon -> CommandManager.sendMessageId("§b" + getDisplayName() + " §aEnabled.", id);
                    case Normal -> CommandManager.sendMessageId("§f" + getDisplayName() + " §aEnabled", id);
                    case Future -> CommandManager.sendMessageId("§7" + getDisplayName() + " toggled §aon", id);
                    case Chinese -> CommandManager.sendMessageId(getDisplayName() + " §a开启", id);
                    case Moon ->
                            CommandManager.sendChatMessageWidthIdNoSync("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [" + "§3" + getDisplayName() + "§f]" + " §7toggled §aon", id);
                    case Earth ->
                            CommandManager.sendChatMessageWidthIdNoSync("§l" + getDisplayName() + " §aenabled.", id);
                }
            }
        }
        this.state = true;
        Alien.EVENT_BUS.subscribe(this);
        this.onToggle();
        this.onEnable();
    }

    public void disable() {
        if (!this.state) return;
        if (!nullCheck() && drawn.getValue()) {
            if (ClientSetting.INSTANCE.toggle.getValue()) {
                int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : hashCode();
                switch (ClientSetting.INSTANCE.messageStyle.getValue()) {
                    case Mio -> CommandManager.sendMessageId("§4[-] §f" + getDisplayName(), id);
                    case Debug ->
                            CommandManager.sendMessageId(getCategory().name().toLowerCase() + "." + getDisplayName().toLowerCase() + ".§cdisable", id);
                    case Lowercase -> CommandManager.sendMessageId(getDisplayName().toLowerCase() + " §cdisabled", id);
                    case Normal -> CommandManager.sendMessageId("§f" + getDisplayName() + " §cDisabled", id);
                    case Melon -> CommandManager.sendMessageId("§b" + getDisplayName() + " §cDisabled.", id);
                    case Future -> CommandManager.sendMessageId("§7" + getDisplayName() + " toggled §coff", id);
                    case Earth ->
                            CommandManager.sendChatMessageWidthIdNoSync("§l" + getDisplayName() + " §cdisabled.", id);
                    case Chinese -> CommandManager.sendMessageId(getDisplayName() + " §c关闭", id);
                    case Moon ->
                            CommandManager.sendChatMessageWidthIdNoSync("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [" + "§3" + getDisplayName() + "§f]" + " §7toggled §coff", id);
                }
            }
        }
        this.state = false;
        Alien.EVENT_BUS.unsubscribe(this);
        this.onToggle();
        this.onDisable();
    }

    public void sendMessage(String message) {
        CommandManager.sendMessage(message);
    }

    public void setState(boolean state) {
        if (this.state == state) return;
        if (state) {
            enable();
        } else {
            disable();
        }
    }

    public boolean setBind(String rkey) {
        if (rkey.equalsIgnoreCase("none")) {
            this.bindSetting.setValue(-1);
            return true;
        }
        int key;
        try {
            key = InputUtil.fromTranslationKey("key.keyboard." + rkey.toLowerCase()).getCode();
        } catch (NumberFormatException e) {
            if (!nullCheck()) sendMessage("§4Bad bind!");
            return false;
        }
        if (rkey.equalsIgnoreCase("none")) {
            key = -1;
        }
        if (key == 0) {
            return false;
        }
        this.bindSetting.setValue(key);
        return true;
    }

    public void onDisable() {

    }

    public void onEnable() {

    }

    public void onToggle() {

    }

    public void onLogin() {

    }

    public void onLogout() {

    }

    public void onRender2D(DrawContext drawContext, float tickDelta) {

    }

    public void onRender3D(MatrixStack matrixStack) {

    }

    public boolean onRender3DQueue(MatrixStack matrixStack) {
        return false;
    }

    public void addSetting(Setting setting) {
        this.settings.add(setting);
    }

    public StringSetting add(StringSetting setting) {
        addSetting(setting);
        return setting;
    }

    public ColorSetting add(ColorSetting setting) {
        addSetting(setting);
        return setting;
    }

    public SliderSetting add(SliderSetting setting) {
        addSetting(setting);
        return setting;
    }

    public BooleanSetting add(BooleanSetting setting) {
        addSetting(setting);
        return setting;
    }

    public <T extends Enum<T>> EnumSetting<T> add(EnumSetting<T> setting) {
        addSetting(setting);
        return setting;
    }

    public BindSetting add(BindSetting setting) {
        addSetting(setting);
        return setting;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public static boolean nullCheck() {
        return mc.player == null || mc.player.input == null || mc.world == null;
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }

    public enum Category {
        Combat {
            @Override
            public String getIcon() {
                return "b";
            }
        }, Misc {
            @Override
            public String getIcon() {
                return "[";
            }
        }, Render {
            @Override
            public String getIcon() {
                return "a";
            }
        }, Movement {
            @Override
            public String getIcon() {
                return "8";
            }
        }, Player {
            @Override
            public String getIcon() {
                return "5";
            }
        }, Exploit {
            @Override
            public String getIcon() {
                return "6";
            }
        }, Client {
            @Override
            public String getIcon() {
                return "7";
            }
        };

        public abstract String getIcon();
    }
}

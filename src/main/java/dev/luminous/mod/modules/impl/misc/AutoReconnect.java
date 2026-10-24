package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.ServerConnectBeginEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;

import java.util.HashMap;

public class AutoReconnect extends Module {
    public static AutoReconnect INSTANCE;
    public final BooleanSetting rejoin = add(new BooleanSetting("Rejoin", true));
    public final SliderSetting delay =
            add(new SliderSetting("Delay", 5, 0, 20, .1).setSuffix("s"));
    public final BooleanSetting autoLogin = add(new BooleanSetting("AutoAuth", true));
    public final SliderSetting afterLoginTime =
            add(new SliderSetting("AfterLoginTime", 3, 0, 10, .1).setSuffix("s"));
    public final BooleanSetting autoQueue = add(new BooleanSetting("AutoQueue", true));
    public final SliderSetting joinQueueDelay =
            add(new SliderSetting("JoinQueueDelay", 3, 0, 10, .1).setSuffix("s"));
    final StringSetting password = add(new StringSetting("password", "123456"));
    public final BooleanSetting autoAnswer = add(new BooleanSetting("AutoAnswer", true));
    public static boolean inQueueServer;
    private final Timer queueTimer = new Timer();
    private final Timer timer = new Timer();
    public Pair<ServerAddress, ServerInfo> lastServerConnection;
    private boolean login = false;

    public AutoReconnect() {
        super("AutoReconnect", Category.Misc);
        setChinese("自动重连");
        INSTANCE = this;
        Alien.EVENT_BUS.subscribe(new StaticListener());
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (login && timer.passedS(afterLoginTime.getValue())) {
            mc.getNetworkHandler().sendChatCommand("login " + password.getValue());
            login = false;
        }
        if (autoQueue.getValue() && InventoryUtil.findItem(Items.COMPASS) != -1 && queueTimer.passedS(joinQueueDelay.getValue())) {
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.COMPASS));
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            queueTimer.reset();
        }
        if (nullCheck()) {
            inQueueServer = false;
            return;
        }
        inQueueServer = InventoryUtil.findItem(Items.COMPASS) != -1;
    }

    @Override
    public void onLogin() {
        if (autoLogin.getValue()) {
            login = true;
            timer.reset();
        }
    }

    public boolean rejoin() {
        return isOn() && rejoin.getValue() && !AutoLog.loggedOut;
    }

    private class StaticListener {
        @EventListener
        private void onGameJoined(ServerConnectBeginEvent event) {
            lastServerConnection = new ObjectObjectImmutablePair<>(event.address, event.info);
        }
    }

    @Override
    public void onLogout() {
        inQueueServer = false;
    }

    @Override
    public void onDisable() {
        inQueueServer = false;
    }

    final String[] abc = new String[]{"A", "B", "C"};

    @EventListener
    public void onPacketReceive(PacketEvent.Receive e) {
        if (nullCheck()) return;
        if (!autoAnswer.getValue()) return;
        if (!inQueueServer) return;
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            for (String key : asks.keySet()) {
                if (packet.content().getString().contains(key)) {
                    for (String s : abc) {
                        if (packet.content().getString().contains(s + "." + asks.get(key))) {
                            mc.getNetworkHandler().sendChatMessage(s.toLowerCase());
                            return;
                        }
                    }
                }
            }
        }
    }

    public static final HashMap<String, String> asks = new HashMap<>() {
        {
            put("红石火把", "15");
            put("猪被闪电", "僵尸猪人");
            put("小箱子能", "27");
            put("开服年份", "2020");
            put("定位末地遗迹", "0");
            put("爬行者被闪电", "高压爬行者");
            put("大箱子能", "54");
            put("羊驼会主动", "不会");
            put("无限水", "3");
            put("挖掘速度最快", "金镐");
            put("凋灵死后", "下界之星");
            put("苦力怕的官方", "爬行者");
            put("南瓜的生长", "不需要");
            put("定位末地", "0");
        }
    };
}
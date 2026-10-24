package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;

public class Spammer extends Module {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public final BooleanSetting checkSelf =
            add(new BooleanSetting("CheckSelf", false));
    final StringSetting message = add(new StringSetting("Message", "最强外挂Alien社区版免费试用 群\uD835\uDFF1\uD835\uDFF4\uD835\uDFF5\uD835\uDFED\uD835\uDFF5\uD835\uDFED\uD835\uDFF1\uD835\uDFF2\uD835\uDFED"));
    private final Random random = new Random();
    private final SliderSetting randoms =
            add(new SliderSetting("Random", 3, 0, 20, 1));
    private final SliderSetting delay =
            add(new SliderSetting("Delay", 5, 0, 60, 0.1).setSuffix("s"));
    private final BooleanSetting tellMode =
            add(new BooleanSetting("RandomWhisper", false));
    private final BooleanSetting autoDisable =
            add(new BooleanSetting("AutoDisable", true));
    private final Timer timer = new Timer();

    public Spammer() {
        super("Spammer", Category.Misc);
        setChinese("自动刷屏");
    }

    @Override
    public void onLogout() {
        if (autoDisable.getValue()) {
            disable();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!timer.passedS(delay.getValue())) return;
        timer.reset();
        String randomString = generateRandomString(randoms.getValueInt());
        if (!randomString.isEmpty()) {
            randomString = " " + randomString;
        }
        if (tellMode.getValue()) {
            Collection<PlayerListEntry> players = mc.getNetworkHandler().getPlayerList();
            List<PlayerListEntry> list = new ArrayList<>(players);
            int size = list.size();
            if (size == 0) {
                return;
            }
            PlayerListEntry playerListEntry = list.get(random.nextInt(size));
            int i = 0;
            while (checkSelf.getValue() && Objects.equals(playerListEntry.getProfile().name(), mc.player.getGameProfile().name())) {
                if (i > 50) return;
                i++;
                playerListEntry = list.get(random.nextInt(size));
            }
            mc.getNetworkHandler().sendChatCommand("tell " + playerListEntry.getProfile().name() + " " + message.getValue() + randomString);
        } else {
            if (message.getValue().startsWith("/")) {
                mc.getNetworkHandler().sendChatCommand(message.getValue().replaceFirst("/", "") + randomString);
            } else {
                mc.getNetworkHandler().sendChatMessage(message.getValue() + randomString);
            }
        }
    }

    private String generateRandomString(int LENGTH) {
        StringBuilder sb = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }
}
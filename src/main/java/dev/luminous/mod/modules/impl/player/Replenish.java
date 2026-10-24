package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class Replenish extends Module {
    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.QuickMove));
    private final SliderSetting delay = add(new SliderSetting("Delay", 2, 0, 5, 0.01).setSuffix("s"));
    private final SliderSetting min = add(new SliderSetting("Min", 50, 1, 100)).setSuffix("%");
    private final SliderSetting forceDelay = add(new SliderSetting("ForceDelay", 0.2, 0, 4, 0.01).setSuffix("s"));
    private final SliderSetting forceMin = add(new SliderSetting("ForceMin", 16, 1, 100)).setSuffix("%");
    private final Timer timer = new Timer();

    public Replenish() {
        super("Replenish", Category.Player);
        setChinese("物品栏补充");
    }

    public enum Mode {
        QuickMove,
        ClickSlot,
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        /*        if (mc.currentScreen != null && !(mc.currentScreen instanceof ClickGuiScreen)) return;*/
        for (int i = 0; i < 9; ++i) {
            if (replenish(i)) {
                timer.reset();
                return;
            }
        }
    }

    private boolean replenish(int slot) {
        ItemStack stack = mc.player.getInventory().getStack(slot);

        if (stack.isEmpty()) return false;
        if (!stack.isStackable()) return false;

        int percent = (int) ((double) stack.getCount() / stack.getMaxCount() * 100);

        if (percent > min.getValue()) return false;

        for (int i = 9; i < 36; ++i) {
            ItemStack item = mc.player.getInventory().getStack(i);
            if (item.isEmpty() || !Sorter.canMerge(stack, item)) continue;
            if (percent > forceMin.getValueFloat()) {
                if (!timer.passedS(delay.getValue())) {
                    return false;
                }
            } else {
                if (!timer.passedS(forceDelay.getValue())) {
                    return false;
                }
            }

            switch (mode.getValue()) {
                case QuickMove ->
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                case ClickSlot -> {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                }
            }
            return true;
        }
        return false;
    }
}
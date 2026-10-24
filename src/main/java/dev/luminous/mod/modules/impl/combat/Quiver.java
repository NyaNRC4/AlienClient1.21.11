package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class Quiver extends Module {
    public Quiver() {
        super("Quiver", Category.Combat);
        setChinese("头顶射箭");
    }

    private final BooleanSetting instant = add(new BooleanSetting("InstantRotate", true));
    private final SliderSetting time = add(new SliderSetting("Time", 0.11f, 0.0f, 1f, .01));
    private final BooleanSetting onlyPress = add(new BooleanSetting("OnlyPress", false));
    private final BindSetting key =
            add(new BindSetting("ActiveKey", -1));
    boolean bow = false;
    boolean pressed = false;
    boolean switching = false;
    int startSlot;

    @Override
    public void onEnable() {
        bow = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (key.isPressed()) {
            if (!pressed && !switching) {
                int bow = InventoryUtil.findItem(Items.BOW);
                if (bow != -1) {
                    startSlot = mc.player.getInventory().selectedSlot;
                    InventoryUtil.switchToSlot(bow);
                    mc.options.useKey.setPressed(true);
                    switching = true;
                    pressed = true;
                }
            }
        } else {
            pressed = false;
        }
        if (switching && (!mc.options.useKey.isPressed() || mc.player.isUsingItem() && mc.player.getActiveItem().getItem() != Items.BOW)) {
            InventoryUtil.switchToSlot(startSlot);
            switching = false;
        }
        bow = mc.player.isUsingItem() && (mc.player.getActiveHand() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack()).getItem() instanceof BowItem;
        if (bow && (!onlyPress.getValue() || switching) && BowItem.getPullProgress(mc.player.getItemUseTime()) >= time.getValue()) {
            if (instant.getValue()) {
                Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, -90);
            }
            mc.options.useKey.setPressed(false);
            mc.interactionManager.stopUsingItem(mc.player);
            if (instant.getValue()) {
                Alien.ROTATION.snapBack();
            }
        }
    }

    @EventListener
    public void onRotate(UpdateRotateEvent event) {
        if (bow && !instant.getValue())
            event.setPitch(-90);
    }
}

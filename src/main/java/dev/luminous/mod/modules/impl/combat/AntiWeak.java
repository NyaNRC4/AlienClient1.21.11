package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.tag.ItemTags;

public class AntiWeak extends Module {
    private final EnumSetting<SwapMode> swapMode =
            add(new EnumSetting<>("SwapMode", SwapMode.Inventory));
    private final SliderSetting delay = add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
    private final BooleanSetting onlyCrystal =
            add(new BooleanSetting("OnlyCrystal", true));
    private final Timer delayTimer = new Timer();
    boolean ignore = false;
    private PlayerInteractEntityC2SPacket lastPacket = null;

    public AntiWeak() {
        super("AntiWeak", Category.Combat);
        setChinese("反虚弱");
    }

    @Override
    public String getInfo() {
        return swapMode.getValue().name();
    }

    @EventListener(priority=200)
    public void onPacketSend(PacketEvent.Send event) {
        PlayerInteractEntityC2SPacket packet;
        if (AntiWeak.nullCheck()) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (this.ignore) {
            return;
        }
        if (AntiWeak.mc.player.getStatusEffect(StatusEffects.WEAKNESS) == null) {
            return;
        }
        if (AntiWeak.mc.player.getMainHandStack().isIn(ItemTags.SWORDS)) {
            return;
        }
        if (!this.delayTimer.passedMs(this.delay.getValue())) {
            return;
        }
        Packet<?> packet2 = event.getPacket();
        if (packet2 instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystalEntity)) {
                return;
            }
            this.lastPacket = packet;
            this.delayTimer.reset();
            this.ignore = true;
            this.doAnti();
            this.ignore = false;
            event.cancel();
        }
    }

    private void doAnti() {
        if (this.lastPacket == null) {
            return;
        }
        int strong = this.swapMode.getValue() != SwapMode.Inventory ? InventoryUtil.findSword() : InventoryUtil.findSwordInventorySlot();
        if (strong == -1) {
            return;
        }
        int old = AntiWeak.mc.player.getInventory().selectedSlot;
        if (this.swapMode.getValue() != SwapMode.Inventory) {
            InventoryUtil.switchToSlot(strong);
        } else {
            InventoryUtil.inventorySwap(strong, AntiWeak.mc.player.getInventory().selectedSlot);
        }
        mc.getNetworkHandler().sendPacket((Packet)this.lastPacket);
        if (this.swapMode.getValue() != SwapMode.Inventory) {
            if (this.swapMode.getValue() != SwapMode.Normal) {
                InventoryUtil.switchToSlot(old);
            }
        } else {
            InventoryUtil.inventorySwap(strong, AntiWeak.mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        }
    }

    public static enum SwapMode {
        Normal,
        Silent,
        Inventory;

    }
}

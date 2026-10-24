package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

public class InteractTweaks
extends Module {
    public static InteractTweaks INSTANCE;
    public final BooleanSetting noEntityTrace =
            add(new BooleanSetting("NoEntityTrace", true).setParent());
    public final BooleanSetting onlyPickaxe =
            add(new BooleanSetting("OnlyPickaxe", true, noEntityTrace::isOpen));
    public final BooleanSetting multiTask =
            add(new BooleanSetting("MultiTask", true));
    public final BooleanSetting respawn =
            add(new BooleanSetting("Respawn", true));
    public final BooleanSetting ghostHand =
            add(new BooleanSetting("IgnoreBedrock", false));
    private final BooleanSetting noAbort =
            add(new BooleanSetting("NoMineAbort", false));
    private final BooleanSetting noReset =
            add(new BooleanSetting("NoMineReset", false));
    private final BooleanSetting noDelay =
            add(new BooleanSetting("NoMineDelay", false));
    private final BooleanSetting noInteract =
            add(new BooleanSetting("NoInteract", false));
    private final BooleanSetting pickaxeSwitch =
            add(new BooleanSetting("SwitchEat", false).setParent());
    private final BooleanSetting allowSword =
            add(new BooleanSetting("Sword", true, pickaxeSwitch::isOpen));
    private final BooleanSetting allowPickaxe =
            add(new BooleanSetting("Pickaxe", true, pickaxeSwitch::isOpen));
    private final BooleanSetting reach =
            add(new BooleanSetting("Reach", false));
    public final SliderSetting blockRange = add(new SliderSetting("BlockRange", 5, 0, 15, 0.1, reach::getValue));
    public final SliderSetting entityRange = add(new SliderSetting("EntityRange", 5, 0, 15, 0.1, reach::getValue));

    private final SliderSetting delay = add(new SliderSetting("UseDelay", 4, 0, 4, 1));
    public boolean isActive;
    boolean swapped = false;
    int lastSlot = 0;

    public InteractTweaks() {
        super("InteractTweaks", Category.Player);
        setChinese("交互调整");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.respawn.getValue() && InteractTweaks.mc.currentScreen instanceof DeathScreen) {
            InteractTweaks.mc.player.requestRespawn();
            mc.setScreen(null);
        }
        if (InteractTweaks.mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
            InteractTweaks.mc.itemUseCooldown = 0;
        }
        if (this.pickaxeSwitch.getValue()) {
            if (!(InteractTweaks.mc.player.getMainHandStack().isIn(ItemTags.PICKAXES) && this.allowPickaxe.getValue() || InteractTweaks.mc.player.getMainHandStack().isIn(ItemTags.SWORDS) && this.allowSword.getValue() || InteractTweaks.mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE || InteractTweaks.mc.player.getMainHandStack().getItem() == Items.GOLDEN_APPLE)) {
                this.swapped = false;
                return;
            }
            int gapple = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (gapple == -1) {
                gapple = InventoryUtil.findItem(Items.GOLDEN_APPLE);
            }
            if (gapple == -1) {
                if (this.swapped) {
                    InventoryUtil.switchToSlot(this.lastSlot);
                    this.swapped = false;
                }
                return;
            }
            if (InteractTweaks.mc.options.useKey.isPressed()) {
                if ((InteractTweaks.mc.player.getMainHandStack().isIn(ItemTags.PICKAXES) && this.allowPickaxe.getValue() || InteractTweaks.mc.player.getMainHandStack().isIn(ItemTags.SWORDS) && this.allowSword.getValue()) && InteractTweaks.mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE && InteractTweaks.mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE) {
                    this.lastSlot = InteractTweaks.mc.player.getInventory().selectedSlot;
                    InventoryUtil.switchToSlot(gapple);
                    this.swapped = true;
                }
            } else if (this.swapped) {
                InventoryUtil.switchToSlot(this.lastSlot);
                this.swapped = false;
            }
        }
    }

    @EventListener
    public void onPacket(PacketEvent.Send event) {
        Packet<?> packet;
        if (InteractTweaks.nullCheck() || !this.noInteract.getValue() || !((packet = event.getPacket()) instanceof PlayerInteractBlockC2SPacket)) {
            return;
        }
        PlayerInteractBlockC2SPacket packet2 = (PlayerInteractBlockC2SPacket)packet;
        Block block = InteractTweaks.mc.world.getBlockState(packet2.getBlockHitResult().getBlockPos()).getBlock();
        if (!InteractTweaks.mc.player.isSneaking() && (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AnvilBlock)) {
            event.cancel();
        }
    }

    @Override
    public void onDisable() {
        this.isActive = false;
    }

    public boolean reach() {
        return this.isOn() && this.reach.getValue();
    }

    public boolean noAbort() {
        return this.isOn() && this.noAbort.getValue() && !InteractTweaks.mc.options.useKey.isPressed();
    }

    public boolean noReset() {
        return this.isOn() && this.noReset.getValue();
    }

    public boolean noDelay() {
        return this.isOn() && this.noDelay.getValue();
    }

    public boolean multiTask() {
        return this.isOn() && this.multiTask.getValue();
    }

    public boolean noEntityTrace() {
        if (this.isOff() || !this.noEntityTrace.getValue()) {
            return false;
        }
        if (this.onlyPickaxe.getValue()) {
            return InteractTweaks.mc.player.getMainHandStack().isIn(ItemTags.PICKAXES) || InteractTweaks.mc.player.isUsingItem() && !(InteractTweaks.mc.player.getMainHandStack().isIn(ItemTags.SWORDS));
        }
        return true;
    }

    public boolean ghostHand() {
        return this.isOn() && this.ghostHand.getValue() && !InteractTweaks.mc.options.useKey.isPressed() && !InteractTweaks.mc.options.sneakKey.isPressed();
    }
}


package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoPot extends Module {

    public static AutoPot INSTANCE;
    private final SliderSetting delay = add(new SliderSetting("Delay", 5, 0, 10, 0.1).setSuffix("s"));
    private final BooleanSetting speed =
            add(new BooleanSetting("Speed", false));
    private final BooleanSetting resistance =
            add(new BooleanSetting("Resistance", false));
    private final BooleanSetting strength =
            add(new BooleanSetting("Strength", false));
    private final BooleanSetting slowFalling =
            add(new BooleanSetting("SlowFalling", false));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround =
            add(new BooleanSetting("OnlyGround", false));
    private final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true));
    private final BindSetting speedKey =
            add(new BindSetting("SpeedKey", -1));
    private final BindSetting strengthKey =
            add(new BindSetting("StrengthKey", -1));
    private final BindSetting resistanceKey =
            add(new BindSetting("ResistanceKey", -1));
    private final Timer delayTimer = new Timer();
    private boolean throwing = false;
    private boolean turtlePress;
    private boolean speedPress;
    private boolean strengthPress;

    public AutoPot() {
        super("AutoPot", Category.Player);
        setChinese("自动药水");
        INSTANCE = this;
        Alien.EVENT_BUS.subscribe(new AutoPotTick());
    }

    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        for (int i = 35; i >= 0; --i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() == targetEffect) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() == targetEffect) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        throwing = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (inventory.getValue() && !EntityUtil.inInventory()) return;
        if (!delayTimer.passedMs(delay.getValue() * 1000)) return;
        if (!onlyGround.getValue() || (mc.player.isOnGround() || Alien.PLAYER.isInWeb(mc.player)) && !mc.world.isAir(new BlockPosX(mc.player.getEntityPos().add(0, -1, 0)))) {
            if (resistance.getValue() && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2)) {
                throwing = checkThrow(StatusEffects.RESISTANCE.value());
                if (isThrow()) {
                    throwPotion(StatusEffects.RESISTANCE.value());
                    return;
                }
            }
            if (speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                throwing = checkThrow(StatusEffects.SPEED.value());
                if (isThrow()) {
                    throwPotion(StatusEffects.SPEED.value());
                    return;
                }
            }
            if (strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                throwing = checkThrow(StatusEffects.STRENGTH.value());
                if (isThrow()) {
                    throwPotion(StatusEffects.STRENGTH.value());
                    return;
                }
            }
            if (slowFalling.getValue() && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                throwing = checkThrow(StatusEffects.SLOW_FALLING.value());
                if (isThrow()) {
                    throwPotion(StatusEffects.SLOW_FALLING.value());
                }
            }
        }
    }

    public void throwPotion(StatusEffect targetEffect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
            Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90);
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            Alien.ROTATION.snapBack();
            delayTimer.reset();
        } else if ((newSlot = findPotion(targetEffect)) != -1) {
            Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90);
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            Alien.ROTATION.snapBack();
            delayTimer.reset();
        }
    }

    public boolean isThrow() {
        return throwing;
    }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (!EntityUtil.inInventory()) {
            return false;
        }
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return false;
        }
        return findPotion(targetEffect) != -1 || (inventory.getValue() && findPotionInventorySlot(targetEffect) != -1);
    }

    public class AutoPotTick {

        @EventListener
        public void onTick(ClientTickEvent event) {
            if (nullCheck() || event.isPost()) return;
            if (inventory.getValue() && !EntityUtil.inInventory()) return;
            if (mc.currentScreen == null) {
                if (resistanceKey.isPressed()) {
                    if (!turtlePress) {
                        if (checkThrow(StatusEffects.RESISTANCE.value())) {
                            throwPotion(StatusEffects.RESISTANCE.value());
                            turtlePress = true;
                            return;
                        }
                    }
                } else {
                    turtlePress = false;
                }

                if (strengthKey.isPressed()) {
                    if (!strengthPress) {
                        if (checkThrow(StatusEffects.STRENGTH.value())) {
                            throwPotion(StatusEffects.STRENGTH.value());
                            strengthPress = true;
                            return;
                        }
                    }
                } else {
                    strengthPress = false;
                }

                if (speedKey.isPressed()) {
                    if (!speedPress) {
                        if (checkThrow(StatusEffects.SPEED.value())) {
                            throwPotion(StatusEffects.SPEED.value());
                            speedPress = true;
                        }
                    }
                } else {
                    speedPress = false;
                }
            } else {
                speedPress = false;
                turtlePress = false;
                strengthPress = false;
            }
        }
    }
}

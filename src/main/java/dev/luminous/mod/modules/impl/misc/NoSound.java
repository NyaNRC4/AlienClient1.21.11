package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;

public class NoSound extends Module {
    static final ArrayList<SoundEvent> armor = new ArrayList<>();
    public static NoSound INSTANCE;

    static {
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_TURTLE.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_IRON.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value());
        armor.add(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value());
    }

    private final BooleanSetting equip =
            add(new BooleanSetting("ArmorEquip", true));
    private final BooleanSetting explode =
            add(new BooleanSetting("Explode", true));
    private final BooleanSetting attack =
            add(new BooleanSetting("Attack", true));
    private final BooleanSetting teleport =
            add(new BooleanSetting("Teleport", true));
    private final BooleanSetting throwConfig =
            add(new BooleanSetting("Throw", true));
    private final BooleanSetting potion =
            add(new BooleanSetting("Potion", true));
    private final BooleanSetting elytra =
            add(new BooleanSetting("Elytra", true));

    public NoSound() {
        super("NoSound", Category.Misc);
        setChinese("去除声音");
        INSTANCE = this;
    }

    @EventListener
    public void onPlaySound(PlaySoundEvent event) {
        if (this.equip.getValue()) {
            for (SoundEvent se : armor) {
                if (event.sound.getId() != se.id()) continue;
                event.cancel();
                return;
            }
        }
        if (this.explode.getValue() && (event.sound.getId() == ((SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value()).id() || event.sound.getId() == SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE.id())) {
            event.cancel();
            return;
        }
        if (this.attack.getValue() && (event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_WEAK.id() || event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK.id() || event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG.id())) {
            event.cancel();
        }
        if (this.teleport.getValue() && event.sound.getId() == SoundEvents.ENTITY_PLAYER_TELEPORT.id()) {
            event.cancel();
        }
        if (this.potion.getValue() && event.sound.getId() == SoundEvents.ENTITY_SPLASH_POTION_BREAK.id()) {
            event.cancel();
        }
        if (this.elytra.getValue() && event.sound.getId() == SoundEvents.ITEM_ELYTRA_FLYING.id()) {
            event.cancel();
        }
        if (this.throwConfig.getValue() && (event.sound.getId() == SoundEvents.ENTITY_ENDER_PEARL_THROW.id() || event.sound.getId() == SoundEvents.ENTITY_EGG_THROW.id() || event.sound.getId() == SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW.id() || event.sound.getId() == SoundEvents.ENTITY_SNOWBALL_THROW.id() || event.sound.getId() == SoundEvents.ENTITY_SPLASH_POTION_THROW.id())) {
            event.cancel();
        }
    }

    static {
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_TURTLE.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_IRON.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value());
        armor.add((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value());
    }
}
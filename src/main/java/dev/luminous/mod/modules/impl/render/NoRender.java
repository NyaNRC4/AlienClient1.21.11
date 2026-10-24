package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.Event;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.ParticleEvent;
import dev.luminous.api.events.impl.RenderEntityEvent;
import dev.luminous.api.events.impl.TickEntityEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.particle.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;

public class NoRender extends Module {
    public static NoRender INSTANCE;
    public final BooleanSetting fastItem = add(new BooleanSetting("2DItem", false).setParent());
    public final BooleanSetting castShadow = add(new BooleanSetting("CastShadow", true, fastItem::isOpen));
    public final BooleanSetting renderSidesOfItems = add(new BooleanSetting("RenderSidesOfItems", false, fastItem::isOpen));
    public final BooleanSetting potionsIcon = add(new BooleanSetting("PotionsIcon", false));
    public final BooleanSetting weather = add(new BooleanSetting("Weather", true));
    public final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
    public final BooleanSetting lightsUpdate = add(new BooleanSetting("LightsUpdate", false));
    public final BooleanSetting potions = add(new BooleanSetting("Potions", true));
    public final BooleanSetting xp = add(new BooleanSetting("XP", true));
    public final BooleanSetting arrows = add(new BooleanSetting("Arrows", false));
    public final BooleanSetting eggs = add(new BooleanSetting("Eggs", false));
    public final BooleanSetting item = add(new BooleanSetting("Items", false));
    public final BooleanSetting armorParts = add(new BooleanSetting("ArmorParts", false));
    public final BooleanSetting armorTrim = add(new BooleanSetting("ArmorTrim", false));
    public final BooleanSetting armorGlint = add(new BooleanSetting("ArmorGlint", false));
    public final BooleanSetting hurtCam = add(new BooleanSetting("HurtCam", true));
    public final BooleanSetting fireOverlay = add(new BooleanSetting("FireOverlay", true));
    public final BooleanSetting waterOverlay = add(new BooleanSetting("WaterOverlay", true));
    public final BooleanSetting blockOverlay = add(new BooleanSetting("BlockOverlay", true));
    public final BooleanSetting portal = add(new BooleanSetting("Portal", true));
    public final BooleanSetting totem = add(new BooleanSetting("Totem", true));
    public final BooleanSetting nausea = add(new BooleanSetting("Nausea", true));
    public final BooleanSetting blindness = add(new BooleanSetting("Blindness", true));
    public final BooleanSetting fog = add(new BooleanSetting("Fog", false));
    public final BooleanSetting darkness = add(new BooleanSetting("Darkness", true));
    public final BooleanSetting fireEntity = add(new BooleanSetting("EntityFire", true));
    public final BooleanSetting antiTitle = add(new BooleanSetting("Title", false));
    public final BooleanSetting antiPlayerCollision = add(new BooleanSetting("PlayerCollision", true));
    public final BooleanSetting effect = add(new BooleanSetting("Effect", true));
    public final BooleanSetting elderGuardian = add(new BooleanSetting("Guardian", false));
    public final BooleanSetting explosions = add(new BooleanSetting("Explosions", true));
    public final BooleanSetting campFire = add(new BooleanSetting("CampFire", false));
    public final BooleanSetting fireworks = add(new BooleanSetting("Fireworks", false));
    public final BooleanSetting guiToast = add(new BooleanSetting("GuiToast", false));

    public NoRender() {
        super("NoRender", "Disables all overlays and potion effects.", Category.Render);
        setChinese("禁用渲染");
        INSTANCE = this;
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof TitleS2CPacket && antiTitle.getValue()) {
            event.setCancelled(true);
        }
    }

    @EventListener
    public void onRender(TickEntityEvent event) {
        cancelEvent(event.getEntity(), event);
    }

    @EventListener
    public void onRender(RenderEntityEvent event) {
        cancelEvent(event.getEntity(), event);
    }

    private void cancelEvent(Entity entity, Event event) {
        if (entity instanceof PotionEntity && potions.getValue()) {
            event.cancel();
        } else if (entity instanceof ExperienceBottleEntity && xp.getValue()) {
            event.cancel();
        } else if (entity instanceof ArrowEntity && arrows.getValue()) {
            event.cancel();
        } else if (entity instanceof EggEntity && eggs.getValue()) {
            event.cancel();
        } else if (entity instanceof ItemEntity && item.getValue()) {
            event.cancel();
        }
    }

    @EventListener
    public void onParticle(ParticleEvent event) {
        if (elderGuardian.getValue() && event.particle instanceof ElderGuardianParticle) {
            event.cancel();
        } else if (explosions.getValue() && event.particle instanceof ExplosionLargeParticle) {
            event.cancel();
        } else if (explosions.getValue() && event.particle instanceof ExplosionSmokeParticle) {
            event.cancel();
        } else if (explosions.getValue() && event.particle instanceof ExplosionEmitterParticle) {
            event.cancel();
        } else if (explosions.getValue() && event.particle instanceof FireSmokeParticle) {
            event.cancel();
        } else if (campFire.getValue() && event.particle instanceof CampfireSmokeParticle) {
            event.cancel();
        } else if (fireworks.getValue() && (event.particle instanceof FireworksSparkParticle.FireworkParticle || event.particle instanceof FireworksSparkParticle.Flash)) {
            event.cancel();
        } else if (effect.getValue() && event.particle instanceof SpellParticle) {
            event.cancel();
        }
    }
}
package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.render.Crosshair;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Objects;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.portal.getValue()) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(CallbackInfo info) {
        if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.potionsIcon.getValue()) info.cancel();
    }

    @Unique
    final
    Color minColor = new Color(196, 0, 0);
    @Unique
    final
    Color maxColor = new Color(0, 227, 0);

    @Inject(method={"renderMainHud"}, at={@At(value="TAIL")})
    private void onRenderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PlayerEntity player;
        if (HUD.INSTANCE.isOn() && HUD.INSTANCE.armor.getValue() && (player = this.getCameraPlayer()) != null) {
            int t;
            int x = context.getScaledWindowWidth() / 2 + 91;
            int y = context.getScaledWindowHeight() - 28 - HUD.INSTANCE.armorOffset.getValueInt();
            if (this.client.interactionManager.hasStatusBars()) {
                y -= 16;
            }
            if ((t = this.getHeartCount(this.getRiddenEntity())) == 0) {
                y -= 10;
            }
            int maxAir = player.getMaxAir();
            int air = Math.min(player.getAir(), maxAir);
            if (player.isSubmergedIn(FluidTags.WATER) || air < maxAir) {
                int w = this.getHeartRows(t) - 1;
                y += w * 10;
            }
            for (net.minecraft.entity.EquipmentSlot slot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET}) {
                ItemStack armor = this.client.player.getEquippedStack(slot);
                x -= 20;
                if (armor.isEmpty()) continue;
                int damage = EntityUtil.getDamagePercent(armor);
                context.drawItem(armor, x, y);
                context.drawStackOverlay(this.client.textRenderer,armor, x, y);
                if (HUD.INSTANCE.durability.getValue()) {
                    if (HUD.INSTANCE.font.getValue()) {
                        FontManager.small.drawString(context.getMatrices(), damage + "%", (double)(x + 1), (double)((float)y - FontManager.small.getFontHeight() / 2.0f), ColorUtil.fadeColor(this.minColor, this.maxColor, (float)damage / 100.0f).getRGB(), HUD.INSTANCE.shadow.getValue());
                    } else {
                        String string = damage + "%";
                        float f = x + 2;
                        float f2 = y;
                        Objects.requireNonNull(this.client.textRenderer);
                        TextUtil.drawStringScale(context, string, f, f2 - 9.0f / 4.0f, ColorUtil.fadeColor(this.minColor, this.maxColor, (float)damage / 100.0f).getRGB(), 0.5f, HUD.INSTANCE.shadow.getValue());
                    }
                }
            }
        }
    }

    @Shadow
    private int getHeartRows(int t) {
        return 0;
    }

    @Shadow
    private int getHeartCount(LivingEntity livingEntity) {
        return 0;
    }

    @Shadow
    private PlayerEntity getCameraPlayer() {
        return null;
    }

    @Shadow
    private LivingEntity getRiddenEntity() {
        return null;
    }

    @Shadow
    public abstract void render(DrawContext context, RenderTickCounter tickCounter);

    @Inject(at = @At(value = "HEAD"), method = "render")
    public void renderStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Alien.EVENT_BUS.post(PreRender2DEvent.get(context));
    }

    @Inject(at = @At(value = "TAIL"), method = "render")
    public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Alien.MODULE.onRender2D(context);
    }

    @Inject(method = "clear", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"), cancellable = true)
    private void onClear(CallbackInfo info) {
        if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.keepHistory.getValue()) {
            info.cancel();
        }
    }

    @ModifyArg(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",
                    ordinal = 1
            ),
            index = 2
    )
    private int selectedSlotX(int x) {
        if (ClientSetting.INSTANCE.hotbar()) {
            double hotbarX = ClientSetting.animation.get(x, ClientSetting.INSTANCE.hotbarTime.getValueInt(), ClientSetting.INSTANCE.animEase.getValue());
            return (int) hotbarX;
        }
        return x;
    }


    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshairBegin(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Crosshair.INSTANCE.isOn()) {
            Crosshair.INSTANCE.draw(context);
            ci.cancel();
        }
    }
}
package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IGameRenderer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class NameTags
        extends Module {
    public static NameTags INSTANCE;
    final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
    final ColorSetting friendConfig = this.add(new ColorSetting("Friend", new Color(155, 155, 255)).injectBoolean(true));
    final ColorSetting invisibleConfig = this.add(new ColorSetting("Invisible", new Color(200, 200, 200)).injectBoolean(true));
    final ColorSetting died = this.add(new ColorSetting("Died", new Color(180, 0, 0)).injectBoolean(true));
    final ColorSetting sneakingConfig = this.add(new ColorSetting("Sneaking", new Color(200, 200, 0)).injectBoolean(true));
    final ColorSetting rectConfig = this.add(new ColorSetting("Rectangle", new Color(0, 0, 0, 100)).injectBoolean(true));
    final BooleanSetting armorConfig = this.add(new BooleanSetting("Armor", true).setParent());
    final BooleanSetting drawItemConfig = this.add(new BooleanSetting("DrawItem", true, this.armorConfig::isOpen));
    final SliderSetting offsetConfig = this.add(new SliderSetting("Offset", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
    final BooleanSetting enchantmentsConfig = this.add(new BooleanSetting("Enchantments", true));
    final BooleanSetting durabilityConfig = this.add(new BooleanSetting("Durability", true).setParent());
    final BooleanSetting forceBarConfig = this.add(new BooleanSetting("ForceBar", true, this.durabilityConfig::isOpen));
    final BooleanSetting itemNameConfig = this.add(new BooleanSetting("ItemName", false));
    final BooleanSetting entityIdConfig = this.add(new BooleanSetting("EntityId", false));
    final BooleanSetting gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
    final BooleanSetting pingConfig = this.add(new BooleanSetting("Ping", true));
    final BooleanSetting healthConfig = this.add(new BooleanSetting("Health", true));
    final BooleanSetting totemsConfig = this.add(new BooleanSetting("Totems", false));
    final SliderSetting scaleConfig = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1));
    final BooleanSetting factorConfig = this.add(new BooleanSetting("Factor", true).setParent());
    final SliderSetting scalingConfig = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
    final SliderSetting distanceConfig = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen));
    final SliderSetting heightConfig = this.add(new SliderSetting("Height", 0.0, -3.0, 3.0, 0.01));
    final DecimalFormat df = new DecimalFormat("0.0");


    public NameTags() {
        super("NameTags", "Renders info on player NameTags", Category.Render);
        setChinese("名字标签");
        INSTANCE = this;
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (NameTags.mc.gameRenderer == null || mc.getCameraEntity() == null) {
            return;
        }
        Camera camera = NameTags.mc.gameRenderer.getCamera();
        BufferAllocator allocator = new BufferAllocator(262144);
        Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> layerBuffers = new Object2ObjectLinkedOpenHashMap<>();
        List<BufferAllocator> layerAllocators = new ArrayList<>();
        addLayerBuffer(layerBuffers, layerAllocators, RenderLayers.glint());
        addLayerBuffer(layerBuffers, layerAllocators, RenderLayers.glintTranslucent());
        addLayerBuffer(layerBuffers, layerAllocators, RenderLayers.entityGlint());
        addLayerBuffer(layerBuffers, layerAllocators, RenderLayers.armorEntityGlint());
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(layerBuffers, allocator);
        OrderedRenderCommandQueueImpl queue = new OrderedRenderCommandQueueImpl();
        RenderDispatcher dispatcher = new RenderDispatcher(
                queue,
                NameTags.mc.getBlockRenderManager(),
                immediate,
                NameTags.mc.getAtlasManager(),
                NameTags.mc.getBufferBuilders().getOutlineVertexConsumers(),
                NameTags.mc.getBufferBuilders().getEffectVertexConsumers(),
                NameTags.mc.textRenderer
        );
        boolean renderedAny = false;
        GpuBufferSlice previousFog = RenderSystem.getShaderFog();
        boolean fogDisabled = false;
        try {
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
            for (PlayerEntity playerEntity : Alien.THREAD.getPlayers()) {
                if (!this.died.booleanValue && !playerEntity.isAlive() || playerEntity == NameTags.mc.player && NameTags.mc.options.getPerspective().isFirstPerson() || !this.invisibleConfig.booleanValue && playerEntity.isInvisible()) continue;
                String info = this.getNametagInfo(playerEntity);
                Vec3d renderPosition = MathUtil.getRenderPosition((Entity)playerEntity, event.tickDelta);
                double x = renderPosition.getX();
                double y = renderPosition.getY();
                double z = renderPosition.getZ();
                int width = NameTags.mc.textRenderer.getWidth(info);
                this.renderInfo(info, (float)width / 2.0f, playerEntity, x, y, z, camera, matrixStack, queue);
                renderedAny = true;
            }
            if (renderedAny) {
                if (NameTags.mc.getFramebuffer().getDepthAttachment() != null) {
                    RenderSystem.getDevice().createCommandEncoder()
                            .clearDepthTexture(NameTags.mc.getFramebuffer().getDepthAttachment(), 1.0);
                }
                FogRenderer fogRenderer = ((IGameRenderer) NameTags.mc.gameRenderer).getFogRenderer();
                RenderSystem.setShaderFog(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
                fogDisabled = true;
                dispatcher.render();
            }
        } finally {
            immediate.draw();
            if (fogDisabled) {
                RenderSystem.setShaderFog(previousFog);
            }
            for (BufferAllocator layerAllocator : layerAllocators) {
                layerAllocator.close();
            }
            allocator.close();
        }
    }

    private static void addLayerBuffer(Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> layerBuffers,
                                       List<BufferAllocator> allocators,
                                       RenderLayer layer) {
        BufferAllocator layerAllocator = new BufferAllocator(Math.max(layer.getExpectedBufferSize(), 16384));
        allocators.add(layerAllocator);
        layerBuffers.put(layer, layerAllocator);
    }

    private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices, OrderedRenderCommandQueue queue) {
        Vec3d pos = camera.getCameraPos();
        double eyeY = y + (double)entity.getHeight() + (double)(entity.isSneaking() ? 0.4f : 0.43f) + (double)this.heightConfig.getValueFloat();
        float scale = (float)((double)(-0.025f * this.scaleConfig.getValueFloat()) + (this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > (double)(this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()) ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - (double)this.distanceConfig.getValueFloat()) * (double)-0.0025f * (double)this.scalingConfig.getValueFloat() : 0.0));
        matrices.push();
        matrices.translate(x - pos.getX(), eyeY - pos.getY(), z - pos.getZ());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(scale, scale, -scale);
        if (this.rectConfig.booleanValue) {
            float paddingX = 2.0f;
            float paddingY = 1.0f;
            Render3DUtil.drawRect(matrices,
                    -width - paddingX,
                    -paddingY,
                    width * 2 + paddingX * 2,
                    10,
                    rectConfig.getValue().getRGB()
            );
        }
        this.drawWithShadow(queue, matrices, info, -width, 0.0f, this.getNametagColor(entity));
        if (this.armorConfig.getValue()) {
            this.renderItems(matrices, entity, queue);
        }
        matrices.pop();
    }


    private void drawWithShadow(OrderedRenderCommandQueue queue, MatrixStack matrices, String info, float x, float y, int color) {
        OrderedText mainText = Text.literal(info).asOrderedText();
        int shadowColor = 0x80000000;
        String plainText = info.replaceAll("§.", "");
        OrderedText shadowText = Text.literal(plainText).asOrderedText();
        queue.submitText(matrices, x + 1, y + 1, shadowText, true, TextRenderer.TextLayerType.SEE_THROUGH, 0xF000F0, shadowColor, 0, 0xF000F0);
        queue.submitText(matrices, x, y, mainText, false, TextRenderer.TextLayerType.SEE_THROUGH, 0xF000F0, color, 0, 0xF000F0);
    }


    private void renderItems(MatrixStack matrixStack, PlayerEntity player, OrderedRenderCommandQueue queue) {
        CopyOnWriteArrayList<ItemStack> displayItems = new CopyOnWriteArrayList<>();
        if (!player.getOffHandStack().isEmpty()) {
            displayItems.add(player.getOffHandStack());
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack armorStack = player.getEquippedStack(slot);
            if (!armorStack.isEmpty()) {
                displayItems.add(armorStack);
            }
        }
        if (!player.getMainHandStack().isEmpty()) {
            displayItems.add(player.getMainHandStack());
        }
        Collections.reverse(displayItems);
        float x = 0.0f;
        int n11 = 0;
        for (ItemStack itemStack : displayItems) {
            x -= 8.0f;
            if (itemStack.getEnchantments().getSize() <= n11) continue;
            n11 = itemStack.getEnchantments().getSize();
        }
        float y = this.offsetConfig.getValueFloat();
        for (ItemStack stack : displayItems) {
            if (this.drawItemConfig.getValue()) {
                this.renderItemStack(matrixStack, stack, x, y + 1.0f, queue);
            }
            this.renderItemOverlay(matrixStack, stack, x, y + 2.5f, queue);
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            if (this.durabilityConfig.getValue()) {
                this.renderDurability(matrixStack, stack, x + 2.0f, y + 11.5f, queue);
            }
            if (this.enchantmentsConfig.getValue()) {
                this.renderEnchants(matrixStack, stack, x + 2.0f, y + 7.0f, queue);
            }
            matrixStack.scale(2.0f, 2.0f, 2.0f);
            x += 16.0f;
        }
        ItemStack itemStack = player.getMainHandStack();
        if (itemStack.isEmpty()) {
            return;
        }
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        if (this.itemNameConfig.getValue()) {
            this.renderItemName(matrixStack, itemStack, y - 4.5f + this.enchantOffset(n11), queue);
        }
        matrixStack.scale(2.0f, 2.0f, 2.0f);
    }

    private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y, OrderedRenderCommandQueue queue) {
        matrixStack.push();
        matrixStack.translate(x, y, 0.0f);
        matrixStack.translate(8.0f, 8.0f, 0.0f);
        matrixStack.scale(16.0f, 16.0f, 0.00000001f);
        matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));

        ItemRenderState renderState = new ItemRenderState();
        mc.getItemModelManager().update(renderState, stack, ItemDisplayContext.GUI, mc.world, null, 0);
        renderState.render(matrixStack, queue, 0xF000F0, OverlayTexture.DEFAULT_UV, 0);


        matrixStack.pop();
    }

    private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y, OrderedRenderCommandQueue queue) {
        matrixStack.push();
        if (stack.getCount() != 1) {
            String string = String.valueOf(stack.getCount());
            this.drawWithShadow(queue, matrixStack, string, x + 17.0f - NameTags.mc.textRenderer.getWidth(string), y + 9.0f, -1);
        }
        if (stack.isItemBarVisible() || (stack.isDamageable() && this.forceBarConfig.getValue())) {
            int i = stack.getItemBarStep();
            int j = stack.getItemBarColor();
            float k = x + 2.0f;
            float l = y + 13.0f;
            Render3DUtil.drawRect(matrixStack, k, l, 13, 2, Colors.BLACK);
            Render3DUtil.drawRect(matrixStack, k, l, i, 1, j | Colors.BLACK);
        }
        matrixStack.pop();
    }

    private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y, OrderedRenderCommandQueue queue) {
        if (!itemStack.isDamageable()) {
            return;
        }
        int n = itemStack.getMaxDamage();
        int n2 = itemStack.getDamage();
        int durability = (int)((float)(n - n2) / (float)n * 100.0f);
        this.drawWithShadow(queue, matrixStack, durability + "%", x * 2.0f, y * 2.0f, ColorUtil.hslToColor((float)(n - n2) / (float)n * 120.0f, 100.0f, 50.0f, 1.0f).getRGB());
    }

    private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y, OrderedRenderCommandQueue queue) {
        if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            this.drawWithShadow(queue, matrixStack, "God", x * 2.0f, y * 2.0f, -3977663);
            return;
        }
        if (!itemStack.hasEnchantments()) {
            return;
        }
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments((ItemStack)itemStack);
        float n2 = 0.0f;
        for (RegistryEntry enchantment : enchants.getEnchantments()) {
            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = ((Enchantment)enchantment.value()).toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
                enchantString.append("§cVan");
            } else if (translatedName.contains("Bind")) {
                enchantString.append("§cBind");
            } else {
                int maxLen;
                int n = maxLen = lvl > 1 ? 2 : 3;
                if (translatedName.length() > maxLen) {
                    translatedName = translatedName.substring(0, maxLen);
                }
                enchantString.append(translatedName);
                enchantString.append(lvl);
            }
            this.drawWithShadow(queue, matrixStack, enchantString.toString(), x * 2.0f, (y + n2) * 2.0f, -1);
            n2 -= 4.5f;
        }
    }

    private float enchantOffset(int n) {
        if (!this.enchantmentsConfig.getValue() || n <= 2) {
            return 0.0f;
        }
        float value = -2.0f;
        return value -= (float)(n - 3) * 4.5f;
    }

    private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float y, OrderedRenderCommandQueue queue) {
        String itemName = itemStack.getName().getString();
        float width = (float)NameTags.mc.textRenderer.getWidth(itemName) / 4.0f;
        this.drawWithShadow(queue, matrixStack, itemName, (0.0f - width) * 2.0f, y * 2.0f, -1);
    }

    private String getNametagInfo(PlayerEntity player) {
        int totems;
        StringBuilder info = new StringBuilder();
        if (this.gamemodeConfig.getValue()) {
            if (player.isCreative()) {
                info.append(Formatting.GOLD);
                info.append("[C] ");
            } else if (player.isSpectator()) {
                info.append(Formatting.GRAY);
                info.append("[I] ");
            } else {
                info.append(Formatting.BOLD);
                info.append("[S] ");
            }
        }
        if (this.pingConfig.getValue()) {
            info.append(this.getEntityPing(player));
            info.append("ms ");
            info.append(Formatting.RESET);
        }
        info.append(player.getName().getString());
        info.append(" ");
        if (this.entityIdConfig.getValue()) {
            info.append("ID: ");
            info.append(player.getId());
            info.append(" ");
        }
        if (this.healthConfig.getValue()) {
            double health = player.getHealth() + player.getAbsorptionAmount();
            Formatting hcolor = health > 18.0 ? Formatting.GREEN : (health > 16.0 ? Formatting.DARK_GREEN : (health > 12.0 ? Formatting.YELLOW : (health > 8.0 ? Formatting.GOLD : (health > 4.0 ? Formatting.RED : Formatting.DARK_RED))));
            String phealth = this.df.format(health);
            info.append(hcolor);
            info.append(phealth);
            info.append(" ");
        }
        if (this.totemsConfig.getValue() && (totems = Alien.POP.getPop(player)) > 0) {
            Formatting c = TextRadar.getPopColor(totems);
            info.append(c);
            info.append(-totems);
            info.append(" ");
        }
        return info.toString().trim();
    }

    private String getEntityPing(PlayerEntity entity) {
        if (mc.getNetworkHandler() == null) {
            return "§7-1";
        }
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerListEntry == null) {
            return "§7-1";
        }
        int ping = playerListEntry.getLatency();
        Formatting color = ping >= 200 ? Formatting.RED : (ping >= 100 ? Formatting.YELLOW : Formatting.GREEN);
        return color.toString() + ping;
    }

    private int getNametagColor(PlayerEntity player) {
        if (this.friendConfig.booleanValue && player.getDisplayName() != null && Alien.FRIEND.isFriend(player)) {
            return this.friendConfig.getValue().getRGB();
        }
        if (this.invisibleConfig.booleanValue && player.isInvisible()) {
            return this.invisibleConfig.getValue().getRGB();
        }
        if (this.sneakingConfig.booleanValue && player.isSneaking()) {
            return this.sneakingConfig.getValue().getRGB();
        }
        if (!player.isAlive()) {
            return this.died.getValue().getRGB();
        }
        return this.colorConfig.getValue().getRGB();
    }
}

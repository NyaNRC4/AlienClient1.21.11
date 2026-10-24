package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.core.impl.BreakManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BreakESP extends Module {
    public static BreakESP INSTANCE;
    private final BooleanSetting progress = add(new BooleanSetting("Progress", true));
    private final SliderSetting damage = add(new SliderSetting("Damage", 1.0f, 0.0f, 2.0f, 0.01));
    private final ColorSetting box = add(new ColorSetting("Box", new Color(198, 176, 12, 255)).injectBoolean(true));
    private final ColorSetting fill = add(new ColorSetting("Fill", new Color(198, 176, 12, 78)).injectBoolean(true));
    private final ColorSetting boxFriend = add(new ColorSetting("FriendBox", new Color(30, 45, 169, 255)).injectBoolean(true));
    private final ColorSetting fillFriend = add(new ColorSetting("FriendFill", new Color(30, 45, 169, 78)).injectBoolean(true));
    private final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut));

    private final BooleanSetting second = add(new BooleanSetting("Second", true));
    private final ColorSetting secondBox = add(new ColorSetting("SecondBox", new Color(255, 255, 255, 255)).injectBoolean(true));
    private final ColorSetting secondFill = add(new ColorSetting("SecondFill", new Color(255, 255, 255, 100)).injectBoolean(true));
    final DecimalFormat df = new DecimalFormat("0.0");

    public BreakESP() {
        super("BreakESP", Category.Render);
        setChinese("挖掘显示");
        INSTANCE = this;
    }

    final Color startColor = new Color(255, 6, 6);
    final Color endColor = new Color(0, 255, 12);
    final Color doubleColor = new Color(255, 179, 96);

    private Color getFillColor(PlayerEntity player) {
        return Alien.FRIEND.isFriend(player) ? fillFriend.getValue() : fill.getValue();
    }

    private Color getBoxColor(PlayerEntity player) {
        return Alien.FRIEND.isFriend(player) ? boxFriend.getValue() : box.getValue();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        for (BreakManager.BreakData breakData : Alien.BREAK.breakMap.values()) {
            if (breakData == null || breakData.getEntity() == null) continue;
            PlayerEntity player = (PlayerEntity) breakData.getEntity();
            double size = 0.5 * (1 - breakData.fade.ease(ease.getValue()));
            Box cbox = new Box(breakData.pos).shrink(size, size, size).shrink(-size, -size, -size);
            if (fill.booleanValue) {
                Render3DUtil.drawFill(matrixStack, cbox, getFillColor(player));
            }
            if (box.booleanValue) {
                Render3DUtil.drawBox(matrixStack, cbox, getBoxColor(player));
            }
            Render3DUtil.drawText3D(player.getName().getString(), breakData.pos.toCenterPos().add(0, progress.getValue() ? 0.15 : 0, 0), -1);
            if (progress.getValue()) {
                Render3DUtil.drawText3D(Text.of(breakData.failed ? "§4Failed" : (breakData.complete ? "Broke" : df.format(Math.min(1, breakData.timer.getMs() / breakData.breakTime) * 100))), breakData.pos.toCenterPos().add(0, -0.15, 0), 0, 0, 1, breakData.complete ? (mc.world.isAir(breakData.pos) ? endColor : startColor) : ColorUtil.fadeColor(startColor, endColor, (double) breakData.timer.getMs() / breakData.breakTime));
            }
        }
        if (second.getValue())
            for (int i : Alien.BREAK.doubleMap.keySet()) {
                BreakManager.BreakData breakData = Alien.BREAK.doubleMap.get(i);
                if (breakData == null || breakData.getEntity() == null || mc.world.isAir(breakData.pos)) {
                    Alien.BREAK.doubleMap.remove(i);
                    continue;
                }
                BreakManager.BreakData singleBreakData = Alien.BREAK.breakMap.get(i);
                if (singleBreakData != null) {
                    if (singleBreakData.pos.equals(breakData.pos)) continue;
                }
                double size = 0.5 * (1 - breakData.fade.ease(ease.getValue()));
                Box cbox = new Box(breakData.pos).shrink(size, size, size).shrink(-size, -size, -size);
                if (secondFill.booleanValue) {
                    Render3DUtil.drawFill(matrixStack, cbox, secondFill.getValue());
                }
                if (secondBox.booleanValue) {
                    Render3DUtil.drawBox(matrixStack, cbox, secondBox.getValue());
                }
                Render3DUtil.drawText3D(breakData.getEntity().getName().getString(), breakData.pos.toCenterPos().add(0, 0.15, 0), -1);
                Render3DUtil.drawText3D("Double", breakData.pos.toCenterPos().add(0, -0.15, 0), doubleColor.getRGB());
            }
    }

    public static double getBreakTime(BlockPos pos, boolean extraBreak) {
        int slot = getTool(pos);
        if (slot == -1) {
            slot = mc.player.getInventory().selectedSlot;
        }
        return getBreakTime(pos, slot, extraBreak ? 1 : INSTANCE.damage.getValue());
    }

    static int getTool(BlockPos pos) {
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        float CurrentFastest = 1.0f;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            float destroySpeed;
            float digSpeed;
            if (entry.getValue().getItem() instanceof AirBlockItem || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)BreakESP.mc.world.getRegistryManager().getOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOrThrow(Enchantments.EFFICIENCY)), (ItemStack)entry.getValue())) + (destroySpeed = entry.getValue().getMiningSpeedMultiplier(BreakESP.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
            CurrentFastest = digSpeed + destroySpeed;
            slot.set(entry.getKey());
        }
        return slot.get();
    }

    static double getBreakTime(BlockPos pos, int slot, double damage) {
        return (1 / getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20 * 1000 * damage);
    }

    static float getBlockStrength(BlockPos position, ItemStack itemStack) {
        BlockState state = mc.world.getBlockState(position);
        float hardness = state.getHardness(mc.world, position);
        if (hardness < 0) {
            return 0;
        }
        float i = !state.isToolRequired() || itemStack.isSuitableFor(state) ? 30 : 100;
        return getDigSpeed(state, itemStack) / hardness / i;
    }

    static float getDigSpeed(BlockState state, ItemStack itemStack) {
        int efficiencyModifier;
        float digSpeed = BreakESP.getDestroySpeed(state, itemStack);
        if (digSpeed > 1.0f && (efficiencyModifier = EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)BreakESP.mc.world.getRegistryManager().getOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOrThrow(Enchantments.EFFICIENCY)), (ItemStack)itemStack)) > 0 && !itemStack.isEmpty()) {
            digSpeed += (float)(StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
        }
        return digSpeed < 0.0f ? 0.0f : digSpeed;
    }

    static float getDestroySpeed(BlockState state, ItemStack itemStack) {
        float destroySpeed = 1;
        if (itemStack != null && !itemStack.isEmpty()) {
            destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
        }
        return destroySpeed;
    }
}

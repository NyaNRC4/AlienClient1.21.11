package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

import java.awt.*;

public class AirPlace extends Module {
    public AirPlace() {
        super("AirPlace", Category.Player);
        setChinese("空气放置");
        INSTANCE = this;
    }

    public static AirPlace INSTANCE;
    public final BooleanSetting module = add(new BooleanSetting("Module", true));
    public final BooleanSetting grimBypass = add(new BooleanSetting("GrimBypass", false));
    public final BooleanSetting crossHair = add(new BooleanSetting("Crosshair", true).setParent());
    private final SliderSetting range = add(new SliderSetting("Range", 5f, 0f, 6f, crossHair::isOpen));
    private final ColorSetting fill = add(new ColorSetting("Fill", new Color(255, 0, 0, 50), crossHair::isOpen).injectBoolean(true));
    private final ColorSetting box = add(new ColorSetting("Box", new Color(255, 0, 0, 100), crossHair::isOpen).injectBoolean(true));

    private BlockHitResult hit;
    private int cooldown;

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (crossHair.getValue()) {
            if (cooldown > 0)
                cooldown--;

            HitResult hitResult = mc.getCameraEntity().raycast(range.getValue(), 0, false);

            if (hitResult instanceof BlockHitResult bhr) {
                hit = bhr;
            } else {
                hit = null;
            }
            if (hit == null || !mc.world.getBlockState(hit.getBlockPos()).getBlock().equals(Blocks.AIR) || (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)))
                return;

            boolean main = mc.player.getMainHandStack().getItem() instanceof BlockItem;
            if (mc.options.useKey.isPressed() && main && cooldown <= 0) {
                BlockUtil.airPlace(hit.getBlockPos(), false);
                cooldown = 2;
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (crossHair.getValue()) {
            if (hit == null || !mc.world.getBlockState(hit.getBlockPos()).getBlock().equals(Blocks.AIR) || (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)))
                return;
            Render3DUtil.draw3DBox(stack, new Box(hit.getBlockPos()), fill.getValue(), box.getValue(), box.booleanValue, fill.booleanValue);
        }
    }
}

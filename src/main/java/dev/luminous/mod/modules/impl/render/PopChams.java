package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.entity.CopyPlayerEntity;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PopChams extends Module {
    public static PopChams INSTANCE;
    public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut));
    private final ColorSetting fill = add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
    private final ColorSetting line = add(new ColorSetting("Line", new Color(255, 255, 255, 100)).injectBoolean(true));
    final CopyOnWriteArrayList<Person> popList = new CopyOnWriteArrayList<>();
    private final BooleanSetting alpha = add(new BooleanSetting("Alpha", true));
    private final BooleanSetting forceSneak = add(new BooleanSetting("ForceSneak", false));
    private final BooleanSetting noSelf = add(new BooleanSetting("NoSelf", true));
    private final BooleanSetting noLimb = add(new BooleanSetting("NoLimb", true));
    private final SliderSetting fadeTime = add(new SliderSetting("FadeTime", 300, 0, 1000));
    private final SliderSetting yOffset = add(new SliderSetting("YOffset", 0, -10, 10, .01));
    private final SliderSetting scale = add(new SliderSetting("Scale", 1, 0, 2, .01));
    private final SliderSetting yaw = add(new SliderSetting("Yaw", 0, 0, 720, .01));

    public PopChams() {
        super("PopChams", Category.Render);
        setChinese("爆图腾上色");
        INSTANCE = this;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        //RenderSystem.depthMask(false);

        popList.removeIf(person -> person.render(matrixStack));

        //RenderSystem.depthMask(true);
    }

    @EventListener
    private void onTotemPop(TotemEvent event) {
        if (noSelf.getValue() && event.getPlayer().equals(mc.player)) return;

        popList.add(new Person(new CopyPlayerEntity(event.getPlayer())));
    }

    private class Person {
        public final ModelPlayer modelPlayer;
        final Animation animation;

        public Person(PlayerEntity player) {
            modelPlayer = new ModelPlayer(player);
            animation = new Animation();
        }

        public boolean render(MatrixStack matrixStack) {
            double animation = this.animation.get(1, fadeTime.getValueInt(), ease.getValue());
            if (animation >= 1) return true;
            modelPlayer.render(matrixStack, fill, line, alpha.getValue() ? (1 - animation) : 1, yOffset.getValue() * animation, 1 + (scale.getValue() - 1) * animation, yaw.getValue() * animation, noLimb.getValue(), forceSneak.getValue());
            return false;
        }
    }
}

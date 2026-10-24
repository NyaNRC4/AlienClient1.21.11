package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

public class Chams extends Module {
    public static Chams INSTANCE;
    public final BooleanSetting crystal = add(new BooleanSetting("Crystal", true).setParent());
    public final BooleanSetting custom = add(new BooleanSetting("Custom", false, crystal::isOpen).setParent());
    public final BooleanSetting depth = add(new BooleanSetting("Depth", false, () -> crystal.isOpen() && custom.isOpen()));
    public final BooleanSetting chamsTexture = add(new BooleanSetting("ChamsTexture", true, () -> crystal.isOpen() && custom.isOpen()));
    public final ColorSetting fill = add(new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> crystal.isOpen() && custom.isOpen()).injectBoolean(false));
    public final ColorSetting line = add(new ColorSetting("Line", new Color(255, 255, 255, 100), () -> crystal.isOpen() && custom.isOpen()).injectBoolean(false));
    public final ColorSetting core =
            add(new ColorSetting("Core", new Color(255, 255, 255, 255), crystal::isOpen).injectBoolean(true));
    public final ColorSetting outerFrame =
            add(new ColorSetting("OuterFrame", new Color(255, 255, 255, 255), crystal::isOpen).injectBoolean(true));
    public final ColorSetting innerFrame =
            add(new ColorSetting("InnerFrame", new Color(255, 255, 255, 255), crystal::isOpen).injectBoolean(true));
    public final BooleanSetting glint = add(new BooleanSetting("Glint", true, crystal::isOpen));
    public final BooleanSetting texture = add(new BooleanSetting("Texture", true, crystal::isOpen));
    public final BooleanSetting spinSync = add(new BooleanSetting("SpinSync", false, crystal::isOpen));
    public final SliderSetting scale = add(new SliderSetting("Scale", 1, 0, 3f, 0.01, crystal::isOpen));
    public final SliderSetting spinValue = add(new SliderSetting("SpinSpeed", 1f, 0, 3f, 0.01, crystal::isOpen));
    public final SliderSetting bounceHeight = add(new SliderSetting("BounceHeight", 1, 0, 3f, 0.01, crystal::isOpen));
    public final SliderSetting floatValue = add(new SliderSetting("BounceSpeed", 1f, 0, 3f, 0.01, crystal::isOpen));
    public final SliderSetting floatOffset = add(new SliderSetting("YOffset", 0f, -1, 1f, 0.01, crystal::isOpen));

    public final BooleanSetting throughWall = add(new BooleanSetting("ThroughWall", false).setParent());
    private final BooleanSetting Crystals = add(new BooleanSetting("Crystals", true, throughWall::isOpen));
    private final BooleanSetting Players = add(new BooleanSetting("Players", true, throughWall::isOpen));
    private final BooleanSetting Mobs = add(new BooleanSetting("Mobs", true, throughWall::isOpen));
    private final BooleanSetting Animals = add(new BooleanSetting("Animals", true, throughWall::isOpen));
    private final BooleanSetting Villagers = add(new BooleanSetting("Villagers", true, throughWall::isOpen));
    private final BooleanSetting Slimes = add(new BooleanSetting("Slimes", true, throughWall::isOpen));

    public final ColorSetting hand = add(new ColorSetting("Hand", -1).injectBoolean(true));

    public Chams() {
        super("Chams", Category.Render);
        setChinese("模型上色");
        INSTANCE = this;
    }

    public boolean customCrystal() {
        return isOn() && crystal.getValue();
    }

    public int age;

    @EventListener
    public void onUpdate(UpdateEvent event) {
        age++;
    }

    @Override
    public boolean onRender3DQueue(MatrixStack matrixStack) {
        if (!isOn() || !throughWall.getValue() || mc.world == null || mc.player == null) {
            return false;
        }

        OrderedRenderCommandQueue queue = mc.gameRenderer.getEntityRenderCommandQueue();
        CameraRenderState cameraRenderState = mc.gameRenderer.getEntityRenderStates().cameraRenderState;
        var tickManager = mc.world.getTickManager();

        boolean submitted = false;
        for (Entity entity : mc.world.getEntities()) {
            if (entity != mc.player && chams(entity)) {
                float delta = mc.getRenderTickCounter().getTickProgress(!tickManager.shouldSkipTick(entity));
                ModelPlayer.renderTexture(matrixStack, queue, cameraRenderState, entity, delta);
                submitted = true;
            }
        }
        return submitted;
    }

    public boolean chams(Entity entity) {
        if (entity instanceof EndCrystalEntity) return Crystals.getValue();
        if (entity instanceof SlimeEntity) return Slimes.getValue();
        if (entity instanceof PlayerEntity) return Players.getValue();
        if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) return Villagers.getValue();
        if (entity instanceof AnimalEntity) return Animals.getValue();
        if (entity instanceof MobEntity) return Mobs.getValue();
        return false;
    }

    public int getHandArgb() {
        return hand.getValue().getRGB();
    }
}

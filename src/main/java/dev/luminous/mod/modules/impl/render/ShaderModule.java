package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.asm.accessors.IGameRenderer;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class ShaderModule
        extends Module {
    public static ShaderModule INSTANCE;
    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.Shader));
    public final EnumSetting<ShaderManager.Shader> mode = add(new EnumSetting<>("Mode", ShaderManager.Shader.Solid, () -> page.getValue() == Page.Shader));
    public final SliderSetting speed = add(new SliderSetting("Speed", 4, 0, 20, 0.1, () -> page.getValue() == Page.Shader));
    public final ColorSetting fill = add(new ColorSetting("Color", () -> page.getValue() == Page.Shader));
    public final SliderSetting maxSample =
            add(new SliderSetting("MaxSample", 10.0f, 0f, 20.0f, () -> page.getValue() == Page.Shader));
    public final SliderSetting divider =
            add(new SliderSetting("Divider", 150.0f, 0f, 300.0f, () -> page.getValue() == Page.Shader));
    public final SliderSetting radius =
            add(new SliderSetting("Radius", 2.0f, 0f, 6.0f, () -> page.getValue() == Page.Shader));
    public final SliderSetting smoothness =
            add(new SliderSetting("Smoothness", 1.0f, 0f, 1.0f, 0.01, () -> page.getValue() == Page.Shader));
    public final SliderSetting alpha =
            add(new SliderSetting("GlowAlpha", 255, 0, 255, () -> page.getValue() == Page.Shader));
    public final SliderSetting maxRange = add(new SliderSetting("MaxRange", 64, 16, 512, () -> page.getValue() == Page.Target));

    public final SliderSetting pulseSpeed = add(new SliderSetting("PulseSize", 10, 0, 200, 0.1, () -> page.getValue() == Page.Color));
    public final ColorSetting pulse = add(new ColorSetting("Pulse", new Color(255, 255, 255), () -> page.getValue() == Page.Color));
    public final SliderSetting step =
            add(new SliderSetting("Step", 0.5f, 0f, 4.0f, 0.01f, () -> page.getValue() == Page.Color));
    public final SliderSetting octaves = add(new SliderSetting("Octaves", 10, 5, 30, () -> page.getValue() == Page.Color));
    public final ColorSetting smoke1 = add(new ColorSetting("Color1", new Color(255, 255, 255), () -> page.getValue() == Page.Color));
    public final ColorSetting smoke2 = add(new ColorSetting("Color2", new Color(255, 255, 255), () -> page.getValue() == Page.Color));
    public final ColorSetting smoke3 = add(new ColorSetting("Color3", new Color(255, 255, 255), () -> page.getValue() == Page.Color));
    public final ColorSetting smoke4 = add(new ColorSetting("Color4", new Color(255, 255, 255), () -> page.getValue() == Page.Color));

    private final BooleanSetting hands = add(new BooleanSetting("Hands", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting self = add(new BooleanSetting("Self", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting players = add(new BooleanSetting("Players", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting friends = add(new BooleanSetting("Friends", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting crystals = add(new BooleanSetting("Crystals", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting creatures = add(new BooleanSetting("Creatures", false, () -> page.getValue() == Page.Target));
    private final BooleanSetting monsters = add(new BooleanSetting("Monsters", false, () -> page.getValue() == Page.Target));
    private final BooleanSetting ambients = add(new BooleanSetting("Ambients", false, () -> page.getValue() == Page.Target));
    private final BooleanSetting items = add(new BooleanSetting("Items", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting others = add(new BooleanSetting("Others", false, () -> page.getValue() == Page.Target));

    public ShaderModule() {
        super("Shader", Category.Render);
        setChinese("着色器");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (ShaderModule.mc.player == null) {
            return false;
        }
        if ((double)MathHelper.sqrt((float)ShaderModule.mc.player.squaredDistanceTo(entity.getEntityPos())) > this.maxRange.getValue()) {
            return false;
        }
        Entity entity2 = entity;
        Objects.requireNonNull(entity2);
        Entity entity3 = entity2;
        switch (entity) {
            case PlayerEntity playerEntity -> {
                if (entity == ShaderModule.mc.player) {
                    return this.self.getValue();
                }
                if (Alien.FRIEND.isFriend(playerEntity)) {
                    return this.friends.getValue();
                }
                return this.players.getValue();
            }
            case EndCrystalEntity endCrystalEntity -> {
                return this.crystals.getValue();
            }
            case ItemEntity itemEntity -> {
                return this.items.getValue();
            }
            default -> {
            }
        }
        return switch (entity.getType().getSpawnGroup()) {
            case SpawnGroup.CREATURE, SpawnGroup.WATER_CREATURE -> this.creatures.getValue();
            case SpawnGroup.MONSTER -> this.monsters.getValue();
            case SpawnGroup.AMBIENT, SpawnGroup.WATER_AMBIENT -> this.ambients.getValue();
            default -> this.others.getValue();
        };
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.hands.getValue()) {
            Alien.SHADER.renderShader(() -> ((IGameRenderer)ShaderModule.mc.gameRenderer).IRenderHand(mc.getRenderTickCounter().getTickProgress(true), false, matrixStack.peek().getPositionMatrix()), this.mode.getValue());
        }
    }

    @Override
    public void onToggle() {
        Alien.SHADER.reloadShaders();
    }

    @Override
    public void onLogin() {
        Alien.SHADER.reloadShaders();
    }

    private static enum Page {
        Shader,
        Target,
        Color;

    }
}


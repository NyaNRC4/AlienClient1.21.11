package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.MovementSync;
import dev.luminous.mod.modules.settings.enums.Placement;
import dev.luminous.mod.modules.settings.enums.SnapBack;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class AntiCheat extends Module {
    public static AntiCheat INSTANCE;
    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    public final BooleanSetting attackCDFix = add(new BooleanSetting("TrueAttackCD", false, () -> page.is(Page.General)));
    public final BooleanSetting multiPlace = add(new BooleanSetting("MultiPlace", true, () -> page.is(Page.General)));
    public final BooleanSetting packetPlace = add(new BooleanSetting("PacketPlace", true, () -> page.is(Page.General)));
    public final BooleanSetting attackRotate = add(new BooleanSetting("AttackRotation", false, () -> page.is(Page.General)));
    public final BooleanSetting invSwapBypass = add(new BooleanSetting("PickSwap", false, () -> page.is(Page.General)));
    public final BooleanSetting priorHotbar = add(new BooleanSetting("PriorHotbar", false, () -> page.is(Page.General)));
    public final SliderSetting ieRange = add(new SliderSetting("InteractEntityRange", 3, 0, 8, .1, () -> page.is(Page.General)));
    public final SliderSetting boxSize = add(new SliderSetting("HitBoxSize", 0.6, 0, 1, 0.01, () -> page.is(Page.General)));
    public final SliderSetting attackDelay = add(new SliderSetting("BreakDelay", 0.2, 0, 1, 0.01, () -> page.is(Page.General)).setSuffix("s"));
    public final BooleanSetting noBadSlot = add(new BooleanSetting("NoBadSlot", false, () -> page.is(Page.General)));
    public final EnumSetting<Placement> placement = add(new EnumSetting<>("Placement", Placement.Vanilla, () -> page.is(Page.General)));
    public final BooleanSetting upDirectionLimit = add(new BooleanSetting("UPDirectionLimit", true, () -> page.is(Page.General) && placement.is(Placement.NCP)));
    public final EnumSetting<SwingSide> interactSwing = add(new EnumSetting<>("InteractSwing", SwingSide.All, () -> page.is(Page.General)));
    public final EnumSetting<SwingSide> attackSwing = add(new EnumSetting<>("AttackSwing", SwingSide.All, () -> page.is(Page.General)));

    public final BooleanSetting grimRotation = add(new BooleanSetting("GrimRotation", false, () -> page.is(Page.Rotation)));
    public final EnumSetting<SnapBack> snapBackEnum = add(new EnumSetting<>("SnapBack", SnapBack.None, () -> page.is(Page.Rotation)));
    public final BooleanSetting look = add(new BooleanSetting("Look", true, () -> page.is(Page.Rotation)));
    public final SliderSetting rotateTime = add(new SliderSetting("LookTime", 0.5, 0, 1, 0.01, () -> page.is(Page.Rotation)));
    public final BooleanSetting random = add(new BooleanSetting("Random", true, () -> page.is(Page.Rotation)));
    public final SliderSetting steps = add(new SliderSetting("Steps", 0.6, 0, 1, 0.01, () -> page.is(Page.Rotation)));
    public final BooleanSetting serverSide = add(new BooleanSetting("ServerSide", false, () -> page.is(Page.Rotation)));
    public final BooleanSetting fullPackets = add(new BooleanSetting("FullPackets", false, () -> page.is(Page.Rotation)).setParent());
    public final BooleanSetting force = add(new BooleanSetting("AlwaysSend", false, () -> page.is(Page.Rotation) && fullPackets.isOpen()));
    public final BooleanSetting forceSync = add(new BooleanSetting("ForceSync", true, () -> page.is(Page.Rotation)));
    public final BooleanSetting interactRotation = add(new BooleanSetting("InteractRotation", false, () -> page.is(Page.Rotation)));

    public final BooleanSetting detectDouble = add(new BooleanSetting("DetectDouble", true, () -> page.is(Page.Misc)));
    public final SliderSetting doubleMineTimeout = add(new SliderSetting("DoubleTimeout", 2, 0, 3, 0.1, () -> page.is(Page.Misc)).setSuffix("*"));
    public final SliderSetting minTimeout = add(new SliderSetting("MinTimeout", 2, 0, 10, 0.1, () -> page.is(Page.Misc)).setSuffix("s"));
    public final SliderSetting breakTimeout = add(new SliderSetting("BreakFailed", 1.5, 0, 3, 0.1, () -> page.is(Page.Misc)).setSuffix("*"));
    public final BooleanSetting ignoreArmorStand = add(new BooleanSetting("IgnoreArmorStand", false, () -> page.is(Page.Misc)));
    public final BooleanSetting closeScreen = add(new BooleanSetting("CloseScreen", false, () -> page.is(Page.Misc)));

    public final EnumSetting<Motion> motion = add(new EnumSetting<>("Motion", Motion.Position, () -> page.getValue() == Page.Predict));
    public final SliderSetting predictTicks = add(new SliderSetting("Predict", 4, 0, 10, () -> page.getValue() == Page.Predict).setSuffix("ticks"));
    public final SliderSetting simulation = add(new SliderSetting("Simulation", 5, 0, 20, 1, () -> page.getValue() == Page.Predict));
    public final SliderSetting maxMotionY = add(new SliderSetting("MaxMotionY", 0.34, 0, 2, 0.01, () -> page.getValue() == Page.Predict));
    public final BooleanSetting step = add(new BooleanSetting("Step", false, () -> page.getValue() == Page.Predict));
    public final BooleanSetting doubleStep = add(new BooleanSetting("DoubleStep", false, () -> page.getValue() == Page.Predict));
    public final BooleanSetting jump = add(new BooleanSetting("Jump", false, () -> page.getValue() == Page.Predict));
    public final BooleanSetting inBlockPause = add(new BooleanSetting("InBlockPause", true, () -> page.getValue() == Page.Predict));

    public AntiCheat() {
        super("AntiCheat", Category.Client);
        setChinese("反作弊选项");
        INSTANCE = this;
    }

    public boolean movementSync() {
        return MovementSync.INSTANCE.isOn();
    }

    public static double getOffset() {
        if (INSTANCE != null) return INSTANCE.boxSize.getValue() / 2;
        return 0.3;
    }

    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    public enum Page {
        General,
        Rotation,
        Misc,
        Predict
    }

    public enum Motion {
        Velocity,
        Position
    }
}

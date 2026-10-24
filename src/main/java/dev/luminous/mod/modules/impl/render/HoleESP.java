package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HoleESP extends Module {

    public static HoleESP INSTANCE;
    public final SliderSetting startFade = add(new SliderSetting("StartFade", 5f, 1f, 20f));
    public final SliderSetting distance = add(new SliderSetting("Distance", 6f, 1f, 20f));
    public final SliderSetting airHeight = add(new SliderSetting("AirHeight", 1, -3, 3, .01));
    public final BooleanSetting airYCheck = add(new BooleanSetting("AirYCheck", true));
    public final SliderSetting height = add(new SliderSetting("Height", 1, -3, 3, .1));
    public final SliderSetting wallHeight = add(new SliderSetting("WallHeight", 3, -3, 3, .1));
    public final BooleanSetting sideCheck = add(new BooleanSetting("SideCheck", true));
    private final ColorSetting airFill = add(new ColorSetting("AirFill", new Color(148, 0, 0, 100)).injectBoolean(true));
    private final ColorSetting airBox = add(new ColorSetting("AirBox", new Color(148, 0, 0, 100)).injectBoolean(true));
    private final ColorSetting airFade = add(new ColorSetting("AirFade", new Color(148, 0, 0, 0)).injectBoolean(true));
    private final ColorSetting normalFill = add(new ColorSetting("UnsafeFill", new Color(255, 0, 0, 50)).injectBoolean(true));
    private final ColorSetting normalBox = add(new ColorSetting("UnsafeBox", new Color(255, 0, 0, 100)).injectBoolean(true));
    private final ColorSetting normalFade = add(new ColorSetting("UnsafeFade", new Color(255, 0, 0, 0)).injectBoolean(true));
    private final ColorSetting bedrockFill = add(new ColorSetting("SafeFill", new Color(8, 255, 79, 50)).injectBoolean(true));
    private final ColorSetting bedrockBox = add(new ColorSetting("SafeBox", new Color(8, 255, 79, 100)).injectBoolean(true));
    private final ColorSetting bedrockFade = add(new ColorSetting("SafeFade", new Color(8, 255, 79, 100)).injectBoolean(true));
    private final ColorSetting wallFill = add(new ColorSetting("WallFill", new Color(0, 255, 255, 128)).injectBoolean(true));
    private final ColorSetting wallBox = add(new ColorSetting("WallBox", new Color(0, 225, 255, 255)).injectBoolean(true));
    private final ColorSetting wallFade = add(new ColorSetting("WallFade", new Color(0, 255, 255, 64)).injectBoolean(true));
    private final ColorSetting wallSideFill = add(new ColorSetting("WallSideFill", new Color(0, 255, 255, 128)).injectBoolean(true));
    private final ColorSetting wallSideBox = add(new ColorSetting("WallSideBox", new Color(0, 225, 255, 255)).injectBoolean(true));
    private final ColorSetting wallSideFade = add(new ColorSetting("WallSideFade", new Color(0, 255, 255, 64)).injectBoolean(true));
    private final SliderSetting updateDelay =
            add(new SliderSetting("UpdateDelay", 50, 0, 1000));
    private final List<BlockPos> tempNormalList = new ArrayList<>();
    private final List<BlockPos> tempBedrockList = new ArrayList<>();
    private final List<BlockPos> tempAirList = new ArrayList<>();
    private final List<BlockPos> tempWallList = new ArrayList<>();
    private final List<BlockPos> tempWallSideList = new ArrayList<>();
    private final Timer timer = new Timer();
    boolean drawing = false;
    private List<BlockPos> normalList = new ArrayList<>();
    private List<BlockPos> bedrockList = new ArrayList<>();
    private List<BlockPos> airList = new ArrayList<>();
    private List<BlockPos> wallList = new ArrayList<>();
    private List<BlockPos> wallSideList = new ArrayList<>();

    public HoleESP() {
        super("HoleESP", Category.Render);
        setChinese("坑透视");
        INSTANCE = this;
    }

    public void onThread() {
        if (HoleESP.nullCheck() || this.isOff()) {
            return;
        }
        if (!this.drawing && this.timer.passedMs(this.updateDelay.getValue())) {
            this.normalList = new ArrayList<BlockPos>(this.tempNormalList);
            this.bedrockList = new ArrayList<BlockPos>(this.tempBedrockList);
            this.airList = new ArrayList<BlockPos>(this.tempAirList);
            this.wallList = new ArrayList<BlockPos>(this.tempWallList);
            this.wallSideList = new ArrayList<BlockPos>(this.tempWallSideList);
            this.timer.reset();
            this.tempBedrockList.clear();
            this.tempNormalList.clear();
            this.tempAirList.clear();
            this.tempWallList.clear();
            this.tempWallSideList.clear();
            for (BlockPos pos : BlockUtil.getSphere(this.distance.getValueFloat(), HoleESP.mc.player.getEntityPos())) {
                Type type;
                if (this.isBedrock(pos) && this.isBedrock(pos.up(2)) && this.isBedrock(pos.down())) {
                    Direction side = this.getWallSide(pos);
                    if (side != null || !this.sideCheck.getValue()) {
                        this.tempWallList.add(pos);
                    }
                    if (side != null) {
                        this.tempWallSideList.add(pos.offset(side));
                    }
                }
                if ((type = this.isHole(pos)) == Type.Bedrock) {
                    this.tempBedrockList.add(pos);
                    continue;
                }
                if (type == Type.Normal) {
                    this.tempNormalList.add(pos);
                    continue;
                }
                if (type != Type.Air) continue;
                this.tempAirList.add(pos);
            }
        }
    }

    private Direction getWallSide(BlockPos pos) {
        double getDistance = Double.MAX_VALUE;
        Direction side = null;
        for (Direction direction : Direction.values()) {
            BlockPos offsetPos;
            if (direction == Direction.UP || direction == Direction.DOWN || !BlockUtil.canCollide(new Box((offsetPos = pos.offset(direction)).down())) || BlockUtil.canCollide(new Box(offsetPos)) || BlockUtil.canCollide(new Box(offsetPos.up()))) continue;
            if (side == null) {
                side = direction;
                getDistance = HoleESP.mc.player.getEyePos().distanceTo(offsetPos.toCenterPos());
                continue;
            }
            if (!(HoleESP.mc.player.getEyePos().distanceTo(offsetPos.toCenterPos()) < getDistance)) continue;
            side = direction;
            getDistance = HoleESP.mc.player.getEyePos().distanceTo(offsetPos.toCenterPos());
        }
        return side;
    }

    private boolean isBedrock(BlockPos pos) {
        return HoleESP.mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    Type isHole(BlockPos pos) {
        if (HoleESP.mc.world.isAir(pos) && (!this.airYCheck.getValue() || pos.getY() == HoleESP.mc.player.getBlockY() - 1 || pos.getY() == HoleESP.mc.player.getBlockY()) && Alien.HOLE.isHard(pos.up())) {
            return Type.Air;
        }
        int blockProgress = 0;
        boolean bedRock = true;
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN || !Alien.HOLE.isHard(pos.offset(i))) continue;
            if (HoleESP.mc.world.getBlockState(pos.offset(i)).getBlock() != Blocks.BEDROCK) {
                bedRock = false;
            }
            ++blockProgress;
        }
        if (HoleESP.mc.world.isAir(pos) && HoleESP.mc.world.isAir(pos.up()) && HoleESP.mc.world.isAir(pos.up(2)) && blockProgress > 3 && BlockUtil.canCollide((Entity)HoleESP.mc.player, new Box(pos.down()))) {
            if (bedRock) {
                return Type.Bedrock;
            }
            return Type.Normal;
        }
        if (Alien.HOLE.isDoubleHole(pos)) {
            return Type.Normal;
        }
        return Type.None;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        this.drawing = true;
        this.draw(matrixStack, this.bedrockList, this.bedrockFill, this.bedrockFade, this.bedrockBox, this.height.getValue());
        this.draw(matrixStack, this.airList, this.airFill, this.airFade, this.airBox, this.airHeight.getValue());
        this.draw(matrixStack, this.normalList, this.normalFill, this.normalFade, this.normalBox, this.height.getValue());
        this.draw(matrixStack, this.wallList, this.wallFill, this.wallFade, this.wallBox, this.wallHeight.getValue());
        this.draw(matrixStack, this.wallSideList, this.wallSideFill, this.wallSideFade, this.wallSideBox, this.height.getValue());
        this.drawing = false;
    }

    private void draw(MatrixStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting fade, ColorSetting box, double height) {
        for (BlockPos pos : list) {
            double getDistance = HoleESP.mc.player.getEntityPos().distanceTo(pos.toCenterPos());
            double fadeRange = Math.max(1.0E-4, this.distance.getValue() - this.startFade.getValue());
            double alpha = getDistance > this.startFade.getValue() ? Math.max(Math.min(1.0, 1.0 - (getDistance - this.startFade.getValue()) / fadeRange), 0.0) : 1.0;
            Box espBox = new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)pos.getY() + height, (double)(pos.getZ() + 1));
            if (fill.booleanValue) {
                if (fade.booleanValue) {
                    Render3DScheduler.INSTANCE.addFade(espBox, ColorUtil.injectAlpha(fill.getValue(), (int)((double)fill.getValue().getAlpha() * alpha)), ColorUtil.injectAlpha(fade.getValue(), (int)((double)fade.getValue().getAlpha() * alpha)));
                } else {
                    Render3DScheduler.INSTANCE.addBox(espBox, ColorUtil.injectAlpha(fill.getValue(), (int)((double)fill.getValue().getAlpha() * alpha)), new Color(0, 0, 0, 0), false, true);
                }
            }
            if (!box.booleanValue) continue;
            Render3DScheduler.INSTANCE.addBox(espBox, new Color(0, 0, 0, 0), ColorUtil.injectAlpha(box.getValue(), (int)((double)box.getValue().getAlpha() * alpha)), true, false);
        }
    }

    public static enum Type {
        None,
        Air,
        Normal,
        Bedrock;

    }
}

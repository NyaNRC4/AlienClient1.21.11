package dev.luminous.mod.modules.impl.client;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.ICustomGoalProcess;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BaritoneModule extends Module {
    public static BaritoneModule INSTANCE;
    private final SliderSetting rangeConfig = add(new SliderSetting("Range", 4.0f, 1.0f, 5.0f));
    private final BooleanSetting placeConfig = add(new BooleanSetting("Place", true));
    private final BooleanSetting breakConfig = add(new BooleanSetting("Break", true));
    private final BooleanSetting sprintConfig = add(new BooleanSetting("Sprint", true));
    private final BooleanSetting inventoryConfig = add(new BooleanSetting("UseInventory", false));
    private final BooleanSetting vinesConfig = add(new BooleanSetting("Vines", true));
    private final BooleanSetting jump256Config = add(new BooleanSetting("JumpAt256", false));
    private final BooleanSetting waterBucketFallConfig = add(new BooleanSetting("WaterBucketFall", false));
    private final BooleanSetting parkourConfig = add(new BooleanSetting("Parkour", true));
    private final BooleanSetting parkourPlaceConfig = add(new BooleanSetting("ParkourPlace", false));
    private final BooleanSetting parkourAscendConfig = add(new BooleanSetting("ParkourAscend", true));
    private final BooleanSetting diagonalAscendConfig = add(new BooleanSetting("DiagonalAscend", false));
    private final BooleanSetting diagonalDescendConfig = add(new BooleanSetting("DiagonalDescend", false));
    private final BooleanSetting mineDownConfig = add(new BooleanSetting("MineDownward", true));
    private final BooleanSetting legitMineConfig = add(new BooleanSetting("LegitMine", false));
    private final BooleanSetting logOnArrivalConfig = add(new BooleanSetting("LogOnArrival", false));
    private final BooleanSetting freeLookConfig = add(new BooleanSetting("FreeLook", true));
    private final BooleanSetting antiCheatConfig = add(new BooleanSetting("AntiCheat", true));
    private final BooleanSetting strictLiquidConfig = add(new BooleanSetting("Strict-Liquid", false));
    private final BooleanSetting censorCoordsConfig = add(new BooleanSetting("CensorCoords", false));
    private final BooleanSetting censorCommandsConfig = add(new BooleanSetting("CensorCommands", false));
    private final BooleanSetting chatControl = add(new BooleanSetting("ChatControl", false));
    private final BooleanSetting debugConfig = add(new BooleanSetting("Debug", false));

    public BaritoneModule() {
        super("Baritone", Category.Client);
        Alien.EVENT_BUS.subscribe(this);
        INSTANCE = this;
        setChinese("寻路设置");
    }

    public static void forward() {
        Direction direction = mc.player.getHorizontalFacing();
        var x = mc.player.getBlockX() + direction.getVector().getX() * 30000000;
        var z = mc.player.getBlockZ() + direction.getVector().getZ() * 30000000;

        cancelEverything();
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            if (customGoalProcess != null) {
                customGoalProcess.setGoalAndPath(new GoalXZ(x, z));
            }
        }
    }

    public static boolean isPathing() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        return baritone != null && baritone.getPathingBehavior() != null && baritone.getPathingBehavior().isPathing();
    }

    public static void gotoPos(BlockPos pos) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            if (customGoalProcess == null) return;
            customGoalProcess.setGoalAndPath(new GoalBlock(pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    public static void mine(Block block) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            baritone.getMineProcess().mine(block);
        }
    }

    public static void cancelEverything() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null && baritone.getPathingBehavior() != null) {
            baritone.getPathingBehavior().cancelEverything();
        }
    }

    public static boolean isActive() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            if (customGoalProcess != null && customGoalProcess.isActive()) {
                return true;
            }

            IPathingControlManager controlManager = baritone.getPathingControlManager();
            if (controlManager != null && controlManager.mostRecentInControl().isPresent()) {
                return controlManager.mostRecentInControl().get().isActive();
            }
        }
        return false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        BaritoneAPI.getSettings().blockReachDistance.value = rangeConfig.getValueFloat();
        BaritoneAPI.getSettings().allowPlace.value = placeConfig.getValue();
        BaritoneAPI.getSettings().allowBreak.value = breakConfig.getValue();
        BaritoneAPI.getSettings().allowSprint.value = sprintConfig.getValue();
        BaritoneAPI.getSettings().allowInventory.value = inventoryConfig.getValue();
        BaritoneAPI.getSettings().allowVines.value = vinesConfig.getValue();
        BaritoneAPI.getSettings().allowJumpAt256.value = jump256Config.getValue();
        BaritoneAPI.getSettings().allowWaterBucketFall.value = waterBucketFallConfig.getValue();
        BaritoneAPI.getSettings().allowParkour.value = parkourConfig.getValue();
        BaritoneAPI.getSettings().allowParkourAscend.value = parkourAscendConfig.getValue();
        BaritoneAPI.getSettings().allowParkourPlace.value = parkourPlaceConfig.getValue();
        BaritoneAPI.getSettings().allowDiagonalAscend.value = diagonalAscendConfig.getValue();
        BaritoneAPI.getSettings().allowDiagonalDescend.value = diagonalDescendConfig.getValue();
        BaritoneAPI.getSettings().allowDownward.value = mineDownConfig.getValue();
        BaritoneAPI.getSettings().legitMine.value = legitMineConfig.getValue();
        BaritoneAPI.getSettings().disconnectOnArrival.value = logOnArrivalConfig.getValue();
        BaritoneAPI.getSettings().freeLook.value = freeLookConfig.getValue();
        BaritoneAPI.getSettings().antiCheatCompatibility.value = antiCheatConfig.getValue();
        BaritoneAPI.getSettings().strictLiquidCheck.value = strictLiquidConfig.getValue();
        BaritoneAPI.getSettings().censorCoordinates.value = censorCoordsConfig.getValue();
        BaritoneAPI.getSettings().censorRanCommands.value = censorCommandsConfig.getValue();
        BaritoneAPI.getSettings().chatControl.value = chatControl.getValue();
        BaritoneAPI.getSettings().chatDebug.value = debugConfig.getValue();
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
}